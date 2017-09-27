package com.github.jrubygradle.jar

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Issue
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class JRubyJarTestKitSpec extends Specification {
    static final File TESTREPO_LOCATION = new File("${System.getProperty('TESTREPO_LOCATION') ?: 'build/tmp/test/repo'}")
    static final File MAVENREPO_LOCATION = new File("${TESTREPO_LOCATION}/../../../../../jruby-gradle-base-plugin/src/integTest/mavenrepo")
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
jruby.defaultRepositories = false
repositories {
    flatDir dirs: "${TESTREPO_LOCATION.absoluteFile.toURI().toURL()}"
    maven {
        url "${MAVENREPO_LOCATION.absoluteFile.toURI().toURL()}"
    }
}
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

        and: "the should not be a jruby-mains.jar or jruby-complete.jar inside the archive"
        ZipFile zip = new ZipFile(builtArtifacts.getAt(0))
        !zip.entries().findAll { ZipEntry entry ->
            entry.name.matches(/(.*)jruby-complete-(.*).jar/) || entry.name.matches(/(.*)jruby-mains-(.*).jar/)
        }

        and:
        result.output.contains('Hello from JRuby')
    }


    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/183")
    def "creating a new task based on JRubyJar produces a jar artifact"() {
        given:
        buildFile << """
import com.github.jrubygradle.jar.JRubyJar

task someDifferentJar(type: JRubyJar) { initScript 'main.rb' }

task validateJar(type: Exec) {
    dependsOn someDifferentJar
    environment [:]
    workingDir "\${buildDir}/libs"
    commandLine 'java', '-jar', someDifferentJar.outputs.files.singleFile.absolutePath
}
    """

        when:
        BuildResult result = GradleRunner.create()
            .withProjectDir(testProjectDir.root)
            .withArguments('validateJar')
            .build()

        then:
        builtArtifacts && builtArtifacts.size() == 1
        result.task(":validateJar").outcome == TaskOutcome.SUCCESS
        result.output.contains("Hello from JRuby")
    }

    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/183")
    def "modifying jruby.defaultVersion should be included in the artifact"() {
        given:
        buildFile << """
jruby.defaultVersion = '1.7.19'
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
            .withArguments('validateJar', '--info')
            .build()

        then:
        builtArtifacts && builtArtifacts.size() == 1

        and:
        result.task(":validateJar").outcome == TaskOutcome.SUCCESS
        result.output.contains("Hello from JRuby: 1.7.19")

        and: 'there should be a warning about using an older JRuby'
        /* see: https://github.com/jruby-gradle/jruby-gradle-plugin/issues/191 */
        result.output.contains('unexpected behavior')
    }


    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/pull/271')
    def 'using a more recent jar-dependencies should work'() {
        given:
        buildFile << """
repositories {
    maven { url 'http://rubygems.lasagna.io/proxy/maven/releases' }
    maven { url 'http://rubygems-proxy.torquebox.org/releases' }
}
dependencies {
    jrubyJar 'rubygems:jar-dependencies:0.2.3'
}
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
            .withArguments('validateJar', '--info')
            .build()

        then:
        builtArtifacts && builtArtifacts.size() == 1

        and:
        result.task(":validateJar").outcome == TaskOutcome.SUCCESS
        result.output.contains("Hello from JRuby")
    }
}
