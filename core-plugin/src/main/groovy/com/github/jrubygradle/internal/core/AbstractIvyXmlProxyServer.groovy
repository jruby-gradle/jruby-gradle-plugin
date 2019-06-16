package com.github.jrubygradle.internal.core

import com.github.jrubygradle.api.core.ApiException
import com.github.jrubygradle.api.core.IvyXmlProxyServer
import com.github.jrubygradle.api.core.RubyGemQueryRestApi
import com.github.jrubygradle.api.gems.GemInfo
import com.github.jrubygradle.api.gems.GemVersion
import com.github.jrubygradle.internal.gems.GemToIvy
import groovy.transform.CompileStatic
import groovy.transform.InheritConstructors
import groovy.transform.Synchronized
import groovy.util.logging.Slf4j
import org.ysb33r.grolifant.api.ExclusiveFileAccess

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

import static com.github.jrubygradle.api.gems.GemVersion.gemVersionFromGradleIvyRequirement
import static com.github.jrubygradle.internal.core.IvyUtils.revisionsAsHtmlDirectoryListing
import static java.nio.file.Files.move
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING

/** Base class for implementing a proxy IvyXML server.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
@Slf4j
abstract class AbstractIvyXmlProxyServer implements IvyXmlProxyServer {

    @InheritConstructors
    static class NotFound extends Exception {
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
        "http://localhost:${bindPort}".toURI()
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

    /** Implementation of a proxy server.
     *
     * @param cache Root directory for local Ivy XML cache.
     * @param serverUri URI of remote Rubygems proxy.
     * @param group Group that will be associated with the Rubygems proxy.
     */
    protected AbstractIvyXmlProxyServer(File cache, URI serverUri, String group) {
        localCachePath = cache
        gemToIvy = new GemToIvy(serverUri)
        api = new DefaultRubyGemRestApi(serverUri)
        this.group = group
    }

    @Synchronized
    @SuppressWarnings('BuilderMethodWithSideEffects')
    protected void createIvyXml(Path ivyXml, String name, String revision) {
        ExclusiveFileAccess efa = new ExclusiveFileAccess(120000, 20)
        efa.access(ivyXml.toFile()) {
            GemInfo gemInfo = api.metadata(name, revision)
            ivyXml.parent.toFile().mkdirs()
            Path tmp = ivyXml.resolveSibling("${ivyXml.toFile().name}.tmp")
            tmp.withWriter { writer ->
                gemToIvy.writeTo(writer, gemInfo)
            }
            move(tmp, ivyXml, ATOMIC_MOVE, REPLACE_EXISTING)
            gemToIvy.writeSha1(ivyXml.toFile())
        }
    }

    protected File getLocalCachePath() {
        this.localCachePath
    }

    protected String getGroup() {
        this.group
    }

    private boolean inGroups(String grp) {
        grp == this.group
    }

    protected boolean expired(Path ivyXml) {
        System.currentTimeMillis()
        Path ivyXmlSha1 = ivyXml.resolveSibling("${ivyXml.toFile().name}.sha1")
        Files.notExists(ivyXml) || Files.notExists(ivyXmlSha1) ||
            (Files.getLastModifiedTime(ivyXml).toMillis() + EXPIRY_PERIOD_MILLIS < Instant.now().toEpochMilli())
    }

    protected Path getIvyXml(String grp, String name, String version) throws NotFound {
        if (inGroups(grp)) {
            String revision = getGemQueryRevisionFromIvy(name, version)
            Path ivyXml = ivyFile(grp, name, revision)
            debug "Requested ${group}:${name}:${version} translated to GEM with version ${revision}"
            if (refreshDependencies || expired(ivyXml)) {
                try {
                    createIvyXml(ivyXml, name, revision)
                } catch (ApiException e) {
                    debug(e.message, e)
                    throw new NotFound()
                }
            }
            debug "Cached file is ${ivyXml.toAbsolutePath()}"
            debug "Cached file contains ${ivyXml.text}"
            ivyXml
        } else {
            throw new NotFound()
        }
    }

    protected Path getIvyXmlSha1(String grp, String name, String version) throws NotFound {
        if (inGroups(grp)) {
            Path ivyXml = getIvyXml(grp, name, version)
            ivyXml.resolveSibling("${ivyXml.toFile().name}.sha1")
        } else {
            throw new NotFound()
        }
    }

    protected String getDirectoryListing(String grp, String name) throws NotFound {
        if (inGroups(grp)) {
            debug "Request to find all versions for ${grp}:${name}"
            List<String> versions = api.allVersions(name)
            debug "Got versions ${versions.join(', ')}"
            revisionsAsHtmlDirectoryListing(versions)
        } else {
            throw new NotFound()
        }
    }
    /** Get port the proxy server has bound to.
     *
     * @return Bind port
     */
    abstract protected int getBindPort()

    private String getGemQueryRevisionFromIvy(String gemName, String revisionPattern) {
        GemVersion version = gemVersionFromGradleIvyRequirement(revisionPattern)
        version.highOpenEnded ? api.latestVersion(gemName) : version.high
    }

    private void debug(String text) {
        log.debug(text)
    }

    private void debug(String text, Object context) {
        log.debug(text, context)
    }

    private static final long EXPIRY_PERIOD_MILLIS =
        System.getProperty('com.github.jrubygradle.cache-expiry-days', '15').toInteger() * 24 * 3600 * 1000
    private volatile int refreshDependencies = 0
    private final File localCachePath
    private final GemToIvy gemToIvy
    private final RubyGemQueryRestApi api
    private final String group
}
