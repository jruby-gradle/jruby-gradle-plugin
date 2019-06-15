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
