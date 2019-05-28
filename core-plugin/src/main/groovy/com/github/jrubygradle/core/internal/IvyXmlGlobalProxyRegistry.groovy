package com.github.jrubygradle.core.internal

import com.github.jrubygradle.core.IvyXmlProxyServer
import groovy.transform.CompileStatic
import org.gradle.api.Project

import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/** Allow only one version of the proxy to run
 *
 */
@CompileStatic
class IvyXmlGlobalProxyRegistry {

    IvyXmlGlobalProxyRegistry(Project project) {
        rootCacheDir = new File(project.gradle.gradleUserHomeDir, 'rubygems-ivyxml-cache')
        refresh = project.gradle.startParameter.refreshDependencies
    }

    IvyXmlProxyServer registerProxy(URI remoteURI, String group) {
        IvyXmlProxyServer proxy = getOrCreateServer(remoteURI, group, new File(rootCacheDir, uriHash(remoteURI)))
        proxy.refreshDependencies = refresh
        proxy
    }

    private String uriHash(URI remoteURI) {
        MessageDigest.getInstance('SHA-1').digest(remoteURI.toString().bytes).encodeHex().toString()
    }

    @SuppressWarnings('ClosureAsLastMethodParameter')
    static private IvyXmlProxyServer getOrCreateServer(
        URI uri,
        String group,
        File cacheDir
    ) {
        SERVER_MAP.computeIfAbsent(uri, {
            IvyXmlProxyServer server = createProxyServer(uri, group, cacheDir)
            server.run()
            server
        })
    }

    static private IvyXmlProxyServer createProxyServer(
        URI uri,
        String group,
        File cacheDir
    ) {
        new IvyXmlRatpackProxyServer(cacheDir, uri, group)
    }

    static private final ConcurrentMap<URI, IvyXmlProxyServer> SERVER_MAP = new ConcurrentHashMap<>()

    private final boolean refresh
    private final File rootCacheDir
}
