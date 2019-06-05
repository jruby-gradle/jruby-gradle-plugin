package com.github.jrubygradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

/**
 * Unit test covering the JRuby extensions (i.e. project.jruby)
 */
class JRubyPluginExtensionSpec extends Specification {
    Project project = ProjectBuilder.builder().build()

    void "changing the defaultVersion via a method should work"() {
        given:
        JRubyPluginExtension ext = new JRubyPluginExtension(project)
        final String version = '9.0.1.0'

        when:
        ext.jrubyVersion { version }

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
}
