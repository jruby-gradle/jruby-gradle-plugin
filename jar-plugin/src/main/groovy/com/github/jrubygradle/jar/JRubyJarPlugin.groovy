package com.github.jrubygradle.jar

import static com.github.jrubygradle.jar.JRubyJar.DEFAULT_JRUBYJAR_CONFIG

import groovy.transform.PackageScope
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test

/**
 * @author Schalk W. Cronjé
 * @author Christian Meier
 */
class JRubyJarPlugin implements Plugin<Project> {
    private static final String TEST_TASK_NAME = 'test'

    void apply(Project project) {
        project.apply plugin: 'com.github.jruby-gradle.base'
        project.apply plugin: 'java-base'
        project.configurations.maybeCreate(DEFAULT_JRUBYJAR_CONFIG)
        project.tasks.create('jrubyJar', JRubyJar)
    }
}
