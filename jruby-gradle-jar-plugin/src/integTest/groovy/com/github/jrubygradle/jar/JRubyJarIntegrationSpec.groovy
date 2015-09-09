package com.github.jrubygradle.jar

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

import spock.lang.*

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

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

    def "Building a Jar with a custom configuration and 'java' plugin is applied"() {
        given: "Java plugin applied before JRuby Jar plugin"
        project.apply plugin : 'java'
        Task jrubyJar = project.tasks.getByName('jrubyJar')
        File expectedDir = new File(TESTROOT, 'libs/')
        File expectedJar = new File(expectedDir, 'test-jruby.jar')

        when: "I set the main class"
        project.configure(jrubyJar) {
            mainClass 'bogus.does.not.exist'
        }
        project.evaluate()

        and: "I actually build the JAR"
        jrubyJar.execute()
        def builtJar = fileNames(project.zipTree(expectedJar))

        then: "I expect to see jruby.home unpacked "
        builtJar.contains("META-INF/jruby.home/lib/ruby".toString())

        and: "I expect the new main class to be listed in the manifest"
        jrubyJar.manifest.effectiveManifest.attributes['Main-Class']?.contains('bogus.does.not.exist')
    }

    def "Setting the jrubyVersion to an older version of JRuby should update jar-dependencies"() {
        given: "a version of JRuby which doesn't bundle jar-dependencies"
        Configuration config = project.configurations.create('spockConfig')
        final String version = '1.7.11'
        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            configuration config.name
            jrubyVersion version
        }

        when:
        project.evaluate()

        then: "the JRuby version and jar-dependencies versions should be set"
        config.dependencies.find { it.name == 'jar-dependencies' }
    }

    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/168")
    def "configuring a new jrubyMainsVersion should update the dependency graph properly"() {
        Configuration config = project.configurations.create('spockConfig')
        final String version = '0.3.0'
        JRubyJar task = project.task('spock-jar', type: JRubyJar) {
            configuration config.name
            jrubyMainsVersion version
        }

        when:
        project.evaluate()

        then: "the custom version should be included in the custom config"
        config.dependencies.find { it.name == 'jruby-mains' && it.version == version }
        config.dependencies.findAll({ it.name == 'jruby-mains' }).size() == 1
    }
}

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
    flatDir dirs: "${TESTREPO_LOCATION.absolutePath}"
    maven {
        url "file://" + "${MAVENREPO_LOCATION.absolutePath}"
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
        result.standardOutput.contains('Hello from JRuby')
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
        result.standardOutput.contains("Hello from JRuby")
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
        result.standardOutput.contains("Hello from JRuby: 1.7.19")
    }
}
