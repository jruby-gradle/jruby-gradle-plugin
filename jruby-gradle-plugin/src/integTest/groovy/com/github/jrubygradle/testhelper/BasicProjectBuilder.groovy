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
        File repo = project.file("../../../../../../src/integTest/mavenrepo")
        if (!repo.exists()){
          throw new RuntimeException("no repo at " + repo)
        }
        project.with {
            logging.level = LIFECYCLE
            apply plugin: 'com.github.jruby-gradle.base'
            jruby.defaultRepositories = true
            repositories {
                maven {
                    url "file://" + repo.absolutePath
                }
            }
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
