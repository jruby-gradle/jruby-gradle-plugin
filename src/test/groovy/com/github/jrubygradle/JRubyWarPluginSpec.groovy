package com.github.jrubygradle

import org.gradle.api.tasks.Delete
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test
import spock.lang.*

import static org.gradle.api.logging.LogLevel.LIFECYCLE
import static org.junit.Assert.assertTrue

/**
 * @author R. Tyler Croy
 *
 */
class JRubyWarPluginSpec extends Specification {

    def project
    def warTask

    void setup() {
        project = ProjectBuilder.builder().build()
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.war'

    }

    def "basic sanity check"() {
        expect:
            project.tasks.jrubyWar.group == 'JRuby'
            project.tasks.jrubyCacheJars instanceof AbstractCopyTask
            project.tasks.jrubyPrepare instanceof Task
            project.tasks.jrubyWar instanceof War
            project.tasks.jrubyJar instanceof Jar
    }
}
