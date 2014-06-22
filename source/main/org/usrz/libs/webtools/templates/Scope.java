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
package org.usrz.libs.webtools.templates;

import java.util.HashMap;
import java.util.Map;

public interface Scope {

    public Map<String, Object> compute(TemplateFactory templates);

    public static ScopeBuilder builder() {
        final Map<String, Object> properties = new HashMap<>();
        final Map<String, String> partials = new HashMap<>();
        return new ScopeBuilder() {

            @Override
            public ScopeBuilder with(String key, Object value) {
                if (key == null) return this;
                if (value == null) properties.remove(key);
                else properties.put(key, value);
                return this;
            }

            @Override
            public ScopeBuilder partial(String name, String partial) {
                if (name == null) return this;
                if (partial == null) partials.remove(name);
                else partials.put(name, partial);
                return this;
            }

            @Override
            public Scope build() {
                return (templates) -> {
                    final Map<String, Object> scope = new HashMap<>();
                    scope.putAll(properties);
                    partials.forEach((name, partial) ->
                        scope.put(name, templates.parse(partial).execute(properties)));
                    System.err.println("SCOPE CONTAINS -> " + scope);
                    return scope;
                };
            }
        };
    }

    public interface ScopeBuilder extends Scoping<ScopeBuilder> {

        public Scope build();

    }

}
