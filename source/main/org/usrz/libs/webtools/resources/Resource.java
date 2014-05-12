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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.usrz.libs.logging.Log;
import org.usrz.libs.utils.Check;

public final class Resource {

    private final Set<Resource> subResources = new HashSet<>();
    private final Log log = new Log();
    private final Charset charset;
    private final File file;

    private long lastModified = -1;

    protected Resource(File file, Charset charset) {
        this.charset = Check.notNull(charset, "Null charset");
        this.file = Check.notNull(file, "Null file");
    }

    public String readString() {
        final Reader reader = read();
        try {
            final StringWriter writer = new StringWriter();
            final char[] buffer = new char[65536];
            int read = -1;
            while ((read = reader.read(buffer)) >= 0)
                writer.write(buffer, 0, read);
            reader.close();
            writer.close();
            return writer.toString();

        } catch (IOException exception) {
            lastModified = -1;
            throw new ResourceException("I/O error reading \"" + file + "\"", exception);
        }
    }

    public Reader read() {
        try {
            final FileInputStream input = new FileInputStream(file);
            final Reader reader = new InputStreamReader(input, charset);
            log.debug("Reading file \"%s\"", file);
            lastModified = file.lastModified();
            return new BufferedReader(reader);
        } catch (IOException exception) {
            lastModified = -1;
            throw new ResourceException("I/O error reading \"" + file + "\"", exception);
        }
    }

    public long lastModified() {
        final AtomicLong modified = new AtomicLong(lastModified < 0 ? file.lastModified() : lastModified);
        subResources.forEach((resource) -> {
            final long current = resource.lastModified();
            modified.getAndUpdate((previous) -> current > previous ? current : previous);
        });
        return modified.get();
    }

    public boolean hasChanged() {
        if (lastModified < 0) return true;
        if (lastModified != file.lastModified()) {
            log.debug("File \"%s\" was modified since last read", file);
            return true;
        }
        return subResources.stream().anyMatch((r) -> r.hasChanged());
    }

    public void addSubResource(Resource resource) {
        subResources.add(notNull(resource, "Null sub-resource"));
    }

    public File getFile() {
        return file;
    }

    public Set<Resource> getSubResources() {
        return Collections.unmodifiableSet(subResources);
    }

    /* ====================================================================== */

    @Override
    public int hashCode() {
        return this.getClass().hashCode() ^ file.hashCode();
    }

    @Override
    public String toString() {
        return this.getClass().getName() + '[' + file + ']';
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object == this) return true;
        try {
            return file.equals(((Resource) object).file);
        } catch (ClassCastException exception) {
            return false;
        }
    }
}
