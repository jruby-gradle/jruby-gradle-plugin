package com.github.jrubygradle.jar

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

        project.buildscript {
            repositories {
                flatDir dirs : TESTREPO_LOCATION.absolutePath
            }
        }
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.jar'
        project.jruby.defaultRepositories = false

        project.repositories {
            flatDir dirs : TESTREPO_LOCATION.absolutePath
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

    @Ignore("gemDir doesn't make sense")
    def "Adding a fake file as if it is a gem layout"() {
        when: 'We configure the jar task with jruby data'
        new File(TESTROOT,'fake.txt').text = 'fake.content'
        project.configure(jarTask) {
            jruby {
                gemDir TESTROOT
            }
        }

        Set<String> names = fileNames(jarTask.source)

        then: 'Expecting jar task'
        !jarTask.manifest.attributes.containsKey('Main-Class')
        names == (['MANIFEST.MF','fake.txt'] as Set<String>)
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

    @Ignore("gemDir doesn't make sense")
    def "Adding the default gem directory"() {
        given:
        project.jruby.gemInstallDir = TESTROOT.absolutePath
        new File(TESTROOT,'gems').mkdirs()
        new File(TESTROOT,'gems/fake.txt').text = 'fake.content'

        when: "Setting a default main class"
        project.configure(jarTask) {
            jruby {
                defaultGems()
            }
        }

        then: "Then expecting that directory to be found"
        fileNames(jarTask.source) == (['MANIFEST.MF','gems','gems/fake.txt'] as Set<String>)
    }

    def "Adding a default main class"() {
        when: "Setting a default main class"
        project.configure(jarTask) {
            defaultMainClass()
        }
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

    @Ignore("gemDir doesn't make sense")
    def "Adding all defaults"() {
        given: "Given some files in a gem location or which some should be excluded"
        File gems = new File(TESTROOT,'gems')
        gems.mkdirs()
        project.jruby.gemInstallDir = gems
        new File(gems,'gems').mkdirs()
        new File(gems,'data').mkdirs()
        new File(gems,'cache').mkdirs()
        new File(gems,'gems/fake.txt').text = 'fake.content'
        new File(gems,'data/data.txt').text = 'data.content'

        File jars = new File(TESTROOT,'jars')
        jars.mkdirs()
        project.jruby.jarInstallDir = jars
        new File(jars,'fake.jar').text = 'fake.content'
        new File(jars,'Jars.lock').text = 'fake'

        File init = new File(TESTROOT,'init.rb')
        init.text = 'fake.content'

        when: "Setting a default main class and default gems via the 'defaults' method"
        project.configure(jarTask) {
            defaults 'gems','mainClass'
            initScript "${init.absolutePath}"
        }
        jarTask.applyConfig()

        then: "The appropriate files included"
        fileNames(jarTask.source).containsAll(['MANIFEST.MF', 'gems', 'gems/fake.txt',
                                               'data', 'data/data.txt', 'fake.jar',
                                               'Jars.lock', 'init.rb'])

        and: "Then the attribute should be set to the default in the manifest"
        jarTask.manifest.attributes.'Main-Class' == JRubyJar.DEFAULT_MAIN_CLASS
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

    @Ignore("needs rework")
    def "Setting up a java project"() {
        given: "All jar, java plugins have been applied"
        project = setupProject()
        project.apply plugin : 'java'
        Task jar = project.tasks.getByName('jrubyJar')

        and: "A local repository"
        File expectedDir= new File(TESTROOT,'libs/')
        expectedDir.mkdirs()
        project.configure(jar) {
            destinationDir = expectedDir
            initScript library()
        }
        jar.applyConfig()

        expect:
        jar.taskDependencies.getDependencies(jar).contains(project.tasks.getByName('jrubyPrepare'))
    }

    @Ignore("needs rework after gemDir dies")
    def "Building a Jar with a custom configuration and 'java' plugin is applied"() {
        given: "Java plugin applied before JRuby Jar plugin"
        project = setupProject()
        project.apply plugin : 'java'
        Task jrubyJar = project.tasks.getByName('jrubyJar')

        and: "A local repository"
        File expectedDir= new File(TESTROOT,'libs/')
        expectedDir.mkdirs()
        File expectedJar= new File(expectedDir,'test-jruby.jar')
        project.jruby.gemInstallDir = new File(TESTROOT,'fakeGemDir').absolutePath

        new File(project.jruby.gemInstallDir,'gems').mkdirs()
        new File(project.jruby.gemInstallDir,'gems/fake.txt').text = 'fake.content'

        when: "I set the main class"
        project.configure(jrubyJar) {
            destinationDir = expectedDir
            jruby {
                defaultGems()
                mainClass 'bogus.does.not.exist'
                initScript runnable()
            }
        }
        project.evaluate()

        and: "I actually build the JAR"
        jrubyJar.execute()
        def builtJar = fileNames(project.zipTree(expectedJar))

        then: "I expect to see jruby.home unpacked "
        builtJar.contains("META-INF/jruby.home/lib/ruby".toString())

        and: "To see my fake files in the 'gems' folder"
        builtJar.contains("gems/fake.txt".toString())

        and: "I expect the new main class to be listed in the manifest"
        jrubyJar.manifest.effectiveManifest.attributes['Main-Class']?.contains('bogus.does.not.exist')
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

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/115')
    def "jrubyVersion is lazily evaluated"() {
        given:
        final String version = '1.7.20'
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.base'
        project.apply plugin: 'com.github.jruby-gradle.jar'

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

    def "prepareTask should have its configuration lazily set"() {
        given:
        Task prepareTask = jarTask.dependsOn.find { it instanceof JRubyPrepare }

        when:
        project.evaluate()

        then:
        prepareTask.dependencies.find { (it instanceof Configuration) && (it.name == jarTask.configuration) }
    }
}
