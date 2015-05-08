package com.github.jrubygradle.testhelper

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author Schalk W. Cronjé.
 */
class BasicProjectBuilder {

    static Project buildWithStdRepo( final File projectDir_, final File cacheDir_ ) {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir_).build()
        project.with {
            logging.level = LIFECYCLE
            apply plugin: 'com.github.jruby-gradle.base'
            jruby.defaultRepositories = true

        }
        project
    }

    static Project buildWithLocalRepo( final File projectDir_, final File repoDir_, final File cacheDir_ ) {
        Project project = ProjectBuilder.builder().withProjectDir(projectDir_).build()
        project.with {
            logging.level = LIFECYCLE
            apply plugin: 'com.github.jruby-gradle.base'

            jruby.defaultRepositories = false

            repositories {
                flatDir dirs : repoDir_.absolutePath
            }

        }
        project
    }
}
