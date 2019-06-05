package com.github.jrubygradle.api.core

import com.github.jrubygradle.api.gems.GemGroups
import com.github.jrubygradle.internal.gems.GemVersionResolver
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware

/** Provides only a repository handler extensiosn for looking up rubygem
 * metadata.
 *
 * @since 2.0
 */
@CompileStatic
class JRubyCorePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        GemGroups gemGroups = project.extensions.create(GemGroups.NAME, GemGroups)

        ((ExtensionAware) project.repositories).extensions.create(
            RepositoryHandlerExtension.NAME,
            RepositoryHandlerExtension,
            project
        )

        project.configurations.all { Configuration cfg ->
            GemVersionResolver.addGemResolver(cfg, gemGroups, new GemVersionResolver(gemGroups, project.logger, cfg))
        }
    }
}
