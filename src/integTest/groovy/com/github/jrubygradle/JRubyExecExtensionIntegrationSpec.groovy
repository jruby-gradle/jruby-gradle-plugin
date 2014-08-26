package com.github.jrubygradle

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author Schalk W. Cronjé
 */
class JRubyExecExtensionIntegrationSpec extends Specification {

    static final boolean TESTS_ARE_OFFLINE = System.getProperty('TESTS_ARE_OFFLINE') != null
    static final File TEST_SCRIPT_DIR = new File( System.getProperty('TEST_SCRIPT_DIR') ?: 'src/integTest/resources/scripts')
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/integration-tests'}/jreeis")

    def project

    void setup() {
        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project = ProjectBuilder.builder().build()
        project.with {
            buildDir = TESTROOT
            logging.level = LIFECYCLE
            apply plugin: 'com.lookout.jruby'
            evaluate()
        }
    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Run a script with minimum parameters"() {
        given:
            def output = new ByteArrayOutputStream()

        when: "I call jrubyexec with only a script name"
            project.jrubyexec {
                script        "${TEST_SCRIPT_DIR}/helloWorld.rb"
                standardOutput output
            }

        then: "I expect the Ruby script to be executed"
            output.toString() == "Hello, World\n"
    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Run a script containing a conditional"() {
        given:
            def output = new ByteArrayOutputStream()

        when: "we have an 'if' clause"
            project.jrubyexec {
                script        "${TEST_SCRIPT_DIR}/helloName.rb"
                if(input == 0) {
                    scriptArgs 'Stan'
                } else {
                    scriptArgs 'man'
                }
                standardOutput output
            }

        then: "only the appropriate parameters should be passed"
            output.toString() == expected

        where:
            input | expected
            0     | "Hello, Stan\n"
            1     | "Hello, man\n"

    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Running a script that requires a gem, a separate jRuby and a separate configuration"() {
        given:
            def output = new ByteArrayOutputStream()

        when:
            project.with {
                dependencies {
                    jrubyExec 'rubygems:credit_card_validator:1.1.0'
                }
                jrubyexec {
                    script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
                    standardOutput output
                    jrubyArgs '-T1'
                }
            }

        then:
            output.toString() == "Not valid\n"
    }

}
