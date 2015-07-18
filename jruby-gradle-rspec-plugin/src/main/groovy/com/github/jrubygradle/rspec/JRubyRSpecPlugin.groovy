package com.github.jrubygradle.rspec

import com.github.jrubygradle.JRubyPlugin
import groovy.transform.PackageScope
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * @author Christian Meier
 */
class JRubyRSpecPlugin implements Plugin<Project> {

    void apply(Project project) {

        project.apply plugin : 'com.github.jruby-gradle.base'
        project.apply plugin : 'java-base'
        project.configurations.maybeCreate('rspec')
        project.tasks.create( 'rspec', RSpec)

        addAfterEvaluateHooks(project)
    }

    @PackageScope
    void addAfterEvaluateHooks(Project project) {
        project.afterEvaluate {
            project.tasks.withType(RSpec) { Task task ->
                project.configurations.maybeCreate(task.name)
                project.dependencies.add(task.name, "org.jruby:jruby-complete:${task.jrubyVersion}")
                project.dependencies.add(task.name, "rubygems:rspec:${task.version}")
            }
        }
    }
}
