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

import org.testng.annotations.Test;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;

public class UglifyJS2Test extends AbstractTest {

    @Test
    public void testUglifyJS2()
    throws Exception {
        final String script = new String(IO.read("test.js"), UTF8);
        final String min = new String(IO.read("test.min.js"), UTF8);
        final String result = new UglifyJS2().convert(script, false, false);
        assertEquals(result, min);
    }

    @Test
    public void testUglifyJS2_Compress()
    throws Exception {
        final String script = new String(IO.read("test.js"), UTF8);
        final String min = new String(IO.read("test.c.min.js"), UTF8);
        final String result = new UglifyJS2().convert(script, true, false);
        assertEquals(result, min);
    }

    @Test
    public void testUglifyJS2_Mangle()
    throws Exception {
        final String script = new String(IO.read("test.js"), UTF8);
        final String min = new String(IO.read("test.m.min.js"), UTF8);
        final String result = new UglifyJS2().convert(script, false, true);
        assertEquals(result, min);
    }

    @Test
    public void testUglifyJS2_All()
    throws Exception {
        final String script = new String(IO.read("test.js"), UTF8);
        final String min = new String(IO.read("test.cm.min.js"), UTF8);
        final String result = new UglifyJS2().convert(script, true, true);
        assertEquals(result, min);
    }
}
