package com.github.jrubygradle.jar

import org.gradle.api.Plugin
import org.gradle.api.Project

import static com.github.jrubygradle.jar.JRubyJar.DEFAULT_JRUBYJAR_CONFIG

/**
 * @author Schalk W. Cronjé
 * @author Christian Meier
 */
class JRubyJarPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'com.github.jruby-gradle.base'
        project.apply plugin: 'java-base'
        project.configurations.maybeCreate(DEFAULT_JRUBYJAR_CONFIG)
        project.tasks.create('jrubyJar', JRubyJar)
    }
}
