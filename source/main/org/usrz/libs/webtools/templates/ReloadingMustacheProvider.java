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

import static org.usrz.libs.utils.Charsets.UTF8;

import java.nio.charset.Charset;

import javax.inject.Inject;

import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.utils.inject.ConfigurableProvider;
import org.usrz.libs.webtools.resources.ResourceManager;

public class ReloadingMustacheProvider extends ConfigurableProvider<ReloadingMustacheFactory> {

    @Inject
    private ReloadingMustacheProvider() {
        super(TemplatesConfigurations.class);
    }

    @Override
    protected ReloadingMustacheFactory get(Configurations configurations) {
        final Charset charset = Charset.forName(configurations.get("charset", UTF8.name()));
        final ResourceManager manager = new ResourceManager(configurations.requireFile("root_path"), charset);
        return new ReloadingMustacheFactory(manager);
    }
}
