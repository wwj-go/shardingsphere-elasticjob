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

package org.apache.shardingsphere.elasticjob.infra.spi.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class annotated by this annotation and has its fully qualified name in file(saved under  META-INF/services/) named by its SPI interface
 * will be considered as a elastic-job SPI provider, will be processed by {@link org.apache.shardingsphere.elasticjob.infra.spi.ExtensionService}.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ExtensionServiceProvider {
    /**
     * Return the key associated with the extension service provider instance.
     * @return the key associated with the extension service provider instance
     */
    String value();

    /**
     * Return the class associated with the extension service provider instance.
     * @return the class associated with the extension service provider instance
     */
    Class keyClass() default Object.class;
}
