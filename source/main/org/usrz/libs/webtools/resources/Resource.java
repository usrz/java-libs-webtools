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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.usrz.libs.utils.Check;

public final class Resource {

    private final Charset charset;
    private final File file;

    private long lastAccessedAt = -1;

    Resource(File file, Charset charset) {
        this.charset = Check.notNull(charset, "Null charset");
        this.file = Check.notNull(file, "Null file");
    }

    /* ====================================================================== */

    public Reader read() {
        return new BufferedReader(new InputStreamReader(stream(), charset));
    }

    public <W extends Writer> W read(W writer) {
        try {
            final Reader reader = read();
            final char[] buffer = new char[4096];
            int read;
            while ((read = reader.read(buffer)) >= 0)
                if (read > 0) writer.write(buffer, 0, read);
            reader.close();
            writer.flush();
            return writer;

        } catch (IOException exception) {
            throw new ResourceException("I/O error reading \"" + file + "\"", exception);
        }
    }

    public String readString() {
        return read(new StringWriter()).toString();
    }

    public InputStream stream() {
        try {
            final FileInputStream input = new FileInputStream(file);
            lastAccessedAt = file.lastModified();
            return new BufferedInputStream(input);
        } catch (IOException exception) {
            throw new ResourceException("I/O error reading \"" + file + "\"", exception);
        }
    }

    public <O extends OutputStream> O stream(O output) {
        try {
            final InputStream input = stream();
            final byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) >= 0)
                if (read > 0) output.write(buffer, 0, read);
            input.close();
            output.flush();
            return output;
        } catch (IOException exception) {
            throw new ResourceException("I/O error reading \"" + file + "\"", exception);
        }
    }

    public byte[] readBytes() {
        return stream(new ByteArrayOutputStream()).toByteArray();
    }

    /* ====================================================================== */

    public long lastModifiedAt() {
        return file.lastModified();
    }

    public boolean hasChanged() {
        System.err.println("LA => " + lastAccessedAt + "\nLM => " + lastModifiedAt());
        return lastAccessedAt != lastModifiedAt();
    }

    public File getFile() {
        return file;
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
