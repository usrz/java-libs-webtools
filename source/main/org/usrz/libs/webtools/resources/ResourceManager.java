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

import static org.usrz.libs.utils.Charsets.UTF8;
import static org.usrz.libs.utils.Check.notNull;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.usrz.libs.logging.Log;

public class ResourceManager {

    private final Log log = new Log();
    private final Charset charset;
    private final File root;

    public ResourceManager(File root) {
        this(root, UTF8);
    }

    public ResourceManager(File root, Charset charset) {
        this.charset = notNull(charset, "Null charset");
        if (! notNull(root, "Null root directory").isDirectory())
            throw new ResourceException("Invalid root directory \"" + root + "\"");
        try {
            this.root = root.getCanonicalFile();
        } catch (IOException exception) {
            throw new ResourceException("I/O error checking root directory \"" + root + "\"", exception);
        }
        log.debug("ResourceManager rooted at %s using default charset %s", this.root, this.charset.name());
    }

    public File getRootPath() {
        return root;
    }

    public Charset getDefaultCharset() {
        return charset;
    }

    public Resource getResource(String fileName) {
        return getResource(new File(root, fileName));
    }

    public Resource getResource(File file) {
        try {
            final File canonical = file.getCanonicalFile();

            /* Check that we're returning a file */
            if (!canonical.exists()) {
                return null;
            } else if (!canonical.isFile()) {
                throw new ResourceException("Resource \"" + file + "\" is not a file");
            }

            /* Check that we are returning a child of the root directory */
            File parent = canonical.getParentFile();
            while (parent != null) {
                if (parent.equals(root)) break;
                parent = parent.getParentFile();
            }
            if (parent == null) {
                throw new ResourceException("Resource \"" + file + "\" not under root \"" + root + "\"");
            }

            /* Return our resource */
            return new Resource(this, canonical, charset);

        } catch (IOException exception) {
            throw new ResourceException("I/O error accessing \"" + file + "\"");
        }

    }
}
