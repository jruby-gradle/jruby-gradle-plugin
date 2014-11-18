package com.github.jrubygradle

import com.github.jrubygradle.testhelper.BasicProjectBuilder
import com.github.jrubygradle.testhelper.VersionFinder
import spock.lang.*

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author Schalk W. Cronjé
 */
class JRubyExecExtensionIntegrationSpec extends Specification {

    static final File CACHEDIR = new File( System.getProperty('TEST_CACHEDIR') ?: 'build/tmp/integrationTest/cache')
    static final File FLATREPO = new File( System.getProperty('TEST_FLATREPO') ?: 'build/tmp/integrationTest/flatRepo')
    static final boolean TESTS_ARE_OFFLINE = System.getProperty('TESTS_ARE_OFFLINE') != null
    static final File TEST_SCRIPT_DIR = new File( System.getProperty('TEST_SCRIPT_DIR') ?: 'src/integTest/resources/scripts')
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/integration-tests'}/jreeis")

    def project

    void setup() {
        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project= BasicProjectBuilder.buildWithLocalRepo(TESTROOT,FLATREPO,CACHEDIR)

        project.evaluate()
    }

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

    def "Running a script that requires a gem, a separate jRuby and a separate configuration"() {
        given:
            def output = new ByteArrayOutputStream()

        when:
            project.with {
                dependencies {
                    jrubyExec VersionFinder.findDependency(FLATREPO,'','credit_card_validator','gem')

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

    def "Running a script that requires a gem, a separate jRuby, a separate configuration and a custom gemWorkDir"() {
        given:
            def output = new ByteArrayOutputStream()

        when:
            project.with {
                dependencies {
                    jrubyExec VersionFinder.findDependency(FLATREPO,'','credit_card_validator','gem')

                }
                jrubyexec {
                    script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
                    standardOutput output
                    jrubyArgs '-T1'
                    gemWorkDir  new File(TESTROOT,'customGemDir')
                }
            }

        then:
            output.toString() == "Not valid\n"
            new File(TESTROOT,'customGemDir').exists()

    }
}
