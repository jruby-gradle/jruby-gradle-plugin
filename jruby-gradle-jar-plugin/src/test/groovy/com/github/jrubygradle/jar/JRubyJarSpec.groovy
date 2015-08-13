package com.github.jrubygradle.jar

import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * JRubyJar tas unit tests
 */
class JRubyJarSpec extends Specification {
    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
        project.gradle.startParameter.offline = true
        project.with {
            logging.level = LIFECYCLE
            apply plugin: 'com.github.jruby-gradle.base'
            jruby.defaultRepositories = false
        }
    }

    def "jrubyVersion should be configurable in a Gradle conventional way"() {
        given:
        final String version = '1.7.20'

        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            jrubyVersion version
        }

        expect:
        task.jrubyVersion == version
    }

    def "configuration should default to 'jrubyJar'"() {
        given:
        JRubyJar task = project.task('spock-jar', type: JRubyJar)

        expect:
        task.configuration == 'jrubyJar'
    }

    def "configuration should be configurable in a Gradle conventional way"() {
        given:
        final String customConfig = 'spockConfig'

        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            configuration customConfig
        }

        expect:
        task.configuration == customConfig
    }

    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/168")
    def "configuring a new version of JRuby requires a non-default configuration"() {
        given:
        final String version = '1.7.11'
        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            jrubyVersion version
        }

        when:
        project.evaluate()

        then: "a configuration error should be thrown"
        thrown(ProjectConfigurationException)
    }

    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/168")
    def "configuring a new version of jruby-mains requires a non-default configuration"() {
        final String version = '0.3.0'
        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            jrubyMainsVersion version
        }

        when:
        project.evaluate()

        then: "a configuration error should be thrown"
        thrown(ProjectConfigurationException)
    }
}
