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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Extension service loader.
 *
 */
public final class ExtensionServiceLoader {

    private static final ConcurrentMap<Class<?>, Holder<ExtensionService>> EXTENSION_SERVICE_HOLDERS = new ConcurrentHashMap<>(8);

    /**
     * load a extension service.
     *
     *
     * @param extensionServiceInterface extension service interface type
     * @param <T> type
     * @return extension service
     */
    public static <T> ExtensionService load(final Class<T> extensionServiceInterface) {
        if (extensionServiceInterface == null) {
            throw new IllegalArgumentException("Extension service interface is not null");
        } else if (!extensionServiceInterface.isInterface()) {
            throw new IllegalArgumentException("(" + extensionServiceInterface.getName() + ") is not an interface!");
        } else if (!extensionServiceInterface.isAnnotationPresent(org.apache.shardingsphere.elasticjob.infra.spi.annotation.ExtensionService.class)) {
            throw new IllegalArgumentException("(" + extensionServiceInterface + ") is not an elastic job extension service interface, "
                    + "because it is not annotated with @" + org.apache.shardingsphere.elasticjob.infra.spi.annotation.ExtensionService.class.getName() + "!");
        }
        Holder<ExtensionService> holder = EXTENSION_SERVICE_HOLDERS.get(extensionServiceInterface);
        if (holder == null) {
            EXTENSION_SERVICE_HOLDERS.putIfAbsent(extensionServiceInterface, new Holder<>());
            holder = EXTENSION_SERVICE_HOLDERS.get(extensionServiceInterface);
        }
        ExtensionService extensionService = holder.get();
        if (extensionService == null) {
            synchronized (holder) {
                extensionService = holder.get();
                if (extensionService == null) {
                    extensionService = new ExtensionService<T>(extensionServiceInterface);
                    holder.set(extensionService);
                }
            }
        }
        return extensionService;
    }

}
