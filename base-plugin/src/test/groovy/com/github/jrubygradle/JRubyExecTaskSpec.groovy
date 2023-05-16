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
package com.github.jrubygradle

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Issue
import spock.lang.Specification

// ===============================================
// *** DO NOT RUN ANY SCRIPTS IN THIS UNITTEST ***
// Use JRubyExecIntegrationSpec instead
// ===============================================

/**
 * @author Schalk W. Cronjé
 *
 */
class JRubyExecTaskSpec extends Specification {
    static final String TASK_NAME = 'RubyWax'
    static final String SCRIPT_NAME = 'helloWorld.rb'

    Project project
    JRubyExec execTask

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.base'
        execTask = project.tasks.create(TASK_NAME, JRubyExec)

        project.file(SCRIPT_NAME).text = this.class.getResource("/${SCRIPT_NAME}")
    }

    void "Do not allow args to be set directly"() {
        when: "Calling args"
        execTask.args('a param', 'b param')

        then: "An exception should be thrown instead of JavaExec.args being set"
        thrown(UnsupportedOperationException)
    }

    void "Changing the jruby version on a JRubyExec task"() {
        given:
        final String configurationName = 'spock-ruby'
        final String newVersion = '9.0.1.0'
        project.configurations.create(configurationName)

        when:
        project.configure(execTask) {
            jruby.gemConfiguration configurationName
            jruby.jrubyVersion newVersion
        }
        project.evaluate()

        then:
        execTask.jruby.jrubyVersion != project.jruby.jrubyVersion

        and: "jrubyConfigurationName must point to this new configuration"
        execTask.jruby.gemConfiguration.name == configurationName

        and: "configuration must exist"
        project.configurations.findByName(configurationName)
    }

    void "Checking the jruby main class"() {
        expect:
        execTask.main == JRubyExec.MAIN_CLASS
    }

    void "Setting the script name without path compoenents yields just the relative file"() {
        when: 'Setting path to a string'
        execTask.script = SCRIPT_NAME

        then: 'script will be File object with the correct path'
        execTask.script == new File(SCRIPT_NAME)
    }

    void "Setting jruby arguments"() {
        when: "calling scriptArgs multiple times, with different kinds of arguments"
        project.configure(execTask) {
            jrubyArgs 'a', 'b', 'c'
            jrubyArgs 'd', 'e', 'f'
        }

        then: "append everything"
        execTask.jrubyArgs == ['a', 'b', 'c', 'd', 'e', 'f']
    }

    void "Setting script arguments"() {
        when: "calling scriptAtgs multiple times, with different kinds of arguments"
        project.configure(execTask) {
            scriptArgs 'a', 'b', 'c'
            scriptArgs 'd', 'e', 'f'
        }

        then: "append everything"
        execTask.scriptArgs == ['a', 'b', 'c', 'd', 'e', 'f']
    }

    void "Setting script arguments with Closures"() {
        when: "calling scriptArgs with a Closure in the array"
        project.configure(execTask) {
            scriptArgs 'a', { 'b' }, 'c'
        }

        then: "evaluate the closure when retrieving scriptArgs"
        execTask.scriptArgs == ['a', 'b', 'c']
    }

    void "Getting correct command-line passed"() {
        when:

        project.configure(execTask) {
            scriptArgs '-s1', '-s2', '-s3'
            jrubyArgs '-j1', '-j2', '-j3', '-S'
            script SCRIPT_NAME
            jruby.gemConfiguration 'gems'
        }

        then:
        execTask.args == ['-rjars/setup',
                          '-j1', '-j2', '-j3', '-S',
                          SCRIPT_NAME,
                          '-s1', '-s2', '-s3']
    }

    void "Properly handle the lack of a `script` argument"() {
        when:
        project.jruby.gemConfiguration 'gems'
        project.configure(execTask) {
            jrubyArgs '-S', 'rspec'
        }

        then:
        execTask.args == ['-rjars/setup', '-S', 'rspec']
    }

    void "Error when `script` is empty and there is no `jrubyArgs`"() {
        when:
        execTask.args

        then: "An exception should be thrown"
        thrown(InvalidUserDataException)
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/170')
    void "setting configuration with Configuration should work as expected"() {
        given: 'a Configuration'
        final Configuration customConfig = project.configurations.create('spockConfig')

        when: 'the task is configured'
        project.configure(execTask) {
            jruby.gemConfiguration customConfig
        }

        then:
        execTask.jruby.gemConfiguration.name == customConfig.name
    }
}
