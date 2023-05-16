/*
 * Copyright (c) 2014-2023, R. Tyler Croy <rtyler@brokenco.de>,
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
package com.github.jrubygradle.internal.core

import com.github.jrubygradle.api.core.GemRepositoryConfiguration
import groovy.transform.CompileStatic
import org.gradle.api.Project

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** Allow only one version of the proxy to run.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class IvyXmlGlobalProxyRegistry {

    /** Create the registry and associate it with a Gradle project.
     *
     * @param project Associated project.
     */
    IvyXmlGlobalProxyRegistry(Project project) {
        rootCacheDir = new File(project.gradle.gradleUserHomeDir, "rubygems-ivyxml-cache/${PluginMetadata.version()}")
        refresh = project.gradle.startParameter.refreshDependencies
    }

    /** Registers a URI and group to be server via the proxy.
     *
     * @param remoteURI URI of remote Rubygems server.
     * @param group Group name for GEMs that will be fected from the remote.
     * @return Access to a proxy sever. If the server does not exist it will be created.
     */
    com.github.jrubygradle.api.core.IvyXmlProxyServer registerProxy(
        URI remoteURI,
        String group,
        GemRepositoryConfiguration grc
    ) {
        com.github.jrubygradle.api.core.IvyXmlProxyServer proxy = getOrCreateServer(
            remoteURI,
            group,
            new File(rootCacheDir, uriHash(remoteURI)),
            grc
        )
        proxy.refreshDependencies = refresh
        proxy
    }

    private String uriHash(URI remoteURI) {
        MessageDigest.getInstance('SHA-1').digest(remoteURI.toString().bytes).encodeHex().toString()
    }

    @SuppressWarnings('ClosureAsLastMethodParameter')
    static private com.github.jrubygradle.api.core.IvyXmlProxyServer getOrCreateServer(
        URI uri,
        String group,
        File cacheDir,
        GemRepositoryConfiguration grc
    ) {
        SERVER_MAP.computeIfAbsent(uri, {
            com.github.jrubygradle.api.core.IvyXmlProxyServer server = createProxyServer(uri, group, cacheDir, grc)
            server.run()
            server
        })
    }

    static private com.github.jrubygradle.api.core.IvyXmlProxyServer createProxyServer(
        URI uri,
        String group,
        File cacheDir,
        GemRepositoryConfiguration grc
    ) {
        new IvyXmlRatpackProxyServer(cacheDir, uri, group, grc)
    }

    static private final ConcurrentMap<URI, com.github.jrubygradle.api.core.IvyXmlProxyServer> SERVER_MAP = new ConcurrentHashMap<>()

    private final boolean refresh
    private final File rootCacheDir
}
