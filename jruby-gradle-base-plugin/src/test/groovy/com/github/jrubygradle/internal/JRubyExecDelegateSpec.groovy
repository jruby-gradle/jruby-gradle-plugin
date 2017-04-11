package com.github.jrubygradle.internal

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Specification

import static org.gradle.api.logging.LogLevel.LIFECYCLE
import org.gradle.api.InvalidUserDataException

// ===============================================
// *** DO NOT CAll jrubyexec IN THIS UNITTEST ***
// Use JRubyExecExtensionIntegrationSpec instead
// ===============================================


/**
 * @author Schalk W. Cronjé
 */
class JRubyExecDelegateSpec extends Specification {
    static final File TEST_SCRIPT_DIR = new File( System.getProperty('TESTS_SCRIPT_DIR') ?: 'src/test/resources/scripts')
    static final File TESTROOT = new File(System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests')
    static final String absFilePrefix = System.getProperty('os.name').toLowerCase().startsWith('windows') ? 'C:' : ''

    def project
    JRubyExecDelegate jred = new JRubyExecDelegate()

    void setup() {
        project = ProjectBuilder.builder().build()
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.base'
    }

    def "When just passing script, scriptArgs, jrubyArgs, expect local properties to be updated"() {
        given:
            def xplatformFileName = new File('path/to/file')
            def cl = {
                script 'path/to/file'
                jrubyArgs 'c','d','-S'
                scriptArgs '-x'
                scriptArgs '-y','-z'
                jrubyArgs 'a','b'
                gemWorkDir 'path/to/file'
            }
            cl.delegate = jred
            cl.call()

        expect:
            jred.passthrough.size() == 0
            jred.script == xplatformFileName
            jred._convertScriptArgs() == ['-x','-y','-z']
            jred._convertJrubyArgs() == ['c','d','-S','a','b']
            jred.buildArgs() == ['-rjars/setup', 'c','d','-S','a','b',xplatformFileName.toString(),'-x','-y','-z']
            jred._convertGemWorkDir(project) == project.file('path/to/file')
    }

    def "When passing absolute file and absolute file, expect check for existence to be executed"() {
        given:
            def cl = {
                script absFilePrefix + '/path/to/file'
                jrubyArgs 'c','d','-S'
                scriptArgs '-x'
                scriptArgs '-y','-z'
                jrubyArgs 'a','b'
            }
            cl.delegate = jred
            cl.call()
        when:
            jred.buildArgs()

        then:
            thrown(InvalidUserDataException)
    }

    def "When just passing arbitrary javaexec, expect them to be stored"() {
        given:
            def cl = {
                environment 'XYZ', '123'
                executable '/path/to/file'
                jvmArgs '-x'
                jvmArgs '-y','-z'
            }
            cl.delegate = jred
            cl.call()

        expect:
            jred.valuesAt(0) == ['XYZ', '123']
            jred.valuesAt(1) == '/path/to/file'
            jred.valuesAt(2) == '-x'
            jred.valuesAt(3) == ['-y','-z']
            jred.keyAt(0) == 'environment'
            jred.keyAt(1) == 'executable'
            jred.keyAt(2) == 'jvmArgs'
            jred.keyAt(3) == 'jvmArgs'

    }

    def "When using a conditional, expect specific calls to be passed"() {
        given:
            def cl = {
                if(condition == 1) {
                    jvmArgs '-x'
                } else {
                    jvmArgs '-y'
                }
            }
            cl.delegate = jred
            cl.call()

        expect:
            jred.valuesAt(0) == parameter

        where:
            condition | parameter
                1     | '-x'
                5     | '-y'

    }

    def "Prevent main from being called"() {
        when:
            def cl = {
                main 'some.class'
            }
            cl.delegate = jred
            cl.call()

        then:
            thrown(UnsupportedOperationException)
    }

    def "Prevent args from being called"() {
        when:
            def cl = {
                args '-x','-y'
            }
            cl.delegate = jred
            cl.call()

        then:
            thrown(UnsupportedOperationException)
    }

    def "Prevent setArgs from being called"() {
        when:
            def cl = {
                setArgs '-x','-y'
            }
            cl.delegate = jred
            cl.call()

        then:
            thrown(UnsupportedOperationException)
    }

}
