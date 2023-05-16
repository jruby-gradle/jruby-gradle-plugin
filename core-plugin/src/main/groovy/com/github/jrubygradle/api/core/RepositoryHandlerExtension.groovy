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
package com.github.jrubygradle.api.core

import com.github.jrubygradle.api.gems.GemResolverStrategy
import com.github.jrubygradle.internal.core.IvyXmlGlobalProxyRegistry
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.util.GradleVersion
import org.ysb33r.grolifant.api.v4.ClosureUtils

import static org.ysb33r.grolifant.api.v4.UriUtils.urize

/** Extension which can be added to {@code project.repositories}.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class RepositoryHandlerExtension {
    public static final String NAME = 'ruby'
    public static final String DEFAULT_GROUP_NAME = 'rubygems'

    /** Creates an extension and associate it with a project.
     *
     * @param project Gradle project.
     */
    RepositoryHandlerExtension(final Project project) {
        this.extensions = project.extensions
        this.repositories = project.repositories
        this.ivyProxies = new IvyXmlGlobalProxyRegistry(project)
    }

    /** Create an artifact repository which will use https://rubygems.org and
     * associate group {@code rubygems} with it.
     *
     * @return Artifact repository.
     */
    ArtifactRepository gems() {
        bindRepositoryToProxyServer(
            RUBYGEMS_URI,
            DEFAULT_GROUP_NAME,
            new GemRepositoryConfiguration()
        )
    }

    /** Create an artifact repository which will use https://rubygems.org and
     * associate group {@code rubygems} with it.
     *
     * @param cfg GEM repository configuration
     * @return Artifact repository.
     */
    ArtifactRepository gems(@DelegatesTo(GemRepositoryConfiguration) Closure cfg) {
        bindRepositoryToProxyServer(RUBYGEMS_URI, DEFAULT_GROUP_NAME, cfg)
    }

    /** Create an artifact repository which will use https://rubygems.org and
     * associate group {@code rubygems} with it.
     *
     * @param cfg GEM repository configuration
     * @return Artifact repository.
     */
    ArtifactRepository gems(Action<GemRepositoryConfiguration> cfg) {
        bindRepositoryToProxyServer(RUBYGEMS_URI, DEFAULT_GROUP_NAME, cfg)
    }

    /** Create an artifact repository which will use specified URI and
     * associate group {@code rubygems} with it.
     *
     * @param uri URI of remote repository that serves up Rubygems. Any object convertible
     * with {@code project.uri} can be provided.
     *
     * @return Artifact repository.
     */
    ArtifactRepository gems(Object uri) {
        bindRepositoryToProxyServer(urize(uri), DEFAULT_GROUP_NAME, new GemRepositoryConfiguration())
    }

    /** Create an artifact repository which will use specified URI and
     * associate group {@code rubygems} with it.
     *
     * @param uri URI of remote repository that serves up Rubygems. Any object convertible
     * with {@code project.uri} can be provided.
     * @param cfg GEM repository configuration
     *
     * @return Artifact repository.
     */
    ArtifactRepository gems(Object uri, @DelegatesTo(GemRepositoryConfiguration) Closure cfg) {
        bindRepositoryToProxyServer(urize(uri), DEFAULT_GROUP_NAME, cfg)
    }

    /** Create an artifact repository which will use specified URI and
     * associate group {@code rubygems} with it.
     *
     * @param uri URI of remote repository that serves up Rubygems. Any object convertible
     * with {@code project.uri} can be provided.
     * @param cfg GEM repository configuration
     *
     * @return Artifact repository.
     */
    ArtifactRepository gems(Object uri, Action<GemRepositoryConfiguration> cfg) {
        bindRepositoryToProxyServer(urize(uri), DEFAULT_GROUP_NAME, cfg)
    }

    /** Create an artifact repository which will use specified URI and
     * associate a specified group with it.
     *
     * @param group Group to associate this server with.
     * @param uri URI of remote repository that serves up Rubygems. Any object convertible
     * with {@code project.uri} can be provided.
     * @return Artifact repository.
     */
    ArtifactRepository gems(String group, Object uri) {
        bindRepositoryToProxyServer(urize(uri), group, new GemRepositoryConfiguration())
    }

    /** Create an artifact repository which will use specified URI and
     * associate a specified group with it.
     *
     * @param group Group to associate this server with.
     * @param uri URI of remote repository that serves up Rubygems. Any object convertible
     * with {@code project.uri} can be provided.
     * @param cfg GEM repository configuration
     * @return Artifact repository.
     */
    ArtifactRepository gems(String group, Object uri, @DelegatesTo(GemRepositoryConfiguration) Closure cfg) {
        bindRepositoryToProxyServer(urize(uri), group, cfg)
    }

    /** Create an artifact repository which will use specified URI and
     * associate a specified group with it.
     *
     * @param group Group to associate this server with.
     * @param uri URI of remote repository that serves up Rubygems. Any object convertible
     * with {@code project.uri} can be provided.
     * @param cfg GEM repository configuration
     * @return Artifact repository.
     */
    ArtifactRepository gems(String group, Object uri, Action<GemRepositoryConfiguration> cfg) {
        bindRepositoryToProxyServer(urize(uri), group, cfg)
    }

    /** Adds the Maven-GEMs proxy that is supported by the JRuby group.
     *
     * For supporting Gradle versions, this repository will only be consulted for artifacts that are in the
     * {@code rubygems} group.
     *
     * @return Maven repository
     */
    MavenArtifactRepository mavengems() {
        bindToMavenRepository(MAVENGEMS_URI, DEFAULT_GROUP_NAME)
    }

    /** Adds a remote Maven-GEMs proxy.
     *
     * For supporting Gradle versions, this repository will only be consulted for artifacts that are in the
     * {@code rubygems} group.
     *
     * @param uri Remote Maven-GEMs proxy
     * @return Maven repository
     */
    MavenArtifactRepository mavengems(Object uri) {
        bindToMavenRepository(urize(uri), DEFAULT_GROUP_NAME)
    }

    /**  Adds a remote Maven-GEMs proxy anbd allocate a dedicated group for it.
     *
     * For supporting Gradle versions, this repository will only be consulted for artifacts that are in the
     * specified group.
     *
     * @param group Maven group name
     * @param uri Remote Maven-GEMs proxy
     * @return Maven repository
     */
    MavenArtifactRepository mavengems(String group, Object uri) {
        bindToMavenRepository(urize(uri), group)
    }

    private MavenArtifactRepository bindToMavenRepository(
        URI serverUri,
        String group
    ) {
        MavenArtifactRepository repo = repositories.maven(new Action<MavenArtifactRepository>() {
            @Override
            void execute(MavenArtifactRepository mvn) {
                mvn.url = serverUri
            }
        })
        restrictToGems(repo, group)
        repo
    }

    private ArtifactRepository bindRepositoryToProxyServer(
        URI serverUri,
        String group,
        GemRepositoryConfiguration cfg
    ) {
        IvyXmlProxyServer proxy = ivyProxies.registerProxy(serverUri, group, cfg)
        extensions.getByType(GemResolverStrategy).addGemGroup(group)
        restrictToGems(createIvyRepo(serverUri, proxy.bindAddress), group)
    }

    private ArtifactRepository bindRepositoryToProxyServer(
        URI serverUri,
        String group,
        @DelegatesTo(GemRepositoryConfiguration) Closure cfg
    ) {
        GemRepositoryConfiguration grc = new GemRepositoryConfiguration()
        ClosureUtils.configureItem(grc, cfg)
        bindRepositoryToProxyServer(serverUri, group, grc)
    }

    private ArtifactRepository bindRepositoryToProxyServer(
        URI serverUri,
        String group,
        Action<GemRepositoryConfiguration> cfg
    ) {
        GemRepositoryConfiguration grc = new GemRepositoryConfiguration()
        cfg.execute(grc)
        bindRepositoryToProxyServer(serverUri, group, grc)
    }

    @CompileDynamic
    private IvyArtifactRepository createIvyRepo(URI server, URI bindAddress) {
        repositories.ivy {
            artifactPattern "${server}/downloads/[artifact]-[revision](-[classifier]).gem"
            ivyPattern "${bindAddress}/[organisation]/[module]/[revision]/ivy.xml"

            if (HAS_SECURE_PROTOCOL_FEATURE) {
                allowInsecureProtocol = true
            }
        }
    }

    @CompileDynamic
    private ArtifactRepository restrictToGems(ArtifactRepository repo, String group) {
        if (HAS_CONTENT_FEATURE) {
            repo.content {
                it.includeGroup group
            }
        }
        repo
    }

    private final IvyXmlGlobalProxyRegistry ivyProxies
    private final ExtensionContainer extensions
    private final RepositoryHandler repositories
    private static final boolean HAS_CONTENT_FEATURE = GradleVersion.current() >= GradleVersion.version('5.1')
    private static final boolean HAS_SECURE_PROTOCOL_FEATURE = GradleVersion.current() >= GradleVersion.version('6.0')
    private static final URI RUBYGEMS_URI = 'https://rubygems.org'.toURI()
    private static final URI MAVENGEMS_URI = 'https://mavengems.jruby.org'.toURI()
}
