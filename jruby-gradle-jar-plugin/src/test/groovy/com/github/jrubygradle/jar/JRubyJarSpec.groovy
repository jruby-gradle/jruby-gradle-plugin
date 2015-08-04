package com.github.jrubygradle.jar

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*

/**
 */
class JRubyJarSpec extends Specification {
    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.jar'
        project.jruby.defaultRepositories = false
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

        when:
        project.evaluate()

        then:
        task.configuration == 'jrubyJar'
    }

    def "configuration should be configurable in a Gradle conventional way"() {
        given:
        final String customConfig = 'spockConfig'

        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            configuration customConfig
        }

        when:
        project.evaluate()

        then:
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
        final String version = '0.1.0'
        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            jrubyMainsVersion version
        }

        when:
        project.evaluate()

        then: "a configuration error should be thrown"
        thrown(ProjectConfigurationException)
    }

    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/168")
    def "configuring a new jrubyMainsVersion should update the dependency graph properly"() {
        Configuration config = project.configurations.create('spockConfig')
        final String version = '0.1.0'
        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            configuration config.name
            jrubyMainsVersion version
        }

        when:
        project.evaluate()

        then: "the custom version should be included in the custom config"
        config.dependencies.find { it.name == 'jruby-mains' && it.version == version }
        config.dependencies.findAll({ it.name == 'jruby-mains' }).size() == 1
    }


    def "Setting the jrubyVersion to an older version of JRuby should update jar-dependencies"() {
        given: "a version of JRuby which doesn't bundle jar-dependencies"
        Configuration config = project.configurations.create('spockConfig')
        final String version = '1.7.11'
        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            configuration config.name
            jrubyVersion version
        }

        when:
        project.evaluate()

        then: "the JRuby version and jar-dependencies versions should be set"
        config.dependencies.find { it.name == 'jruby-complete' && it.version == version }
        config.dependencies.find { it.name == 'jar-dependencies' }

    }
}
