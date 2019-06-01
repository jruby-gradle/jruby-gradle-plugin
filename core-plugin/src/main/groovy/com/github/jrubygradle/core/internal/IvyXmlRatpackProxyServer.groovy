package com.github.jrubygradle.core.internal

import com.github.jrubygradle.core.ApiException
import com.github.jrubygradle.core.GemInfo
import com.github.jrubygradle.core.GemVersion
import com.github.jrubygradle.core.IvyXmlProxyServer
import com.github.jrubygradle.core.RubyGemQueryRestApi
import groovy.transform.CompileStatic
import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
import org.ysb33r.grolifant.api.ExclusiveFileAccess
import ratpack.handling.RequestLogger
import ratpack.server.RatpackServer

import ratpack.server.ServerConfig

import java.nio.file.Files
import java.nio.file.Path

import static com.github.jrubygradle.core.GemVersion.gemVersionFromGradleRequirement
import static com.github.jrubygradle.core.internal.IvyUtils.revisionsAsHtmlDirectoryListing
import static java.nio.file.Files.move
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** Uses Ratpack to run a small proxy server inside Gradle to proxy Rubygems.org
 * as if it is local Ivy server with remote artifacts.
 *
 * @since 2.0
 */
@CompileStatic
@Slf4j
class IvyXmlRatpackProxyServer implements IvyXmlProxyServer {

    /** Implementation of a proxy server based upon Ratpack.
     *
     * @param cache Root directory for local Ivy XML cache.
     * @param serverUri URI of remote Rubygems proxy.
     * @param group Group that will be associated with the Rubygems proxy.
     */
    IvyXmlRatpackProxyServer(File cache, URI serverUri, String group) {
        localCachePath = cache
        gemToIvy = new GemToIvy(serverUri)
        api = new DefaultRubyGemRestApi(serverUri)
        this.group = group
    }

    /** Tell the server to refresh dependencies upon a next run.
     *
     * @param refresh {@code true} to reload dependencies.
     */
    @Override
    void setRefreshDependencies(boolean refresh) {
        refreshDependencies = refresh ? 1 : 0
    }

    /** Get the address of the local proxy.
     *
     * @return Local address as a URI.
     */
    @Override
    URI getBindAddress() {
        "http://localhost:${server.bindPort}".toURI()
    }

    /** Start the proxy.
     *
     */
    @Override
    @SuppressWarnings('DuplicateStringLiteral')
    void run() {
        server = RatpackServer.start {
            it.serverConfig(
                ServerConfig.embedded()
                    .publicAddress('http://localhost'.toURI())
                    .port(0)
                    .baseDir(localCachePath)
            ).handlers { chain ->
                chain.all(RequestLogger.ncsa())
                chain.get("${this.group}/:module/:revision/ivy.xml") { ctx ->
                    String name = ctx.allPathTokens['module']
                    String revision = getGemQueryRevisionFromIvy(name, ctx.allPathTokens['revision'])
                    Path ivyXml = ivyFile(group, name, revision)
                    debug "Requested ${group}:${name}:${ctx.allPathTokens['revision']} translated to GEM with version ${revision}"
                    if (Files.notExists(ivyXml) || refreshDependencies) {
                        try {
                            createIvyXml(ivyXml, name, revision)
                            ctx.response.contentType('text/xml').sendFile(ivyXml)
                        } catch (ApiException e) {
                            debug(e.message, e)
                            ctx.clientError(404)
                        }
                    } else {
                        ctx.response.contentType('text/xml').sendFile(ivyXml)
                    }

                    debug "Cached file is ${ivyXml.toAbsolutePath()}"
                    debug "Cached file contains ${ivyXml.text}"
                }.get(':group/:module') { ctx ->
                    String grp = ctx.allPathTokens['group']
                    String name = ctx.allPathTokens['module']
                    debug "Request to find all versions for ${grp}:${name}"
                    List<String> versions = api.allVersions(name)
                    debug "Got versions ${versions.join(', ')}"
                    ctx.response.contentType('text/html').send(revisionsAsHtmlDirectoryListing(versions))
                }.get { ctx ->
                    ctx.clientError(403)
                }
            }
        }
        debug "Ivy.xml proxy server starting on ${bindAddress}"
        server
    }

    /** Returns the cache location for a specific GEM.
     *
     * @param group Group associated with GEM.
     * @param name GEM name.
     * @param revision GEM revision.
     * @return Location of {@code ivy.xml} file.
     */
    @SuppressWarnings('UnusedMethodParameter')
    Path ivyFile(String group, String name, String revision) {
        new File(localCachePath, "${name}/${revision}/ivy.xml").toPath()
    }

    @Synchronized
    @SuppressWarnings('BuilderMethodWithSideEffects')
    private void createIvyXml(Path ivyXml, String name, String revision) {
        ExclusiveFileAccess efa = new ExclusiveFileAccess(120000, 20)
        efa.access(ivyXml.toFile()) {
            GemInfo gemInfo = api.metadata(name, revision)
            ivyXml.parent.toFile().mkdirs()
            Path tmp = ivyXml.resolveSibling("${ivyXml.toFile().name}.tmp")
            tmp.withWriter { writer ->
                gemToIvy.writeTo(writer, gemInfo)
            }
            move(tmp, ivyXml, ATOMIC_MOVE, REPLACE_EXISTING)
        }
    }

    private String getGemQueryRevisionFromIvy(String gemName, String revisionPattern) {
        GemVersion version = gemVersionFromGradleRequirement(revisionPattern)
        version.openHigh ? api.latestVersion(gemName) : version.high
    }

    private void debug(String text) {
        log.debug(text)
    }

    private void debug(String text, Object context) {
        log.debug(text, context)
    }

    private volatile int refreshDependencies = 0
    private RatpackServer server
    private final File localCachePath
    private final GemToIvy gemToIvy
    private final RubyGemQueryRestApi api
    private final String group
}
