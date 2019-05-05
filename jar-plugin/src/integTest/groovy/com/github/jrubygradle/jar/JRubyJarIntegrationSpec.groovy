package com.github.jrubygradle.jar

import org.gradle.api.Project
import spock.lang.Ignore
import spock.lang.Specification

/*
 * A series of tests which expect to use the JRubyJar task in more of an integration
 * test fashion, i.e. evaluating the Project, etc
 */

@Ignore
class JRubyJarIntegrationSpec extends Specification {
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}/jrjps")
    static final File TESTREPO_LOCATION = new File("${System.getProperty('TESTREPO_LOCATION') ?: 'build/tmp/test/repo'}")

    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
        project.gradle.startParameter.offline = true

        project.buildscript {
            repositories {
                flatDir dirs: TESTREPO_LOCATION.absolutePath
            }
        }
        project.buildDir = TESTROOT
        project.with {
            logging.level = LIFECYCLE
            apply plugin: 'com.github.jruby-gradle.jar'
            jruby.defaultRepositories = false

            repositories {
                flatDir dirs: TESTREPO_LOCATION.absolutePath
            }
        }
    }

}

