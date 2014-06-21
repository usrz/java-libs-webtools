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

import static org.usrz.libs.utils.Check.notNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.usrz.libs.logging.Log;
import org.usrz.libs.webtools.resources.Resource;
import org.usrz.libs.webtools.resources.ResourceManager;
import org.usrz.libs.webtools.resources.Resources;
import org.usrz.libs.webtools.templates.Template;
import org.usrz.libs.webtools.templates.TemplateException;
import org.usrz.libs.webtools.templates.TemplateManager;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheException;
import com.google.common.cache.ForwardingLoadingCache;
import com.google.common.cache.LoadingCache;

public class MustacheTemplateManager
extends MustacheTemplateFactory
implements TemplateManager {

    private static final Log log = new Log();

    private final Factory factory;

    /* ====================================================================== */

    public MustacheTemplateManager(File root) {
        this(new ResourceManager(root));
    }

    public MustacheTemplateManager(File root, Charset charset) {
        this(new ResourceManager(root, charset));
    }

    public MustacheTemplateManager(ResourceManager manager) {
        super(new Factory(manager));
        factory = (Factory) super.factory;
    }

    /* ====================================================================== */

    @Override
    public boolean canCompile(String name) {
        final Resource resource = factory.manager.getResource(name);
        if (resource != null) return true;
        if (name.endsWith(".mustache")) return false;
        return factory.manager.getResource(name + ".mustache") != null;
    }

    @Override
    public Template compile(String name) {
        final Resource resource = factory.manager.getResource(name);
        if (resource != null) return new MustacheTemplate(this, name);
        if (name.endsWith(".mustache")) return new MustacheTemplate(this, name);
        return new MustacheTemplate(this, name + ".mustache");
    }

    /* ====================================================================== */

    protected Entry<Mustache, Resources> compileTemplate(String name) {

        factory.currentResources.remove();
        final Mustache mustache = factory.compile(name);
        Resources resources = factory.currentResources.get();
        factory.currentResources.remove();

        /* If the template was cached, we return whatever we have */
        if (resources == null) {
            resources = factory.cachedResources.get(name);
        } else {
            factory.cachedResources.put(name, resources); // Remember the resource
        }

        /* If we got something, remember the resources associated with this */
        if (resources != null) return new SimpleImmutableEntry<>(mustache, resources);

        /* No resources (read or cached)... Fail */
        throw new TemplateException("No resources associated with \"" + name + "\"");
    }


    /* ====================================================================== */

    private static final class Factory extends DefaultMustacheFactory {

        private final ConcurrentHashMap<String, Resources> cachedResources = new ConcurrentHashMap<>();
        private final ThreadLocal<Resources> currentResources = new ThreadLocal<>();
        private final ResourceManager manager;

        private Factory(ResourceManager manager) {
            this.manager = notNull(manager, "Null resource manager");
        }

        @Override
        public Reader getReader(String resourceName) {
            log.debug("Reading resource \"%s\"", resourceName);
            final Resource resource = manager.getResource(resourceName);
            if (resource == null)
                    throw new MustacheException("Mustache Resource \"" + resourceName + "\" not found",
                                                new FileNotFoundException(resourceName));
            final Resources current = currentResources.get();
            if (current == null) {
                currentResources.set(new Resources(resource));
            } else {
                current.with(resource);
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
                    final Resources resources = cachedResources.get(key);
                    if (resources == null) {
                        log.debug("Cached template for unknown resource \"%s\"", key);
                        cache.invalidate(key);
                        return cache;
                    }

                    /* Check each resource */
                    if (resources.hasChanged()) {
                        log.debug("A resource associated with \"%s\" was modified", key);
                        cachedResources.remove(key);
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
