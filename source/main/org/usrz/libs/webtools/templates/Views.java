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

import static org.usrz.libs.utils.Check.notNull;

import java.io.Serializable;
import java.lang.annotation.Annotation;

public final class Views {

    private Views() {
        throw new IllegalStateException("Do not construct");
    }

    public static final View view(String value) {
        return new ViewImpl(notNull(value, "Null view name"));
    }

    @SuppressWarnings("all")
    private static class ViewImpl implements View, Serializable {

        private final String value;

        public ViewImpl(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public int hashCode() {
            return (127 * "value".hashCode()) ^ value.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if (object == null) return false;
            if (object == this) return true;
            try {
                return value.equals(((View) object).value());
            } catch (ClassCastException exception) {
                /* Nothing /*/
            }
            return false;
        }

        @Override
        public String toString() {
            return "@" + View.class.getName() + "(value=" + value + ")";
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return View.class;
        }
    }
}