/*
 * Copyright (c) 2014-2023, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.jar

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Issue
import spock.lang.Specification

/**
 * JRubyJar task's unit tests
 */
class JRubyJarSpec extends Specification {
    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
        project.gradle.startParameter.offline = true
        project.with {
            apply plugin: 'com.github.jruby-gradle.base'
            jruby.defaultRepositories = false
        }
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/357')
    void 'group should be set to "JRuby"'() {
        given:
        JRubyJar task = project.task('spock-jar', type: JRubyJar)
        def prepareTask = project.tasks.find { it.name == 'prepareSpock-jar' }

        expect:
        task.group == 'JRuby'
        prepareTask.group == task.group
    }

    void "jrubyVersion should be configurable in a Gradle conventional way"() {
        given:
        final String version = '1.7.20'

        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            jrubyVersion version
        }

        expect:
        task.jrubyVersion == version
    }

    void "jrubyMainsVersion should be configurable in a Gradle conventional way"() {
        given:
        final String version = '0.4.1.'

        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            jrubyMainsVersion version
        }

        expect:
        task.jrubyMainsVersion == version
    }

    void "configuration should default to 'jrubyJar'"() {
        given:
        JRubyJar task = project.task('spock-jar', type: JRubyJar)

        expect:
        task.configuration == 'jrubyJar'
    }

    void "configuration should be configurable in a Gradle conventional way"() {
        given:
        final String customConfig = 'spockConfig'

        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            configuration customConfig
        }

        expect:
        task.configuration == customConfig
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/202')
    void "defaults 'gems' should log an error"() {
        given:
        boolean evaluated = false
        JRubyJar task = project.task('spock-jar', type: JRubyJar)

        when:
        task.configure {
            evaluated = true
            defaults 'gems'
        }

        then:
        evaluated
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/169')
    void "configuration should be configurable with a Configuration object"() {
        given: 'a configuration'
        project.with {
            configurations {
                spockConfig
            }
        }
        final Configuration customConfig = project.configurations.findByName('spockConfig')

        when: 'the configuration is set'
        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            configuration customConfig
        }

        then:
        task.configuration == customConfig.name
    }

    void "configure library()"() {
        given: 'a task with initScript library()'
        boolean evaluated = false
        JRubyJar task = project.task('library-jar', type: JRubyJar)

        when:
        task.configure {
            evaluated = true
            initScript library()
        }

        then:
        evaluated
    }
}
