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
package org.usrz.libs.webtools.exceptions;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.testing.IO;
import org.usrz.libs.testing.NET;
import org.usrz.libs.webtools.exceptions.GeneralExceptionMapper;
import org.usrz.libs.webtools.exceptions.HtmlExceptionBodyWriter;
import org.usrz.libs.webtools.exceptions.JsonExceptionBodyWriter;
import org.usrz.libs.webtools.exceptions.TemplateExceptionBodyWriter;
import org.usrz.libs.webtools.exceptions.TemplatedException;
import org.usrz.libs.webtools.mustache.ReloadingMustacheFactory;
import org.usrz.libs.webtools.mustache.TemplateFactory;

public class TemplatedExceptionTest extends TestWithServer {

    @Override
    @BeforeClass(alwaysRun=true)
    public void before()
    throws Exception {
        final File root = IO.makeTempDir();
        final File errors = new File(root, "errors");
        errors.mkdirs();
        // encode "message", pass "partial" (as it's pre-rendered) as is
        IO.copy("404:S[{{status_reason}}]/M[{{message}}]/P[{{{partial}}}]".getBytes(), new File(errors, "404.mustache"));
        IO.copy("5xx:S[{{status_reason}}]/M[{{message}}]/P[{{{partial}}}]".getBytes(), new File(errors, "5xx.mustache"));
        IO.copy("ERR:S[{{status_reason}}]/M[{{message}}]/P[{{{partial}}}]".getBytes(), new File(errors, "error.mustache"));

        port = NET.serverPort();
        final Configurations serverConfig = new ConfigurationsBuilder()
                    .put("server.listener.port", port)
                    .put("server.listener.host", "127.0.0.1")
                    .put("server.listener.secure", false)
                    .build();

        starter = new ServerStarter().start((builder) -> {
            builder.install((binder) -> binder.bind(TemplateFactory.class).toInstance(new ReloadingMustacheFactory(root)));
            builder.configure(serverConfig.strip("server"));
            builder.serveApp("/fail", (config) -> {
                config.register(TestResource.class);
                config.register(GeneralExceptionMapper.class);
//                config.register(TemplatedExceptionMapper.class);
                config.register(JsonExceptionBodyWriter.class);
                config.register(HtmlExceptionBodyWriter.class);
                config.register(TemplateExceptionBodyWriter.class);
            });
        });

        Thread.sleep(1000);
    }

    /* ====================================================================== */

    @Test
    public void test400()
    throws Exception {
         assertResponse("/fail/t?s=400&m=my%2f%22%3c%3eMessage-%e6%9d%b1%e4%ba%ac&p=my%2f%22%3c%3ePartial-%e6%9d%b1%e4%ba%ac", "text/html", 400, "text/html; charset=UTF-8",
                        "ERR:S\\[Bad Request\\]/M\\[my/&quot;&lt;&gt;Message-\u6771\u4EAC\\]/P\\[<div>my/&quot;&lt;&gt;Partial-\u6771\u4EAC</div>\\]");
    }

    @Test
    public void test404()
    throws Exception {
         assertResponse("/fail/t?s=404&m=my%2f%22%3c%3eMessage-%e6%9d%b1%e4%ba%ac&p=my%2f%22%3c%3ePartial-%e6%9d%b1%e4%ba%ac", "text/html", 404, "text/html; charset=UTF-8",
                        "404:S\\[Not Found\\]/M\\[my/&quot;&lt;&gt;Message-\u6771\u4EAC\\]/P\\[<div>my/&quot;&lt;&gt;Partial-\u6771\u4EAC</div>\\]");
    }

    @Test
    public void test500()
    throws Exception {
         assertResponse("/fail/t?s=500&m=my%2f%22%3c%3eMessage-%e6%9d%b1%e4%ba%ac&p=my%2f%22%3c%3ePartial-%e6%9d%b1%e4%ba%ac", "text/html", 500, "text/html; charset=UTF-8",
                        "5xx:S\\[Internal Server Error\\]/M\\[my/&quot;&lt;&gt;Message-\u6771\u4EAC\\]/P\\[<div>my/&quot;&lt;&gt;Partial-\u6771\u4EAC</div>\\]");
    }

    @Test
    public void test503()
    throws Exception {
         assertResponse("/fail/t?s=503&m=my%2f%22%3c%3eMessage-%e6%9d%b1%e4%ba%ac&p=my%2f%22%3c%3ePartial-%e6%9d%b1%e4%ba%ac", "text/html", 503, "text/html; charset=UTF-8",
                        "5xx:S\\[Service Unavailable\\]/M\\[my/&quot;&lt;&gt;Message-\u6771\u4EAC\\]/P\\[<div>my/&quot;&lt;&gt;Partial-\u6771\u4EAC</div>\\]");
    }

    /* ====================================================================== */

    @Path("/")
    public static final class TestResource {

        @GET
        @Path("/t")
        public void get(@QueryParam("s") int status,
                        @QueryParam("m") String message,
                        @QueryParam("p") String parameter) {
            throw new TemplatedException.Builder()
                                        .status(status)
                                        .message(message)
                                        .partial("partial", "<div>{{parameter}}</div>")
                                        .put("parameter", parameter)
                                        .build();
        }
    }
}
