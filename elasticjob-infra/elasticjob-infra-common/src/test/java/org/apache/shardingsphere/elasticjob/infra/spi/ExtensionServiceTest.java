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

import org.apache.shardingsphere.elasticjob.infra.exception.JobConfigurationException;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

public class ExtensionServiceTest {

    private final ExtensionService<FooService> extensionService = ExtensionServiceLoader.load(FooService.class);

    @Test
    public void assertGetDefaultProviderWithNullKey() {
        FooService fooService = extensionService.getProvider((String) null);
        Assert.assertEquals("FooServiceProvider0", fooService.getName());
    }

    @Test
    public void assertGetProviderByKey() {
        FooService fooService = extensionService.getProvider("FooServiceProvider1");
        Assert.assertEquals("FooServiceProvider1", fooService.getName());
    }

    @Test
    public void assertGetProviderByKeyClass() {
        FooService fooService = extensionService.getProvider(Foo.FooKeyClass.class);
        Assert.assertEquals("FooServiceProvider0", fooService.getName());
    }

    @Test
    public void assertGetOptionalProviderByKey() {
        Optional<FooService> optionalFooService = extensionService.getOptionalProvider("FooServiceProvider1");
        Assert.assertEquals("FooServiceProvider1", optionalFooService.get().getName());
    }

    @Test
    public void assertGetOptionalProviderByKeyClass() {
        Optional<FooService> optionalFooService = extensionService.getOptionalProvider(Foo.FooKeyClass.class);
        Assert.assertEquals("FooServiceProvider0", optionalFooService.get().getName());
    }

    @Test
    public void assertRepeatedGetProviderByKey() {
        FooService fooService = extensionService.getProvider("FooServiceProvider1");
        Assert.assertEquals(fooService, extensionService.getProvider("FooServiceProvider1"));
    }

    @Test
    public void assertRepeatedGetProviderByKeyClass() {
        FooService fooService = extensionService.getProvider(Foo.FooKeyClass.class);
        Assert.assertEquals(fooService, extensionService.getProvider(Foo.FooKeyClass.class));
    }

    @Test
    public void assertGetSameProviderByDiffTypeKey() {
        FooService fooService = extensionService.getProvider(Foo.FooKeyClass.class);
        Assert.assertEquals(fooService, extensionService.getProvider("FooServiceProvider0"));
    }

    @Test
    public void assertGetDiffProviderByDiffKey() {
        FooService fooService = extensionService.getProvider("FooServiceProvider0");
        Assert.assertNotEquals(fooService, extensionService.getProvider("FooServiceProvider1"));
    }

    @Test(expected = JobConfigurationException.class)
    public void assertGetProviderByKeyFailureWithInvalidKey() {
        extensionService.getProvider("FAIL");
    }
}
