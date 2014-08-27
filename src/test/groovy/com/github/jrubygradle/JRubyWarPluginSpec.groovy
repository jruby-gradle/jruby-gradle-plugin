package com.github.jrubygradle.war

import org.gradle.api.Task
import org.gradle.api.tasks.bundling.War
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
        project.apply plugin: 'com.github.jruby-gradle.war'

    }

    def "basic sanity check"() {
        expect:
            project.tasks.jrubyWar.group == 'JRuby'
            project.tasks.jrubyWar instanceof War
    }
}
