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

import static java.util.Collections.singletonMap;
import static org.usrz.libs.utils.Charsets.UTF8;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.usrz.libs.testing.AbstractTest;
import org.usrz.libs.testing.IO;
import org.usrz.libs.webtools.mustache.ReloadingMustacheTemplate;
import org.usrz.libs.webtools.mustache.ReloadingMustacheFactory;

public class MustacheTest extends AbstractTest {

    private final byte[] template = "Hello{{>included}}world! {{name}}".getBytes(UTF8);
    private final byte[] included1 = ", ".getBytes(UTF8);
    private final byte[] included2 = " to everyone in the ".getBytes(UTF8);

    private File root;
    private ReloadingMustacheFactory factory;

    @BeforeClass
    public void before()
    throws IOException {
        root = IO.makeTempDir();
        factory = new ReloadingMustacheFactory(root);
    }

    @Test
    public void testInineMustache()
    throws Exception {
        final String template = "Hello, {{name}}!";
        assertEquals(factory.execute(template, singletonMap("name", "world")), "Hello, world!");
        assertEquals(factory.execute(template, singletonMap("name", "Pier")), "Hello, Pier!");
    }

    @Test
    public void testMustache()
    throws Exception {
        final long now = System.currentTimeMillis();
        final File file = new File(root, "included.mustache");

        IO.copy(template, new File(root, "template.mustache"));
        IO.copy(included1, file);
        file.setLastModified(now - 10000);

        /* Compile and check we have what we want (first) */
        final Map<String, Object> scopes1 = Collections.singletonMap("name", "(one)");
        final ReloadingMustacheTemplate mustache1 = factory.compile("template.mustache");
        assertEquals(mustache1.execute(scopes1), "Hello, world! (one)");

        /* Change the file but keep the old time stamp */
        IO.copy(included2, file);
        file.setLastModified(now - 10000);

        /* No changes should happen, either recompiling or re-applying */
        final Map<String, Object> scopes2 = Collections.singletonMap("name", "(two)");
        final ReloadingMustacheTemplate mustache2 = factory.compile("template.mustache");
        assertEquals(mustache1.execute(scopes2), "Hello, world! (two)");
        assertEquals(mustache2.execute(scopes2), "Hello, world! (two)");

        /* Touch the file and verify the changes */
        file.setLastModified(System.currentTimeMillis());
        final Map<String, Object> scopes3 = Collections.singletonMap("name", "(three)");
        final ReloadingMustacheTemplate mustache3 = factory.compile("template.mustache");
        assertEquals(mustache1.execute(scopes3), "Hello to everyone in the world! (three)");
        assertEquals(mustache2.execute(scopes3), "Hello to everyone in the world! (three)");
        assertEquals(mustache3.execute(scopes3), "Hello to everyone in the world! (three)");

    }
}
