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

    /*
        @Ignore('should be an integration test since we add jar-dependencies')
    def "prepareTask should have its configuration lazily set"() {
        given:
        Task prepareTask = jarTask.dependsOn.find { it instanceof JRubyPrepare }

        when:
        project.evaluate()

        then:
        prepareTask.dependencies.find { (it instanceof Configuration) && (it.name == jarTask.configuration) }
    }

        def "Adding a default main class"() {
        when: "Setting a default main class"
        project.configure(jarTask) {
            defaultMainClass()
        }
        jarTask.addJRubyDependency()
        jarTask.applyConfig()

        then: "Then the attribute should be set to the default in the manifest"
        jarTask.manifest.attributes.'Main-Class' == DEFAULT_MAIN_CLASS
    }

        @Ignore('should be an integration test since we add jar-dependencies')
    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/115')
    def "jrubyVersion is lazily evaluated"() {
        given:
        final String version = '1.7.11'

        when:
        project.jruby {
            defaultVersion version
        }
        project.evaluate()

        then:
        project.tasks.findByName('jrubyJar').jrubyVersion == version
    }

     */
}

