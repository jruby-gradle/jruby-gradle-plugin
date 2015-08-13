package com.github.jrubygradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*


/**
 * Unit test covering the JRuby extensions (i.e. project.jruby)
 */
class JRubyPluginExtensionSpec extends Specification {
    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
    }

    def "Creating a JRubyPlugin instance"() {
        when:
        JRubyPluginExtension jrpe = new JRubyPluginExtension(project)

        then:
        jrpe.defaultRepositories
        jrpe.defaultVersion == jrpe.execVersion
        jrpe.gemInstallDir != project.buildDir
        jrpe.gemInstallDir == new File(project.buildDir, 'gems').absoluteFile

        when:
        jrpe.gemInstallDir = { 'vendor2' }

        then:
        jrpe.gemInstallDir == new File(project.projectDir, 'vendor2').absoluteFile
    }

    def "changing the defaultVersion via a method should work"() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        final String version = '1.7.11'

        when:
        ext.defaultVersion version

        then:
        ext.defaultVersion == version
    }

    def "changing the defaultVersion with a setter should work"() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        final String version = '1.7.11'

        when:
        ext.defaultVersion = version

        then:
        ext.defaultVersion == version
    }

    def "changing the defaultVersion should invoke registered callbacks"() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        Boolean executedCallback = false

        when:
        ext.registerDefaultVersionCallback {
            executedCallback = true
        }
        ext.defaultVersion = '1.7.11'

        then:
        executedCallback
    }

    def "changing defaultVersion with execVersion callbacks should invoke it"() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        Boolean executedCallback = false
        final String version = '1.7.11'

        when:
        ext.registerExecVersionCallback { String v ->
            executedCallback = v == version
        }
        ext.defaultVersion = '1.7.11'

        then:
        executedCallback
    }

    def "changing the execVersion should invoke registered callbacks"() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        Boolean executedCallback = false
        final String version = '1.7.11'

        when:
        ext.registerExecVersionCallback { String v ->
            executedCallback = (v == version)
        }
        ext.execVersion = '1.7.11'

        then:
        executedCallback
    }

    def "changing default and exec versions "() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        Boolean executedCallback = false
        final String version = '1.7.11'
        int calledbackTimes = 0

        when:
        ext.registerExecVersionCallback { String v ->
            calledbackTimes += 1
            executedCallback = (v == version)
        }
        ext.execVersion = version
        ext.defaultVersion = '1.7.19'

        then:
        executedCallback
        calledbackTimes == 1
    }
}
