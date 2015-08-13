package com.github.jrubygradle.jar

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

import spock.lang.*

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class JRubyJarIntegrationSpec extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    File rubyFile

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        /* let's create a sample file to include */
        rubyFile = testProjectDir.newFile('main.rb')
        rubyFile << """
puts "Hello from JRuby: #{JRUBY_VERSION}"
"""
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.json")

        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        String pluginDependencies = pluginClasspathResource.text

        buildFile << """
buildscript {
    dependencies {
        classpath files(${pluginDependencies})
    }
}
apply plugin: 'com.github.jruby-gradle.jar'

repositories { jcenter() }
"""
    }

    File[] getBuiltArtifacts() {
        return (new File(testProjectDir.root, ['build', 'libs'].join(File.separator))).listFiles()
    }

    def "executing the jrubyJar default task produces a jar artifact"() {
        given:
        buildFile << """
jrubyJar {
}
    """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('jrubyJar')
                .build()

        then:
        builtArtifacts && builtArtifacts.size() == 1

        and:
        result.task(":jrubyJar").outcome == TaskOutcome.SUCCESS
    }

    def "executing the jrubyJar task produces an executable artifact"() {
        given:
        buildFile << """
jrubyJar { initScript 'main.rb' }

task validateJar(type: Exec) {
    dependsOn jrubyJar
    environment [:]
    workingDir "\${buildDir}/libs"
    commandLine 'java', '-jar', jrubyJar.outputs.files.singleFile.absolutePath
}
"""

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('validateJar')
                .build()

        then:
        builtArtifacts && builtArtifacts.size() == 1
        result.task(":jrubyJar").outcome == TaskOutcome.SUCCESS
        result.task(":validateJar").outcome == TaskOutcome.SUCCESS
    }


    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/183")
    def "creating a new task based on JRubyJar produces a jar artifact"() {
        given:
        buildFile << """
import com.github.jrubygradle.jar.JRubyJar

task someDifferentJar(type: JRubyJar) {
}
    """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('someDifferentJar')
                .build()

        then:
        builtArtifacts && builtArtifacts.size() == 1
        result.task(":someDifferentJar").outcome == TaskOutcome.SUCCESS
    }

    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/183")
    def "modifying jruby.defaultVersion should be included in the artifact"() {
        given:
        buildFile << """
jruby.defaultVersion = '1.7.11'
jrubyJar {
}
    """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('jrubyJar', '--info')
                .build()

        then:
        builtArtifacts && builtArtifacts.size() == 1
>>>>>>> Implement a callback system for setting the defaultVersion and execVersion
        result.task(":jrubyJar").outcome == TaskOutcome.SUCCESS
    }
}
