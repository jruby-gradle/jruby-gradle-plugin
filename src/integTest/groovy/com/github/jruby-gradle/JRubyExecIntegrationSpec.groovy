package com.lookout.jruby

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*
import static org.gradle.api.logging.LogLevel.LIFECYCLE


/**
 * Created by schalkc on 20/08/2014.
 */
@Stepwise
class JRubyExecIntegrationSpec extends Specification {
    static final boolean TESTS_ARE_OFFLINE = System.getProperty('TESTS_ARE_OFFLINE') != null
    static final File TEST_SCRIPT_DIR = new File( System.getProperty('TEST_SCRIPT_DIR') ?: 'src/integTest/resources/scripts')
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/integration-tests'}/jreis")
    static final String TASK_NAME = 'RubyWax'

    def project
    def execTask

    void setup() {
        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project = ProjectBuilder.builder().build()
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.lookout.jruby'
        execTask = project.task(TASK_NAME,type: JRubyExec)
    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Changing the jruby version will load the correct jruby"() {
        when: "Version is set on the task"
            final String newVersion = '1.7.11'
            assert project.jruby.execVersion != newVersion
            execTask.jrubyVersion = newVersion
            project.evaluate()

            def jarName = project.configurations.getByName('jrubyExec$$'+TASK_NAME).files.find { it.toString().find('jruby-complete') }
            def matches = jarName ? (jarName =~ /.*(jruby-complete-.+.jar)/ ) : null

        then: "jruby-complete-${newVersion}.jar must be selected"
            jarName != null
            matches != null
            "jruby-complete-${newVersion}.jar".toString() ==  matches[0][1]
    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Running a Hello World script"() {
        given:
            def output = new ByteArrayOutputStream()
            project.configure(execTask) {
                script        "${TEST_SCRIPT_DIR}/helloWorld.rb"
                standardOutput output
            }

        when:
            project.evaluate()
            execTask.exec()

        then:
            output.toString() == "Hello, World\n"
    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Running a script that requires a gem"() {
        given:
            def output = new ByteArrayOutputStream()
            project.configure(execTask) {
                setEnvironment [:]
                script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
                standardOutput output
            }

        when:
            project.dependencies.add(JRubyExec.JRUBYEXEC_CONFIG,'rubygems:credit_card_validator:1.2.0' )
            project.evaluate()
            execTask.exec()

        then:
            output.toString() == "Not valid\n"
    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Running a script that requires a gem, a separate jRuby and a separate configuration"() {
        given:
            def output = new ByteArrayOutputStream()
            project.with {
                configurations.create('RubyWax')
                dependencies.add('RubyWax','rubygems:credit_card_validator:1.1.0')
                configure(execTask) {
                    script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
                    standardOutput output
                    jrubyVersion   '1.7.11'
                    configuration 'RubyWax'
                }
            }

        when:
            project.evaluate()
            execTask.exec()

        then:
            output.toString() == "Not valid\n"
    }

}