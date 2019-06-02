package com.github.jrubygradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Unit test covering the JRuby extensions (i.e. project.jruby)
 */
class JRubyPluginExtensionSpec extends Specification {
    Project project

    void setup() {
        project = ProjectBuilder.builder().build()
    }

    void "changing the defaultVersion via a method should work"() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        final String version = '9.0.1.0'

        when:
        ext.jrubyVersion {version}

        then:
        ext.jrubyVersion == version
    }

    void "changing the defaultVersion with a setter should work"() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        final String version = '9.0.1.0'

        when:
        ext.jrubyVersion = version

        then:
        ext.jrubyVersion == version
    }

//    void "changing the defaultVersion should invoke registered callbacks"() {
//        given:
//        JRubyPluginExtension ext = new JRubyPluginExtension(project)
//        Boolean executedCallback = false
//
//        when:
//        ext.registerDefaultVersionCallback {
//            executedCallback = true
//        }
//        ext.defaultVersion = '9.0.1.0'
//
//        then:
//        executedCallback
//    }

//    void "changing defaultVersion with execVersion callbacks should invoke it"() {
//        given:
//        JRubyPluginExtension ext = new JRubyPluginExtension(project)
//        Boolean executedCallback = false
//        final String version = '9.0.1.0'
//
//        when:
//        ext.registerExecVersionCallback { String v ->
//            executedCallback = v == version
//        }
//        ext.defaultVersion = '9.0.1.0'
//
//        then:
//        executedCallback
//    }

//    void "changing the execVersion should invoke registered callbacks"() {
//        given:
//        JRubyPluginExtension ext = new JRubyPluginExtension(project)
//        Boolean executedCallback = false
//        final String version = '9.0.1.0'
//
//        when:
//        ext.registerExecVersionCallback { String v ->
//            executedCallback = (v == version)
//        }
//        ext.execVersion = '9.0.1.0'
//
//        then:
//        executedCallback
//    }
//
//    void "changing default and exec versions "() {
//        given:
//        JRubyPluginExtension ext = new JRubyPluginExtension(project)
//        Boolean executedCallback = false
//        final String version = '9.0.1.0'
//        int calledbackTimes = 0
//
//        when:
//        ext.registerExecVersionCallback { String v ->
//            calledbackTimes += 1
//            executedCallback = (v == version)
//        }
//        ext.execVersion = version
//        ext.defaultVersion = '9.0.1.1'
//
//        then:
//        executedCallback
//        calledbackTimes == 1
//    }
}
