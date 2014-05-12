/* ========================================================================== *
 * Copyright 2014 USRZ.com and Pier Paolo Fumagalli                           *
 * -------------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License");            *
 * you may not use this file except in compliance with the License.           *
 * You may obtain a copy of the License at                                    *
 *                                                                            *
 *  http://www.apache.org/licenses/LICENSE-2.0                                *
 *                                                                            *
 * Unless required by applicable law or agreed to in writing, software        *
 * distributed under the License is distributed on an "AS IS" BASIS,          *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   *
 * See the License for the specific language governing permissions and        *
 * limitations under the License.                                             *
 * ========================================================================== */
package org.usrz.libs.webtools.mustache;

import java.io.File;
import java.io.Reader;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.usrz.libs.logging.Log;
import org.usrz.libs.webtools.resources.Resource;
import org.usrz.libs.webtools.resources.ResourceManager;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.google.common.cache.ForwardingLoadingCache;
import com.google.common.cache.LoadingCache;

public class ReloadingMustacheFactory {

    private final ConcurrentHashMap<String, Resource> resources = new ConcurrentHashMap<>();
    private final ThreadLocal<Resource> resourceLocal = new ThreadLocal<>();
    private final Log log = new Log();

    private final ResourceManager manager;
    private final Factory factory;

    public ReloadingMustacheFactory(File root) {
        manager = new ResourceManager(root);
        factory = new Factory(root);
    }

    public ReloadingMustache compile(String name) {
        return new ReloadingMustache(this, name);
    }

    protected Entry<Mustache, Resource> compileTemplate(String name) {

        resourceLocal.remove();
        final Mustache mustache = factory.compile(name);
        Resource resource = resourceLocal.get();
        resourceLocal.remove();

        /* If the template was cached, we return whatever we have */
        if (resource == null) resource = resources.get(name);

        /* Check if we compiled or returned a cached template */
        if (resource == null) {
            throw new MustacheException("No resources associated with \"" + name + "\"");
        } else {
            resources.put(name, resource); // Remember the resource
            return new SimpleImmutableEntry<>(mustache, resource);
        }
    }


    /* ====================================================================== */

    private final class Factory extends DefaultMustacheFactory {

        private Factory(File root) {
            super(root);
        }

        @Override
        public Reader getReader(String resourceName) {
            log.debug("Reading resource \"%s\"", resourceName);
            final Resource resource = manager.getResource(resourceName);
            final Resource current = resourceLocal.get();
            if (current == null) {
                resourceLocal.set(resource);
            } else {
                current.addSubResource(resource);
            }
            return resource.read();
        }

        @Override
        protected LoadingCache<String, Mustache> createMustacheCache() {
            final LoadingCache<String, Mustache> cache = super.createMustacheCache();
            return new ForwardingLoadingCache<String, Mustache>() {

                private LoadingCache<String, Mustache> possiblyInvalidate(String key) {

                    /* Not cached? Nothing to do! */
                    if (cache.getIfPresent(key) == null) return cache;

                    /* Get our resource, if null invalidate and return */
                    final Resource resource = resources.get(key);
                    if (resource == null) {
                        log.debug("Cached template for unknown resource \"%s\"", key);
                        cache.invalidate(key);
                        return cache;
                    }

                    /* Check each resource */
                    if (resource.hasChanged()) {
                        log.debug("A resource associated with \"%s\" was modified", key);
                        resources.remove(key);
                        cache.invalidate(key);
                        return cache;
                    }

                    /* Nothing modified, return cache unchanged */
                    return cache;
                }

                @Override
                public Mustache get(String key)
                throws ExecutionException {
                    return possiblyInvalidate(key).get(key);
                }

                @Override
                public Mustache getUnchecked(String key) {
                    return possiblyInvalidate(key).getUnchecked(key);
                }

                @Override
                protected LoadingCache<String, Mustache> delegate() {
                    return cache;
                }

            };
        }
    }

}
