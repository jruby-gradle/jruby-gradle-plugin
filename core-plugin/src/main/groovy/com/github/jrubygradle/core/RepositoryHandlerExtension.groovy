package com.github.jrubygradle.core

import com.github.jrubygradle.core.internal.IvyXmlGlobalProxyRegistry
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.util.GradleVersion

/** Extension which can be added to {@code project.repositories}.
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
        this.ivyProxies = new IvyXmlGlobalProxyRegistry((project))
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

    private ArtifactRepository bindRepositoryToProxyServer(
        URI serverUri,
        String group
    ) {
        IvyXmlProxyServer proxy = ivyProxies.registerProxy(serverUri, group)
        restrictToGems(createIvyRepo(serverUri, proxy.bindAddress), group)
    }

    @CompileDynamic
    private IvyArtifactRepository createIvyRepo(URI server, URI bindAddress) {
        this.project.repositories.ivy {
            artifactPattern "${server}/downloads/[artifact]-[revision].gem"
            ivyPattern "${bindAddress}/[organisation]/[module]/[revision]/ivy.xml"
        }
    }

    @CompileDynamic
    private IvyArtifactRepository restrictToGems(IvyArtifactRepository repo, String group) {
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
