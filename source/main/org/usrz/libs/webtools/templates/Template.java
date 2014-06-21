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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

public interface Template {

    default String execute(Object scope)
    throws TemplateException {
        final StringWriter writer = new StringWriter();
        try {
            this.execute(writer, scope);
            writer.flush();
            return writer.toString();
        } catch (IOException exception) {
            throw new TemplateException("I/O error writing to StringWriter", exception);
        }
    }

    public void execute(Writer output, Object scope)
    throws IOException, TemplateException;

}
