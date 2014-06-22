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

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static javax.ws.rs.core.Response.Status.FORBIDDEN;
import static javax.ws.rs.core.Response.Status.GONE;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

@Path("/")
public class FailResource {

    @GET
    @Path("/json")
    @Produces(APPLICATION_JSON)
    public Response json(@QueryParam("p") String param) {
        throw new RuntimeException("JSON Error Parameter: " + param);
    }

    @GET
    @Path("/html")
    @Produces(TEXT_HTML)
    public Response html(@QueryParam("p") String param) {
        throw new RuntimeException("HTML Error Parameter: " + param);
    }

    @GET
    @Path("/negotiate")
    public Response foo(@QueryParam("p") String param) {
        throw new RuntimeException("Negotiated Error Parameter: " + param);
    }

    @GET
    @Path("/webapp")
    public Response webapp(@QueryParam("p") String param) {
        throw new WebApplicationException("WebApp Error Parameter: " + param, GONE);
    }

    @GET
    @Path("/template")
    public Response template(@QueryParam("p") String param) {
        throw new WebApplicationExceptionBuilder(FORBIDDEN)
                .message("Templated Error Parameter: " + param)
                .partial("partial", "The partial says {{param}}!")
                .with("param", param)
                .build();
    }

}