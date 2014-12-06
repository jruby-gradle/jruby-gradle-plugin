package com.github.jrubygradle.war

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.War
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete

import com.github.jrubygradle.internal.WarblerBootstrap
import com.github.jrubygradle.JRubyPlugin

/**
 * Created by schalkc on 27/08/2014.
 */
class JRubyWarPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'war'
        project.apply plugin: 'com.github.jruby-gradle.base'
        project.configurations.create(JRubyWar.JRUBYWAR_CONFIG)
        project.configurations.maybeCreate('jrubyEmbeds')

        project.afterEvaluate {
            JRubyWar.updateJRubyDependencies(project)

            project.dependencies {
                jrubyEmbeds group: 'com.github.jruby-gradle', name: 'warbler-bootstrap', version: '0.2.0+'
            }
        }

        // Only jRubyWar will depend on jrubyPrepare. Other JRubyWar tasks created by
        // build script authors will be under their own control
        // jrubyWar task will use jrubyWar as configuration
        project.task('jrubyWar', type: JRubyWar) {
            group JRubyPlugin.TASK_GROUP_NAME
            description 'Create a JRuby-based web archive'
            dependsOn project.tasks.jrubyPrepare
            classpath project.configurations.jrubyWar
        }
    }
}
