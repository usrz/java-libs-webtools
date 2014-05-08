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

public class LessTest extends AbstractTest {

    @Test
    public void testLess()
    throws Exception {
        final String less = new String(IO.read("test.less"), UTF8);
        final String css = new String(IO.read("test.css"), UTF8);
        final String result = new LessCSS().convert(less, false);
        assertEquals(result, css);
    }

    @Test
    public void testLessCompressed()
    throws Exception {
        final String less = new String(IO.read("test.less"), UTF8);
        final String css = new String(IO.read("test.min.css"), UTF8);
        final String result = new LessCSS().convert(less, true);
        assertEquals(result, css);
    }

}
