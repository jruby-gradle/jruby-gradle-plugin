package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import org.gradle.api.InvalidUserDataException
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*
import static org.gradle.api.logging.LogLevel.*

// ===============================================
// *** DO NOT RUN ANY SCRIPTS IN THIS UNITTEST ***
// Use JRubyExecIntegrationSpec instead
// ===============================================

/**
 * @author Schalk W. Cronjé
 *
 */
class JRubyExecSpec extends Specification {

    static final File TEST_SCRIPT_DIR = new File( System.getProperty('TEST_SCRIPT_DIR') ?: 'src/integTest/resources/scripts').absoluteFile
    static final File TESTROOT = new File( System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests', 'jes')
    static final String TEST_JAR_DEPENDENCIES = JRubyExec.jarDependenciesGemLibPath(new File(TESTROOT, 'tmp/jrubyExec'))
    static final String TASK_NAME = 'RubyWax'

    def project
    def execTask

    void setup() {
        project = ProjectBuilder.builder().withProjectDir(TESTROOT).build()
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.base'
        execTask = project.task(TASK_NAME, type: JRubyExec)
    }

    def "Do not allow JRubyExec to be instantiated if plugin has not been loaded"() {
        given: "A basic project"
            def badProject = ProjectBuilder.builder().build()
            badProject.logging.level = LIFECYCLE

        when: "A JRubyExec task is instantiated with the jruby plugin being applied"
            badProject.task( 'bad', type : JRubyExec )

        then: "An exception should be thrown"
            thrown(TaskInstantiationException)
    }

    def "Do not allow args to be set directly"() {

        when: "Calling args"
            execTask.args ( 'a param','b param')

        then: "An exception should be thrown instead of JavaExec.args being set"
            thrown(UnsupportedOperationException)

    }

    def "Check jruby defaults"() {
        expect: "Default jruby version should be same as project.ruby.execVersion"
        execTask.jrubyVersion == project.jruby.execVersion

        and: "Default configuration should be jrubyExec"
        execTask.configuration == JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG
    }

    def "Check jruby defaults when jruby.execVersion is changed after the task is created"() {
        given:
        final String initialVersion = project.jruby.execVersion

        when: "ExecVersion is changed later on, and JRubyExec.jrubyVersion was not called"
        project.jruby.execVersion = '1.5.0'

        then: "jruby defaults version should point to the earlier version"
        execTask.jrubyVersion == '1.5.0'
    }

    def "Changing the JRuby version with the default configuration"() {
        given:
        final String newVersion = '1.7.11'
        execTask.jrubyVersion = newVersion

        when:
        project.evaluate()

        then:
        project.jruby.execVersion != newVersion
        thrown(ProjectConfigurationException)
    }

    def "Changing the jruby version on a JRubyExec task"() {
        given:
        final String configurationName = 'spock-ruby'
        final String newVersion = '1.7.11'

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

    def "Checking the jruby main class"() {
        expect:
            execTask.main == 'org.jruby.Main'
    }

    def "Setting the script name"() {
        when: 'Setting path to a string'
            execTask.script = "${TEST_SCRIPT_DIR}/helloWorld.rb"

        then: 'script will be File object with the correct path'
            execTask.script.absolutePath == new File(TEST_SCRIPT_DIR,'helloWorld.rb').absolutePath
    }

    def "Setting jruby arguments"()  {
        when: "calling scriptArgs multiple times, with different kinds of arguments"
            project.configure(execTask) {
                jrubyArgs 'a', 'b', 'c'
                jrubyArgs 'd', 'e', 'f'
            }

        then: "append everything"
            execTask.jrubyArgs == ['a','b','c','d','e','f']
    }

    def "Setting script arguments"()  {
        when: "calling scriptAtgs multiple times, with different kinds of arguments"
            project.configure(execTask) {
                scriptArgs 'a', 'b', 'c'
                scriptArgs 'd', 'e', 'f'
            }
        then: "append everything"
            execTask.scriptArgs == ['a','b','c','d','e','f']
    }

    def "Setting script arguments with Closures"() {
        when: "caling scriptArgs with a Closure in the array"
            project.configure(execTask) {
                scriptArgs 'a', { 'b' }, 'c'
            }
        then: "evaluate the closure when retrieving scriptArgs"
            execTask.scriptArgs == ['a','b','c']
    }

    def "Getting correct command-line passed"() {
        when:
            project.configure(execTask) {
                scriptArgs '-s1','-s2','-s3'
                jrubyArgs  '-j1','-j2','-j3','-S'
                script     "${TEST_SCRIPT_DIR}/helloWorld.rb"
            }

        then:
        execTask.getArgs() == ['-I', TEST_JAR_DEPENDENCIES, '-rjars/setup', '-j1','-j2','-j3','-S',new File(TEST_SCRIPT_DIR,'helloWorld.rb').absolutePath,'-s1','-s2','-s3']
    }

    def "Properly handle the lack of a `script` argument"() {
        when:
            project.configure(execTask) {
                jrubyArgs '-S', 'rspec'
            }

        then:
            execTask.getArgs() == ['-I', TEST_JAR_DEPENDENCIES, '-rjars/setup', '-S', 'rspec']
    }

    def "Error when `script` is empty and there is no `jrubyArgs`"() {
        when:
            project.configure(execTask) {
            }
            execTask.getArgs()

        then: "An exception should be thrown"
            thrown(InvalidUserDataException)
    }

    def "Properly set the PATH in the Exec envirionment"() {
        given:
            project.configurations.maybeCreate('foo')

        when:
            project.configure(execTask) {
                configuration 'foo'
            }

        then:
            execTask.getComputedPATH(System.env.PATH).contains('foo')
    }
}
