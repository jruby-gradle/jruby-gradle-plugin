package com.lookout.jruby

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*
import static org.gradle.api.logging.LogLevel.*

/**
 * @author R. Tyler Croy
 *
 */
class JRubyJarSpec extends Specification {
    static final File TEST_SCRIPT_DIR = new File( System.getProperty('TEST_SCRIPT_DIR') ?: 'src/test/resources/scripts')
    static final File TESTROOT = new File(System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests')
    static final String TASK_NAME = 'JarJar'

    def project
    def jarTask

    void setup() {
        project = ProjectBuilder.builder().build()
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.lookout.jruby'
        jarTask = project.task(TASK_NAME, type: JRubyJar)

    }

    def "basic sanity check"() {
        expect: "jarTask to be an instance"
            jarTask instanceof JRubyJar
            jarTask.group == 'JRuby'
    }
}
