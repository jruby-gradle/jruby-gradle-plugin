package com.github.jrubygradle

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Issue
import spock.lang.Specification

import static com.github.jrubygradle.JRubyExec.jarDependenciesGemLibPath
import static com.github.jrubygradle.internal.JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG

// ===============================================
// *** DO NOT RUN ANY SCRIPTS IN THIS UNITTEST ***
// Use JRubyExecIntegrationSpec instead
// ===============================================

/**
 * @author Schalk W. Cronjé
 *
 */
class JRubyExecSpec extends Specification {
    static final String TASK_NAME = 'RubyWax'
    static final String SCRIPT_NAME = 'helloWorld.rb'

    Project project
    JRubyExec execTask
    String testJarDependencies

    void setup() {
        project = ProjectBuilder.builder().build()
        testJarDependencies = jarDependenciesGemLibPath(new File(project.buildDir, 'tmp/jrubyExec'))
        project.apply plugin: 'com.github.jruby-gradle.base'
        execTask = project.tasks.create(TASK_NAME, JRubyExec)

        project.file(SCRIPT_NAME).text = this.class.getResource("/${SCRIPT_NAME}")
    }

    void "Do not allow JRubyExec to be instantiated if plugin has not been loaded"() {
        given: "A basic project"
        def badProject = ProjectBuilder.builder().build()

        when: "A JRubyExec task is instantiated with the jruby plugin being applied"
        badProject.task('bad', type: JRubyExec)

        then: "An exception should be thrown"
        thrown(org.gradle.api.internal.tasks.DefaultTaskContainer.TaskCreationException)
    }

    void "Do not allow args to be set directly"() {
        when: "Calling args"
        execTask.args('a param', 'b param')

        then: "An exception should be thrown instead of JavaExec.args being set"
        thrown(UnsupportedOperationException)
    }

    void "Check jruby defaults"() {
        expect: "Default jruby version should be same as project.ruby.execVersion"
        execTask.jrubyVersion == project.jruby.execVersion

        and: "Default configuration should be jrubyExec"
        execTask.configuration == DEFAULT_JRUBYEXEC_CONFIG
    }

    void "Check jruby defaults when jruby.execVersion is changed after the task is created"() {
        when: "ExecVersion is changed later on, and JRubyExec.jrubyVersion was not called"
        project.jruby.execVersion = '1.5.0'

        then: "jruby defaults version should point to the earlier version"
        execTask.jrubyVersion == '1.5.0'
    }

    void "Changing the JRuby version with the default configuration"() {
        given:
        final String newVersion = '9.0.1.0'
        execTask.jrubyVersion = newVersion

        when:
        project.evaluate()

        then:
        project.jruby.execVersion != newVersion
        thrown(ProjectConfigurationException)
    }

    void "Changing the jruby version on a JRubyExec task"() {
        given:
        final String configurationName = 'spock-ruby'
        final String newVersion = '9.0.1.0'

        when:
        project.configure(execTask) {
            configuration configurationName
            jrubyVersion newVersion
        }
        project.evaluate()

        then:
        execTask.jrubyVersion != project.jruby.execVersion

        then: "jrubyVersion must be updated"
        execTask.jrubyVersion == newVersion

        and: "jrubyConfigurationName must point to this new configuration"
        execTask.configuration == configurationName

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
        }

        then:
        execTask.args == ['-I', testJarDependencies, '-rjars/setup',
                          '-j1', '-j2', '-j3', '-S',
                          SCRIPT_NAME,
                          '-s1', '-s2', '-s3']
    }

    void "Properly handle the lack of a `script` argument"() {
        when:
        project.configure(execTask) {
            jrubyArgs '-S', 'rspec'
        }

        then:
        execTask.args == ['-I', testJarDependencies, '-rjars/setup', '-S', 'rspec']
    }

    void "Error when `script` is empty and there is no `jrubyArgs`"() {
        when:
        execTask.args

        then: "An exception should be thrown"
        thrown(InvalidUserDataException)
    }

    void "Properly set the PATH in the Exec envirionment"() {
        given:
        project.configurations.maybeCreate('foo')

        when:
        project.configure(execTask) {
            configuration 'foo'
        }

        then:
        execTask.getComputedPATH(System.env.PATH).contains('foo')
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/170')
    void "setting configuration with Configuration should work as expected"() {
        given: 'a Configuration'
        final Configuration customConfig = project.configurations.create('spockConfig')

        when: 'the task is configured'
        project.configure(execTask) {
            configuration customConfig
        }

        then:
        execTask.configuration == customConfig.name
    }
}
