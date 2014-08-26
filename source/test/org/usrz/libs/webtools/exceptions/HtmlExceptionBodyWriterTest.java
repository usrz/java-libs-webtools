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

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.usrz.libs.configurations.Configurations;
import org.usrz.libs.configurations.ConfigurationsBuilder;
import org.usrz.libs.httpd.ServerStarter;
import org.usrz.libs.testing.NET;
import org.usrz.libs.webtools.utils.JsonMessageBodyWriter;

public class HtmlExceptionBodyWriterTest extends TestWithServer {

    @Override
    @BeforeClass(alwaysRun=true)
    public void before()
    throws Exception {
        port = NET.serverPort();
        final Configurations serverConfig = new ConfigurationsBuilder()
                    .put("server.listener.port", port)
                    .put("server.listener.host", "127.0.0.1")
                    .put("server.listener.secure", false)
                    .build();

        starter = new ServerStarter().start((builder) -> {
            builder.configure(serverConfig.strip("server"));
            builder.serveApp("/fail", (config) -> {
                config.register(FailResource.class);
                config.register(ExceptionWrapperMapper.class);
                config.register(HtmlExceptionWrapperBodyWriter.class);
                config.register(JsonMessageBodyWriter.class);
            });
        });

        Thread.sleep(1000);
    }

    /* ====================================================================== */

    @Test
    public void testHtmlWithHtmlPriority()
    throws Exception {
        request("/fail/html?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1")
            .assertStatus(500)
            .assertContentType("text/html; charset=UTF-8")
            .assertMatch("<title>HTTP/500: Internal Server Error</title>")
            .assertMatch("<h1>HTTP/500: Internal Server Error</h1>")
            .assertMatch("<dt>Reference:\\s*</dt><dd>" + UUID_PATTERN + "</dd>")
            .assertMatch("<dt>Exception:\\s*</dt><dd>java.lang.RuntimeException</dd>")
            .assertMatch("<dt>Message:\\s*</dt><dd>HTML Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</dd>")
            .assertMatch("<dt>Method:\\s*</dt><dd>GET</dd>")
            .assertMatch("<dt>URI:\\s*</dt><dd>http://127.0.0.1:\\d+/fail/html\\?p=")
            ;
    }

    @Test
    public void testHtmlWithJsonPriority()
    throws Exception {
        request("/fail/html?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9")
            .assertStatus(500)
            .assertContentType("text/html; charset=UTF-8")
            .assertMatch("<title>HTTP/500: Internal Server Error</title>")
            .assertMatch("<h1>HTTP/500: Internal Server Error</h1>")
            .assertMatch("<dt>Reference:\\s*</dt><dd>" + UUID_PATTERN + "</dd>")
            .assertMatch("<dt>Exception:\\s*</dt><dd>java.lang.RuntimeException</dd>")
            .assertMatch("<dt>Message:\\s*</dt><dd>HTML Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</dd>")
            .assertMatch("<dt>Method:\\s*</dt><dd>GET</dd>")
            .assertMatch("<dt>URI:\\s*</dt><dd>http://127.0.0.1:\\d+/fail/html\\?p=")
            ;
    }

    @Test
    public void testJsonWithHtmlPriority()
    throws Exception {
        request("/fail/json?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1")
            .assertStatus(500)
            .assertContentType("application/json; charset=UTF-8")
            .assertMatch("\"status_code\":500")
            .assertMatch("\"status_reason\":\"Internal Server Error\"")
            .assertMatch("\"reference\":\"" + UUID_PATTERN + "\"")
            .assertMatch("\"exception_type\":\"java.lang.RuntimeException\"")
            .assertMatch("\"exception_message\":\"JSON Error Parameter: <div class=\\\\\"foobar\\\\\"/>-\u6771\u4EAC\"")
            .assertMatch("\"request_method\":\"GET\"")
            .assertMatch("\"request_uri\":\"http://127.0.0.1:\\d+/fail/json\\?p=")
            ;
    }

    @Test
    public void testJsonWithJsonPriority()
    throws Exception {
        request("/fail/json?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9")
            .assertStatus(500)
            .assertContentType("application/json; charset=UTF-8")
            .assertMatch("\"status_code\":500")
            .assertMatch("\"status_reason\":\"Internal Server Error\"")
            .assertMatch("\"reference\":\"" + UUID_PATTERN + "\"")
            .assertMatch("\"exception_type\":\"java.lang.RuntimeException\"")
            .assertMatch("\"exception_message\":\"JSON Error Parameter: <div class=\\\\\"foobar\\\\\"/>-\u6771\u4EAC\"")
            .assertMatch("\"request_method\":\"GET\"")
            .assertMatch("\"request_uri\":\"http://127.0.0.1:\\d+/fail/json\\?p=")
            ;
    }

    @Test
    public void testNegotiateWithHtmlPriority()
    throws Exception {
        request("/fail/negotiate?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1")
            .assertStatus(500)
            .assertContentType("text/html; charset=UTF-8")
            .assertMatch("<title>HTTP/500: Internal Server Error</title>")
            .assertMatch("<h1>HTTP/500: Internal Server Error</h1>")
            .assertMatch("<dt>Reference:\\s*</dt><dd>" + UUID_PATTERN + "</dd>")
            .assertMatch("<dt>Exception:\\s*</dt><dd>java.lang.RuntimeException</dd>")
            .assertMatch("<dt>Message:\\s*</dt><dd>Negotiated Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</dd>")
            .assertMatch("<dt>Method:\\s*</dt><dd>GET</dd>")
            .assertMatch("<dt>URI:\\s*</dt><dd>http://127.0.0.1:\\d+/fail/negotiate\\?p=")
            ;
    }

    @Test
    public void testNegotiateWithJsonPriority()
    throws Exception {
        request("/fail/negotiate?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9")
            .assertStatus(500)
            .assertContentType("application/json; charset=UTF-8")
            .assertMatch("\"status_code\":500")
            .assertMatch("\"status_reason\":\"Internal Server Error\"")
            .assertMatch("\"reference\":\"" + UUID_PATTERN + "\"")
            .assertMatch("\"exception_type\":\"java.lang.RuntimeException\"")
            .assertMatch("\"exception_message\":\"Negotiated Error Parameter: <div class=\\\\\"foobar\\\\\"/>-\u6771\u4EAC\"")
            .assertMatch("\"request_method\":\"GET\"")
            .assertMatch("\"request_uri\":\"http://127.0.0.1:\\d+/fail/negotiate\\?p=")
            ;
    }

    @Test
    public void testWebAppWithHtmlPriority()
    throws Exception {
        request("/fail/webapp?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1")
            .assertStatus(410)
            .assertContentType("text/html; charset=UTF-8")
            .assertMatch("<title>HTTP/410: Gone</title>")
            .assertMatch("<h1>HTTP/410: Gone</h1>")
            .assertMatch("<p>WebApp Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</p>")
            .assertMatch("<dt>Reference:\\s*</dt><dd>" + UUID_PATTERN + "</dd>")
            .assertMatch("<dt>Method:\\s*</dt><dd>GET</dd>")
            .assertMatch("<dt>URI:\\s*</dt><dd>http://127.0.0.1:\\d+/fail/webapp\\?p=")
            .assertNotMatch("<dt>Exception:\\s*</dt>")
            .assertNotMatch("<dt>Message:\\s*</dt>")
            ;
    }

    @Test
    public void testWebAppWithJsonPriority()
    throws Exception {
        request("/fail/webapp?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9")
            .assertStatus(410)
            .assertContentType("application/json; charset=UTF-8")
            .assertMatch("\"status_code\":410")
            .assertMatch("\"status_reason\":\"Gone\"")
            .assertMatch("\"reference\":\"" + UUID_PATTERN + "\"")
            .assertMatch("\"message\":\"WebApp Error Parameter: <div class=\\\\\"foobar\\\\\"/>-\u6771\u4EAC\"")
            .assertMatch("\"request_method\":\"GET\"")
            .assertMatch("\"request_uri\":\"http://127.0.0.1:\\d+/fail/webapp\\?p=")
            .assertNotMatch("\"exception_type\"")
            .assertNotMatch("\"exception_message\"")
            ;
    }

    @Test
    public void testTemplatedWithHtmlPriority()
    throws Exception {
        request("/fail/template?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.9,application/json;q=0.1")
            .assertStatus(403)
            .assertContentType("text/html; charset=UTF-8")
            .assertMatch("<title>HTTP/403: Forbidden</title>")
            .assertMatch("<h1>HTTP/403: Forbidden</h1>")
            .assertMatch("<p>Templated Error Parameter: &lt;div class=&quot;foobar&quot;/&gt;-\u6771\u4EAC</p>")
            .assertMatch("<dt>Reference:\\s*</dt><dd>" + UUID_PATTERN + "</dd>")
            .assertMatch("<dt>Method:\\s*</dt><dd>GET</dd>")
            .assertMatch("<dt>URI:\\s*</dt><dd>http://127.0.0.1:\\d+/fail/template\\?p=")
            .assertNotMatch("<dt>Exception:\\s*</dt>")
            .assertNotMatch("<dt>Message:\\s*</dt>")
            ;
    }

    @Test
    public void testTemplatedWithJsonPriority()
    throws Exception {
        request("/fail/template?p=%3Cdiv+class=%22foobar%22%2F%3E-%e6%9d%b1%e4%ba%ac", "text/html;q=0.1,application/json;q=0.9")
            .assertStatus(403)
            .assertContentType("application/json; charset=UTF-8")
            .assertMatch("\"status_code\":403")
            .assertMatch("\"status_reason\":\"Forbidden\"")
            .assertMatch("\"reference\":\"" + UUID_PATTERN + "\"")
            .assertMatch("\"message\":\"Templated Error Parameter: <div class=\\\\\"foobar\\\\\"/>-\u6771\u4EAC\"")
            .assertMatch("\"request_method\":\"GET\"")
            .assertMatch("\"request_uri\":\"http://127.0.0.1:\\d+/fail/template\\?p=")
            .assertNotMatch("\"exception_type\"")
            .assertNotMatch("\"exception_message\"")
            ;
    }

    @Test
    public void test204()
    throws Exception {
        request("/fail/204")
            .assertStatus(204)
            .assertContentType(null)
            .assertMatch("^$");
    }

    @Test
    public void test307()
    throws Exception {
        request("/fail/307")
            .assertStatus(307)
            .assertContentType(null)
            .assertMatch("^$");
    }
}
