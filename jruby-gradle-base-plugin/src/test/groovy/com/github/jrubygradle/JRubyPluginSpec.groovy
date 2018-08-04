package com.github.jrubygradle

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static com.github.jrubygradle.JRubyPlugin.TORQUEBOX_RUBYGEMS_RELEASE_URL

/**
 */
class JRubyPluginSpec extends Specification {
    static final File TESTROOT = new File(System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests')
    Project project

    def setup() {
        if (TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.base'
    }

    def "plugin should set repositories correctly"() {
        when:
        project.evaluate()

        then:
        hasRepositoryUrl(project, TORQUEBOX_RUBYGEMS_RELEASE_URL)
    }

    def "setting the default repository via rubygemsRelease()"() {
        when:
        project.evaluate()

        then: "rubygemsRelease() should be defined"
        project.repositories.metaClass.respondsTo(project.repositories,'rubygemsRelease')

        and:
        hasRepositoryUrl(project, TORQUEBOX_RUBYGEMS_RELEASE_URL)
    }

    def "applying the plugin with no properties should have jruby.defaultVersion defaulted"() {
        when:
        project.evaluate()

        then:
        project.jruby.defaultVersion == JRubyPluginExtension.DEFAULT_JRUBY_VERSION
    }

    def "applying the plugin with -PjrubyVersion= set should changej jruby.defaultVersion"() {
        given:
        final String version = '9.0.1.0'
        project = ProjectBuilder.builder().build()
        project.with {
            ext.jrubyVersion = version
        }
        project.apply plugin: 'com.github.jruby-gradle.base'

        when:
        project.evaluate()

        then:
        project.jruby.defaultVersion == version
    }

    private boolean hasRepositoryUrl(Project p, String url) {
        boolean result = false
        p.repositories.each { ArtifactRepository r ->
            if (r.url.toString() == url) {
                result = true
            }
        }
        return result
    }
}
