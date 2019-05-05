package com.github.jrubygradle.war

import org.gradle.api.tasks.bundling.War
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * @author R. Tyler Croy
 *
 */
class JRubyWarPluginSpec extends Specification {

    def project

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.war'

    }

    def "Basic sanity check"() {
        expect:
        project.tasks.jrubyWar.group == 'JRuby'
        project.tasks.jrubyWar instanceof War
    }
}
