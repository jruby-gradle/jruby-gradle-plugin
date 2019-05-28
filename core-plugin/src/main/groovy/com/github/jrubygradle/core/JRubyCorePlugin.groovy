package com.github.jrubygradle.core

import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
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
        ((ExtensionAware) project.repositories).extensions.create(
            RepositoryHandlerExtension.NAME,
            RepositoryHandlerExtension,
            project
        )
    }
}
