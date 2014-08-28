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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;
import org.usrz.libs.webtools.lesscss.LessCSS;
import org.usrz.libs.webtools.resources.ResourceManager;

public class LessTest extends AbstractTest {

    private ResourceManager manager;
    private LessCSS lessc;

    @BeforeClass
    public void setupLess() {
        final File file = new File(this.getClass().getResource("import.less").getPath());
        final File directory = file.getParentFile();

        manager = new ResourceManager(directory, UTF8);
        lessc = new LessCSS();
    }

    @Test(priority=1)
    public void testLess()
    throws Exception {
        final String less = new String(IO.read("test.less"), UTF8);
        final String css = new String(IO.read("test.css"), UTF8);
        final String result = lessc.convert(less, false);
        assertEquals(result, css);
    }

    @Test(priority=2)
    public void testLessCompressed()
    throws Exception {
        final String less = new String(IO.read("test.less"), UTF8);
        final String css = new String(IO.read("test.min.css"), UTF8);
        final String result = lessc.convert(less, true);
        assertEquals(result, css);
    }

    @Test(priority=3)
    public void testLessImport()
    throws Exception {
        final String result = lessc.convert(manager.getResource("import.less"), true);
        final String css = new String(IO.read("import.min.css"), UTF8);
        assertEquals(result, css);
    }

    @Test(priority=999)
    public void testLessBootstrap()
    throws Exception {
        final String result = lessc.convert(manager.getResource("bootstrap/bootstrap.less"), true);
        final String css = new String(IO.read("bootstrap.min.css"), UTF8);
        assertEquals(result, css);
    }

}
