/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.elasticjob.infra.spi;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.elasticjob.infra.exception.JobConfigurationException;
import org.apache.shardingsphere.elasticjob.infra.spi.annotation.ExtensionServiceProvider;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceConfigurationError;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Extension service.
 *
 * @param <T> extension service interface type
 */
@Slf4j
public final class ExtensionService<T> {

    private static final String PREFIX = "META-INF/services/";

    private final Map<Class<?>, String> cachedKeyClasses = new HashMap<>(8);

    private final ConcurrentMap<String, Holder<T>> cachedExtensionServiceProviders = new ConcurrentHashMap<>(8);

    private final Class<T> extensionServiceInterface;

    private final String defaultProviderKey;

    private Map<String, Class<T>> cachedExtensionServiceProviderClasses;

    ExtensionService(final Class<T> extensionServiceInterface) {
        this.extensionServiceInterface = extensionServiceInterface;
        this.defaultProviderKey = extensionServiceInterface.getAnnotation(org.apache.shardingsphere.elasticjob.infra.spi.annotation.ExtensionService.class).value();
        this.cachedExtensionServiceProviderClasses = loadExtensionServiceProviderClasses();
    }

    /**
     * Get extension service provider by associated class.
     *
     * @param keyClass class
     * @return extension service provider
     */
    public T getProvider(final Class<?> keyClass) {
        return getProvider(getProviderKey(keyClass));
    }

    /**
     * Get extension service provider by associated key.
     *
     * @param key key
     * @return extension service provider
     */
    public T getProvider(final String key) {
        if (Strings.isNullOrEmpty(key) && Strings.isNullOrEmpty(defaultProviderKey)) {
            throw new IllegalArgumentException("Extension service provider key and defaultProviderKey are both null(or empty)");
        }
        String providerKey = key;
        if (Strings.isNullOrEmpty(key)) {
            providerKey = defaultProviderKey;
        }

        Holder<T> holder = cachedExtensionServiceProviders.get(providerKey);
        if (holder == null) {
            cachedExtensionServiceProviders.putIfAbsent(providerKey, new Holder<>());
            holder = cachedExtensionServiceProviders.get(providerKey);
        }

        T provider = holder.get();
        if (provider == null) {
            synchronized (holder) {
                provider = holder.get();
                if (provider == null) {
                    provider = createExtensionServiceProvider(providerKey);
                    holder.set(provider);
                }
            }
        }
        return provider;
    }

    /**
     * Get optional extension service provider by key.
     *
     * @param key service provider key
     * @return optional Optional wrapped extension service provider
     */
    public Optional<T> getOptionalProvider(final String key) {
        T provider = null;
        try {
            provider = getProvider(key);
        } catch (IllegalArgumentException | JobConfigurationException | IllegalStateException ex) {
            log.warn("Error happened while getting extension service provider, the Optional instance returned will be empty", ex);
        }
        return Optional.ofNullable(provider);
    }

    /**
     * Get optional extension service provider by keyClass.
     *
     * @param keyClass service provider keyClass
     * @return optional  Optional wrapped extension service provider
     */
    public Optional<T> getOptionalProvider(final Class keyClass) {
        return getOptionalProvider(getProviderKey(keyClass));
    }

    private String getProviderKey(final Class<?> keyClass) {
        String providerKey = null;
        for (Map.Entry<Class<?>, String> entry : cachedKeyClasses.entrySet()) {
            if (entry.getKey().isAssignableFrom(keyClass)) {
                providerKey = entry.getValue();
                break;
            }
        }
        return providerKey;
    }

    private T createExtensionServiceProvider(final String key) {
        Class<T> clazz = cachedExtensionServiceProviderClasses.get(key);
        if (clazz == null) {
            throw new JobConfigurationException("Can not find class keyed by " + key);
        } else {
            try {
                return clazz.newInstance();
            } catch (InstantiationException ex) {
                throw new IllegalStateException("ExtensionServiceProvider instance (key: " + key + ", class: " + extensionServiceInterface + ") couldn't be instantiated: " + ex.getMessage(), ex);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("ExtensionServiceProvider instance (key: " + key + ", class: " + extensionServiceInterface + ") couldn't be accessed: " + ex.getMessage(), ex);
            }
        }
    }

    private Map<String, Class<T>> loadExtensionServiceProviderClasses() {
        String fileName = PREFIX + extensionServiceInterface.getName();
        Map<String, Class<T>> extensionServiceClasses = new HashMap<>();
        Enumeration<URL> urls = null;
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
        } catch (IOException e) {
            fail("Error locating configuration files", e);
        }

        if (urls != null) {
            while (urls.hasMoreElements()) {
                URL resourceUrl = urls.nextElement();
                loadResource(extensionServiceClasses, classLoader, resourceUrl);
            }
        }
        return extensionServiceClasses;
    }

    private void loadResource(final Map<String, Class<T>> extensionServiceClasses, final ClassLoader classLoader, final URL resourceUrl) {
        InputStream in = null;
        BufferedReader reader = null;
        try {
            in = resourceUrl.openStream();
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            Class<T> clazz = null;
            while ((line = parseLine(resourceUrl, reader)) != null) {
                try {
                    clazz = (Class<T>) Class.forName(line, false, classLoader);
                } catch (ClassNotFoundException ex) {
                    fail("ExtensionServiceProvider " + line + " not found");
                }
                if (!extensionServiceInterface.isAssignableFrom(clazz)) {
                    fail("ExtensionServiceProvider " + line + " not a subtype");
                }
                String providerKey = extractServiceKey(clazz);
                extensionServiceClasses.put(providerKey, clazz);
            }
        } catch (IOException ex) {
            fail("Error reading configuration file", ex);
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                fail("Error closing configuration file", ex);
            }
        }
    }

    private String parseLine(final URL url, final BufferedReader reader)
            throws IOException, ServiceConfigurationError {
        String line = reader.readLine();
        if (line == null) {
            return null;
        }
        int ci = line.indexOf('#');
        if (ci >= 0) {
            line = line.substring(0, ci);
        }
        line = line.trim();
        if (line.length() == 0) {
            return parseLine(url, reader);
        }
        if ((line.indexOf(' ') >= 0) || (line.indexOf('\t') >= 0)) {
            fail(url, "Illegal configuration-file syntax: " + line);
        }
        int cp = line.codePointAt(0);
        if (!Character.isJavaIdentifierStart(cp)) {
            fail(url, "Illegal extension-service-provider-class name: " + line);
        }

        for (int i = Character.charCount(cp); i < line.length(); i += Character.charCount(cp)) {
            cp = line.codePointAt(i);
            if (!Character.isJavaIdentifierPart(cp) && (cp != '.')) {
                fail(url, "Illegal extension-service-provider-class name: " + line);
            }
        }

        return line;
    }

    private String extractServiceKey(final Class<?> provider) {
        ExtensionServiceProvider extensionServiceProviderAnnotation = provider.getAnnotation(ExtensionServiceProvider.class);
        if (extensionServiceProviderAnnotation != null) {
            Class keyClass = extensionServiceProviderAnnotation.keyClass();
            if (Object.class != keyClass) {
                cachedKeyClasses.putIfAbsent(extensionServiceProviderAnnotation.keyClass(), extensionServiceProviderAnnotation.value());
            }
            return extensionServiceProviderAnnotation.value();
        }
        return provider.getSimpleName();
    }

    private void fail(final String errMessage) throws ServiceConfigurationError {
        throw new ServiceConfigurationError(extensionServiceInterface.getName() + ": " + errMessage);
    }

    private void fail(final URL url, final String errMessage) throws ServiceConfigurationError {
        fail(url + ": " + errMessage);
    }

    private void fail(final String msg, final Throwable cause) throws ServiceConfigurationError {
        throw new ServiceConfigurationError(extensionServiceInterface.getName() + ": " + msg, cause);
    }

}
