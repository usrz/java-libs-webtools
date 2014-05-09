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
package org.usrz.libs.webtools;

import static org.usrz.libs.utils.Charsets.UTF8;

import java.io.File;

import javax.ws.rs.core.MediaType;

import org.testng.annotations.Test;
import org.usrz.libs.testing.AbstractTest;

public class MediaTypesTest extends AbstractTest {

    @Test
    public void testMediaTypes() {
        final MediaType javascript = new MediaType("application", "javascript");

        assertEquals(MediaTypes.get(new File("test.js")), javascript);
        assertEquals(MediaTypes.get("test.js"), javascript);
        assertEquals(MediaTypes.get(".js"), javascript);
        assertEquals(MediaTypes.get("js"), javascript);

    }

    @Test
    public void testMediaTypesAsString() {
        assertEquals(MediaTypes.get(new File("test.js")).toString(), "application/javascript");
        assertEquals(MediaTypes.get("test.js").toString(), "application/javascript");
        assertEquals(MediaTypes.get(".js").toString(), "application/javascript");
        assertEquals(MediaTypes.get("js").toString(), "application/javascript");
    }

    @Test
    public void testMediaTypesWithCharset() {
        assertEquals(MediaTypes.get(new File("test.js")).withCharset(UTF8.name()).toString(), "application/javascript;charset=UTF-8");
        assertEquals(MediaTypes.get("test.js").withCharset(UTF8.name()).toString(), "application/javascript;charset=UTF-8");
        assertEquals(MediaTypes.get(".js").withCharset(UTF8.name()).toString(), "application/javascript;charset=UTF-8");
        assertEquals(MediaTypes.get("js").withCharset(UTF8.name()).toString(), "application/javascript;charset=UTF-8");
    }
}
