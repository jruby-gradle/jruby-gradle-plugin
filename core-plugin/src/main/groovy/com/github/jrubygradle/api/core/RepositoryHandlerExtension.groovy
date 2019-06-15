/*
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
package com.github.jrubygradle.api.core

import com.github.jrubygradle.api.gems.GemGroups
import com.github.jrubygradle.internal.core.IvyXmlGlobalProxyRegistry
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.util.GradleVersion

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
        this.project = project
        this.ivyProxies = new IvyXmlGlobalProxyRegistry(project)
    }

    /** Create an artifact repository which will use {@link https://rubygems.org} and
     * associate group {@code rubygems} with it.
     *
     * @return Artifact repository.
     */
    ArtifactRepository gems() {
        bindRepositoryToProxyServer('https://rubygems.org'.toURI(), DEFAULT_GROUP_NAME)
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
        bindRepositoryToProxyServer(project.uri(uri), DEFAULT_GROUP_NAME)
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
        bindRepositoryToProxyServer(project.uri(uri), group)
    }

    /** Adds the legacy Torquebox Maven proxy to {@code rubygems.org}.
     *
     * Please note that this proxy is effectively unmaintained an no longer supported
     * by the original creators.
     *
     * @return Maven artifact repository
     */
    MavenArtifactRepository torquebox() {
        Action mvnConfigurator = new Action<MavenArtifactRepository>() {
            void execute(MavenArtifactRepository mvn) {
                mvn.url = 'http://rubygems-proxy.torquebox.org/releases'.toURI()
            }
        }
        (MavenArtifactRepository)restrictToGems(
            this.project.repositories.maven(mvnConfigurator),
            DEFAULT_GROUP_NAME
        )
    }

    private ArtifactRepository bindRepositoryToProxyServer(
        URI serverUri,
        String group
    ) {
        IvyXmlProxyServer proxy = ivyProxies.registerProxy(serverUri, group)
        project.extensions.getByType(GemGroups).addGemGroup(group)
        restrictToGems(createIvyRepo(serverUri, proxy.bindAddress), group)
    }

    @CompileDynamic
    private IvyArtifactRepository createIvyRepo(URI server, URI bindAddress) {
        this.project.repositories.ivy {
            artifactPattern "${server}/downloads/[artifact]-[revision](-[classifier]).gem"
            ivyPattern "${bindAddress}/[organisation]/[module]/[revision]/ivy.xml"
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

    private final Project project
    private final IvyXmlGlobalProxyRegistry ivyProxies
    private static final boolean HAS_CONTENT_FEATURE = GradleVersion.current() >= GradleVersion.version('5.1')
}
