package com.github.jrubygradle

import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class JRubyPluginSpec extends Specification {
    public static final String RUBYGEMS = 'https://rubygems.org'

    Project project

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.base'
    }

    void "applying the plugin with no properties should have jruby.defaultVersion defaulted"() {
        when:
        project.evaluate()

        then:
        project.jruby.defaultVersion == JRubyPluginExtension.DEFAULT_JRUBY_VERSION
    }

//    void "applying the plugin with -PjrubyVersion= set should changej jruby.defaultVersion"() {
//        given:
//        final String version = '9.0.1.0'
//        project = ProjectBuilder.builder().build()
//        project.with {
//            ext.jrubyVersion = version
//        }
//        project.apply plugin: 'com.github.jruby-gradle.base'
//
//        when:
//        project.evaluate()
//
//        then:
//        project.jruby.defaultVersion == version
//    }

    private Collection hasRepositoryUrl(Project p, String url) {
        p.repositories.findAll { ArtifactRepository r ->
            r instanceof IvyArtifactRepository
        }.findAll {
            it.ivyPattern.contains(url)
        }
    }
}
