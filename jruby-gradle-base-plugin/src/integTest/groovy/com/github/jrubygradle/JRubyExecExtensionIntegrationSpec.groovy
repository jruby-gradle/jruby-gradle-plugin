package com.github.jrubygradle

import com.github.jrubygradle.testhelper.BasicProjectBuilder
import com.github.jrubygradle.testhelper.VersionFinder
import org.gradle.api.Project
import spock.lang.*

/**
 * @author Schalk W. Cronjé
 */
class JRubyExecExtensionIntegrationSpec extends Specification {

    static final File CACHEDIR = new File( System.getProperty('TEST_CACHEDIR') ?: 'build/tmp/integrationTest/cache')
    static final File FLATREPO = new File( System.getProperty('TEST_FLATREPO') ?: 'build/tmp/integrationTest/flatRepo')
    static final boolean TESTS_ARE_OFFLINE = System.getProperty('TESTS_ARE_OFFLINE') != null
    static final File TEST_SCRIPT_DIR = new File( System.getProperty('TEST_SCRIPT_DIR') ?: 'src/integTest/resources/scripts').absoluteFile
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/integration-tests'}/jreeis").absoluteFile
    static final File TEST_JARS_DIR = new File(TESTROOT, "build/tmp/jrubyExec/jars")

    Project project
    ByteArrayOutputStream output = new ByteArrayOutputStream()

    String getOutputBuffer() {
        return output.toString()
    }

    void setup() {
        if (TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project = BasicProjectBuilder.buildWithLocalRepo(TESTROOT,FLATREPO,CACHEDIR)

        project.evaluate()
    }

    def "Run a script with minimum parameters"() {
        when: "I call jrubyexec with only a script name"
        project.jrubyexec {
            script        "${TEST_SCRIPT_DIR}/helloWorld.rb"
            standardOutput output
        }

        then: "I expect the Ruby script to be executed"
        outputBuffer =~ /Hello, World/
    }

    def "Run an inline script"() {
        when: "I call jrubyexec with only a script name"
        project.jrubyexec {
            jrubyArgs      "-e", "puts 'Hello, World'"
            standardOutput output
        }

        then: "I expect the Ruby script to be executed"
        outputBuffer =~ /Hello, World/
    }

    def "Run a script containing a conditional"() {
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
        outputBuffer == expected

        where:
        input | expected
        0     | "Hello, Stan\n"
        1     | "Hello, man\n"

    }

    def "Running a script that requires a jar"() {
        when:
        project.with {
            dependencies {
                jrubyExec VersionFinder.findDependency(FLATREPO,'org.bouncycastle','bcprov-jdk15on','jar')

            }
            jrubyexec {
                jrubyArgs '-e'
                jrubyArgs 'print $CLASSPATH'
                standardOutput output
            }
        }

        then:
        outputBuffer == "[\"${new File(TEST_JARS_DIR, 'org/bouncycastle/bcprov-jdk15on/1.46/bcprov-jdk15on-1.46.jar').toURL()}\"]"
    }

    def "Running a script that requires a gem, a separate jRuby and a separate configuration"() {
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
        outputBuffer =~ /Not valid/
    }

    def "Running a script that requires a gem, a separate jRuby, a separate configuration and a custom gemWorkDir"() {
        given:
        final String customGemDir = 'customGemDir'

        when:
        project.with {
            dependencies {
                jrubyExec VersionFinder.findDependency(FLATREPO,'','credit_card_validator','gem')

            }
            jrubyexec {
                script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
                standardOutput output
                jrubyArgs '-T1'
                gemWorkDir  "${buildDir}/${customGemDir}"
            }
        }

        then:
        outputBuffer =~ /Not valid/
        new File(project.buildDir, customGemDir).exists()

    }
}
