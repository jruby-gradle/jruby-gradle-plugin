/**
 * Copyright (c) 2014-2019, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.internal.core;

import ratpack.server.RatpackServer;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;

import static ratpack.server.RatpackServer.start;
import static ratpack.server.ServerConfig.embedded;

/**
 * Uses Ratpack to run a small proxy server inside Gradle to proxy Rubygems.org
 * as if it is local Ivy server with remote artifacts.
 *
 * @author Schalk W. Cronjé
 * @since 2.0
 */
public class IvyXmlRatpackProxyServer extends AbstractIvyXmlProxyServer {
    /**
     * Implementation of a proxy server based upon Ratpack.
     *
     * @param cache     Root directory for local Ivy XML cache.
     * @param serverUri URI of remote Rubygems proxy.
     * @param group     Group that will be associated with the Rubygems proxy.
     */
    public IvyXmlRatpackProxyServer(File cache, URI serverUri, String group) {
        super(cache, serverUri, group);
    }

    /**
     * Start the proxy.
     */
    @Override
    public void run() {
        try {
            server = start(server -> server
                    .serverConfig(
                            embedded()
                                    .publicAddress(new URI("http://localhost"))
                                    .port(0)
                                    .baseDir(getLocalCachePath())
                    ).handlers(chain -> chain
                            .get(":group/:module/:revision/ivy.xml", ctx -> {
                                try {
                                    Path ivyXml = getIvyXml(
                                            ctx.getAllPathTokens().get("group"),
                                            ctx.getAllPathTokens().get("module"),
                                            ctx.getAllPathTokens().get("revision")
                                    );
                                    ctx.getResponse().contentType("text/xml").sendFile(ivyXml);
                                } catch (NotFound e) {
                                    ctx.clientError(404);
                                }
                            }).get(":group/:module/:revision/ivy.xml.sha1", ctx -> {
                                try {
                                    Path ivyXmlSha1 = getIvyXmlSha1(
                                            ctx.getAllPathTokens().get("group"),
                                            ctx.getAllPathTokens().get("module"),
                                            ctx.getAllPathTokens().get("revision")
                                    );
                                    ctx.getResponse().contentType("text/plain").sendFile(ivyXmlSha1);
                                } catch (NotFound e) {
                                    ctx.clientError(404);
                                }
                            }).get(":group/:module", ctx -> {
                                try {
                                    String listing = getDirectoryListing(
                                            ctx.getAllPathTokens().get("group"),
                                            ctx.getAllPathTokens().get("module")
                                    );
                                    ctx.getResponse().contentType("text/html").send(listing);
                                } catch (NotFound e) {
                                    ctx.clientError(404);
                                }
                            }).get(ctx -> ctx.clientError(403))
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Could not start Ratpack", e);
        }
    }

    @Override
    protected int getBindPort() {
        return server.getBindPort();
    }

    private RatpackServer server;
}
