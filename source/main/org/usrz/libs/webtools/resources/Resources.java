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
package org.usrz.libs.webtools.resources;

import static org.usrz.libs.utils.Check.notNull;

import java.util.HashSet;
import java.util.Set;

public class Resources {

    private final Resource resource;
    private final Set<Resource> subResources;

    public Resources(Resource resource) {
        this.resource = notNull(resource, "Null resource");
        subResources = new HashSet<>();
    }

    public Resources with(Resource resource) {
        subResources.add(notNull(resource, "Null resource"));
        return this;
    }

    public Resource resource() {
        return resource;
    }

    public boolean hasChanged() {
        if (resource.hasChanged()) return true;
        for (Resource resource: subResources)
            if (resource.hasChanged()) return true;
        return false;
    }


}
