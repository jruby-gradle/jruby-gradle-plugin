package com.github.jrubygradle.jar

import com.github.jrubygradle.JRubyPlugin
import com.github.jrubygradle.JRubyPrepare
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author R. Tyler Croy
 * @author Schalk W. Cronjé
 * @author Christian Meier
 *
 */
class JRubyJarPluginSpec extends Specification {
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}/jrjps")
    static final File TESTREPO_LOCATION = new File("${System.getProperty('TESTREPO_LOCATION') ?: 'build/tmp/test/repo'}")

    Project project
    JRubyJar jarTask

    static Set<String> fileNames(FileCollection fc) {
        Set<String> names = []
        fc.asFileTree.visit { fvd ->
            names.add(fvd.relativePath.toString())
        }
        return names
    }

    static Project setupProject() {
        Project project = ProjectBuilder.builder().build()
        project.gradle.startParameter.offline = true
        File repo = project.file("${TESTREPO_LOCATION}/../../../../../jruby-gradle-base-plugin/src/integTest/mavenrepo")

        project.buildDir = TESTROOT

        project.with {
            apply plugin: 'com.github.jruby-gradle.jar'
            jruby.defaultRepositories = false

            repositories {
                flatDir dirs : TESTREPO_LOCATION.absolutePath
                maven {
                    url "file://" + repo.absolutePath
                }
            }
        }

        return project
    }

    void setup() {
        if (TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()

        project = setupProject()
        jarTask = project.tasks.getByName('jrubyJar')
    }

    def 'Checking tasks exist'() {
        expect:
        project.tasks.getByName('jrubyJar')
    }

    def 'Checking appendix'() {
        expect:
        project.tasks.getByName('jrubyJar').appendix == 'jruby'
    }

    def "Fails on adding non-existing initScript"() {
        given:
        project.jruby.gemInstallDir = TESTROOT.absolutePath
        new File(TESTROOT,'gems').mkdirs()
        new File(TESTROOT,'gems/fake.txt').text = 'fake.content'

        when: "Setting a default main class"
        project.configure(jarTask) {
            initScript 'not.existing'
        }
        jarTask.applyConfig()

        then: "Then expecting use error"
        thrown(InvalidUserDataException)
    }

    def "Adding a default main class"() {
        when: "Setting a default main class"
        project.configure(jarTask) {
            defaultMainClass()
        }
        jarTask.addJRubyDependency()
        jarTask.applyConfig()

        then: "Then the attribute should be set to the default in the manifest"
        jarTask.manifest.attributes.'Main-Class' == JRubyJar.DEFAULT_MAIN_CLASS
    }

    def "Adding a default extracting main class"() {
        when: "Setting a default extracting main class"
        project.configure(jarTask) {
            extractingMainClass()
        }
        jarTask.applyConfig()

        then: "Then the attribute should be set to the default in the manifest"
        jarTask.manifest.attributes.'Main-Class' == JRubyJar.EXTRACTING_MAIN_CLASS
    }

    def "Adding a custom main class"() {
        when: "Setting a default main class"
        project.configure(jarTask) {
            mainClass 'org.scooby.doo.snackMain'
        }
        jarTask.applyConfig()

        then: "Then the attribute should be set accordingly in the manifest"
        jarTask.manifest.attributes.'Main-Class' == 'org.scooby.doo.snackMain'
    }

    def "Adding a main class and additional manifest attributes"() {
        when: "Setting a main class"
        project.configure(jarTask) {
            mainClass 'org.scooby.doo.snackMain'
            manifest.attributes('Class-Path': 'gangway.jar zoinks.jar')
        }
        jarTask.applyConfig()

        then: "Then the Main-Class attribute does not erase other attributes"
        jarTask.manifest.attributes.'Class-Path' == 'gangway.jar zoinks.jar'
    }

    def "Setting up a java project"() {
        given: "All jar, java plugins have been applied"
        project = setupProject()
        project.apply plugin : 'java'
        Task jar = project.tasks.getByName('jrubyJar')

        and: "A local repository"
        File expectedDir= new File(TESTROOT,'libs/')
        expectedDir.mkdirs()
        project.configure(jar) {
            initScript library()
        }
        jar.applyConfig()

        expect:
        jar.taskDependencies.getDependencies(jar).contains(project.tasks.getByName('prepareJRubyJar'))
    }

    def 'Checking setting no mainClass'() {
        when:
        project.file( 'app.rb') << ''
        jarTask.initScript('app.rb')
        jarTask.applyConfig()

        then:
        jarTask.manifest.attributes['Main-Class'] == JRubyJar.DEFAULT_MAIN_CLASS
    }

    def 'Checking setting of mainClass once'() {
        when:
        project.file( 'app.rb') << ''
        jarTask.initScript('app.rb')
        jarTask.mainClass('org.example.Main')
        jarTask.applyConfig()

        then:
        jarTask.manifest.attributes['Main-Class'] == 'org.example.Main'
    }

    def 'Checking setup runnable jrubyJar task'() {
        when:
        jarTask.initScript(jarTask.runnable())
        jarTask.applyConfig()

        then:
        jarTask.manifest.attributes.containsKey('Main-Class')
    }

    def 'Checking valid library config'() {
        when:
        jarTask.initScript(jarTask.library())
        jarTask.applyConfig()

        then:
        !jarTask.manifest.attributes.containsKey('Main-Class')
    }

    def 'Checking invalid library config'() {
        when:
        jarTask.initScript(jarTask.library())
        jarTask.extractingMainClass()
        jarTask.applyConfig()

        then:
        Exception e = thrown()
        e.message == 'can not have mainClass for library'
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

    def "prepareTask should be an instance of JRubyPrepare"() {
        expect:
        jarTask.dependsOn.find { (it instanceof JRubyPrepare) && (it.name == 'prepareJRubyJar') }
    }

    @Ignore('should be an integration test since we add jar-dependencies')
    def "prepareTask should have its configuration lazily set"() {
        given:
        Task prepareTask = jarTask.dependsOn.find { it instanceof JRubyPrepare }

        when:
        project.evaluate()

        then:
        prepareTask.dependencies.find { (it instanceof Configuration) && (it.name == jarTask.configuration) }
    }
}
