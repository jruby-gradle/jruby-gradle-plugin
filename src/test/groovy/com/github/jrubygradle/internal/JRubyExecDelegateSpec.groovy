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
            def cl = {
                script 'path/to/file'
                jrubyArgs 'c','d','-S'
                scriptArgs '-x'
                scriptArgs '-y','-z'
                jrubyArgs 'a','b'
            }
            cl.delegate = jred
            cl.call()

        expect:
            jred.passthrough.size() == 0
            jred.script == 'path/to/file'
            jred.scriptArgs == ['-x','-y','-z']
            jred.jrubyArgs == ['c','d','-S','a','b']
            jred.buildArgs() == ['c','d','-S','a','b','path/to/file','-x','-y','-z']
    }

    def "When passing absolute file and absolute file, expect check for existence to be executed"() {
        given:
            def cl = {
                script '/path/to/file'
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
                executable '/path/to/file'
                jvmArgs '-x'
                jvmArgs '-y','-z'
            }
            cl.delegate = jred
            cl.call()

        expect:
            jred.valuesAt(0) == '/path/to/file'
            jred.valuesAt(1) == '-x'
            jred.valuesAt(2) == ['-y','-z']
            jred.keyAt(0) == 'executable'
            jred.keyAt(1) == 'jvmArgs'
            jred.keyAt(2) == 'jvmArgs'

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
