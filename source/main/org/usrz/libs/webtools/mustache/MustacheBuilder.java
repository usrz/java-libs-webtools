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

import org.usrz.libs.utils.inject.ConfiguringBindingBuilder;
import org.usrz.libs.webtools.templates.TemplateManager;

import com.google.inject.Binder;

public class MustacheBuilder extends ConfiguringBindingBuilder<MustacheBuilder> {

    public MustacheBuilder(Binder binder) {
        super(binder, MustacheConfigurations.class);
        binder.bind(TemplateManager.class).toProvider(MustacheTemplateManagerProvider.class);
    }

}
