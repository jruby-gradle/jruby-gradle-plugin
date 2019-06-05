package com.github.jrubygradle.internal.core


import groovy.transform.CompileStatic
import org.gradle.api.Project

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** Allow only one version of the proxy to run.
 *
 * @Since 2.0
 */
@CompileStatic
class IvyXmlGlobalProxyRegistry {

    /** Create the registry and associate it with a Gradle project.
     *
     * @param project Associated project.
     */
    IvyXmlGlobalProxyRegistry(Project project) {
        rootCacheDir = new File(project.gradle.gradleUserHomeDir, 'rubygems-ivyxml-cache')
        refresh = project.gradle.startParameter.refreshDependencies
    }

    /** Registers a URI and group to be server via the proxy.
     *
     * @param remoteURI URI of remote Rubygems server.
     * @param group Group name for GEMs that will be fected from the remote.
     * @return Access to a proxy sever. If the server does not exist it will be created.
     */
    com.github.jrubygradle.api.core.IvyXmlProxyServer registerProxy(URI remoteURI, String group) {
        com.github.jrubygradle.api.core.IvyXmlProxyServer proxy = getOrCreateServer(remoteURI, group, new File(rootCacheDir, uriHash(remoteURI)))
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
        File cacheDir
    ) {
        SERVER_MAP.computeIfAbsent(uri, {
            com.github.jrubygradle.api.core.IvyXmlProxyServer server = createProxyServer(uri, group, cacheDir)
            server.run()
            server
        })
    }

    static private com.github.jrubygradle.api.core.IvyXmlProxyServer createProxyServer(
        URI uri,
        String group,
        File cacheDir
    ) {
        new IvyXmlRatpackProxyServer(cacheDir, uri, group)
    }

    static private final ConcurrentMap<URI, com.github.jrubygradle.api.core.IvyXmlProxyServer> SERVER_MAP = new ConcurrentHashMap<>()

    private final boolean refresh
    private final File rootCacheDir
}
