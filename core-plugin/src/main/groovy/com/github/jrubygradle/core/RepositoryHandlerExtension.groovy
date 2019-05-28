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

    RepositoryHandlerExtension(final Project project) {
        this.project = project
        this.ivyProxies = new IvyXmlGlobalProxyRegistry((project))
    }

    ArtifactRepository gems() {
        bindRepositoryToProxyServer('https://rubygems.org'.toURI(), DEFAULT_GROUP_NAME)
    }

    ArtifactRepository gems(Object uri) {
        bindRepositoryToProxyServer(project.uri(uri), DEFAULT_GROUP_NAME)
    }

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
