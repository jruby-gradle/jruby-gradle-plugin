package com.github.jrubygradle.jar

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author R. Tyler Croy
 * @author Schalk W. Cronjé
 *
 */
class JRubyJarPluginSpec extends Specification {
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}/jrjps")
    static final File TESTREPO_LOCATION = new File("${System.getProperty('TESTREPO_LOCATION') ?: 'build/tmp/test/repo'}")

    def project
    def jarTask

    static Set<String> fileNames(FileCollection fc) {
        Set<String> names = []
        fc.asFileTree.visit { fvd ->
            names.add(fvd.relativePath.toString())
        }
        return names
    }

    static Project setupProject( boolean withShadow ) {
        Project project = ProjectBuilder.builder().build()

        project.buildscript {
            repositories {
                flatDir dirs : TESTREPO_LOCATION.absolutePath
            }

            dependencies {
                classpath 'com.github.jengelman.gradle.plugins:shadow:1.1.2'
            }
        }
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.jar'
        project.jruby.defaultRepositories = false

        project.repositories {
            flatDir dirs : TESTREPO_LOCATION.absolutePath
        }

        if(withShadow) {
            project.apply plugin: 'com.github.johnrengelman.shadow'
        }

        return project
    }

    void setup() {

        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()

        project = setupProject(false)
        jarTask = project.task('JarJar', type: Jar)
    }

    def "Checking configurations exist"() {
        given:
            def cfg = project.configurations

        expect:
            cfg.getByName('jrubyEmbeds')
            cfg.getByName('jrubyJar')
    }

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
                jruby {
                    defaultMainClass()
                }
            }

        then: "Then the attribute should be set to the default in the manifest"
            jarTask.manifest.attributes.'Main-Class' == JRubyJarConfigurator.DEFAULT_BOOTSTRAP_CLASS

        and: "Then jruby-complete should be added as a dependency"
            project.configurations.getByName('compile').dependencies.matching { Dependency d -> d.name == 'jruby-complete' }
    }

    def "Adding all defaults"() {
        given: "Given some files in a gem location or which some should be excluded"
            project.jruby.gemInstallDir = TESTROOT.absolutePath
            new File(TESTROOT,'gems').mkdirs()
            new File(TESTROOT,'data').mkdirs()
            new File(TESTROOT,'cache').mkdirs()
            new File(TESTROOT,'gems/fake.txt').text = 'fake.content'
            new File(TESTROOT,'data/data.txt').text = 'data.content'

        when: "Setting a default main class and default gems via the 'defaults' method"
            project.configure(jarTask) {
                jruby {
                    defaults 'gems','mainClass'
                }
            }

        then: "The appropriate files included"
            fileNames(jarTask.source) == (['MANIFEST.MF','gems','gems/fake.txt','data','data/data.txt'] as Set<String>)

        and: "Then the attribute should be set to the default in the manifest"
            jarTask.manifest.attributes.'Main-Class' == JRubyJarConfigurator.DEFAULT_BOOTSTRAP_CLASS

    }

    def "Adding a custom main class"() {
        when: "Setting a default main class"
            project.configure(jarTask) {
                jruby {
                    mainClass 'org.scooby.doo.snackMain'
                }
            }

        then: "Then the attribute should be set accordingly in the manifest"
            jarTask.manifest.attributes.'Main-Class' == 'org.scooby.doo.snackMain'
    }

    def "Building a Jar and 'java' plugin is applied"() {
        given: "Java plugin applied before JRuby Jar plugin"
            project= setupProject(false)
            project.apply plugin : 'java'
            Task jar = project.tasks.getByName('jar')

        and: "A local repository"
            final String jrubyTestVersion = '1.7.15'
            File expectedDir= new File(TESTROOT,'libs/')
            expectedDir.mkdirs()
            File expectedJar= new File(expectedDir,'test.jar')
            project.jruby.gemInstallDir = new File(TESTROOT,'fakeGemDir').absolutePath

            new File(project.jruby.gemInstallDir,'gems').mkdirs()
            new File(project.jruby.gemInstallDir,'gems/fake.txt').text = 'fake.content'

            project.with {
                jruby {
                    defaultRepositories = false
                    defaultVersion = jrubyTestVersion
                }
                dependencies {
                    jrubyJar 'org.spockframework:spock-core:0.7-groovy-2.0'
                }
            }

        when: "I set the default main class"
            project.configure(jar) {
                archiveName = 'test.jar'
                destinationDir = expectedDir
                jruby {
                    defaults 'gems'
                    mainClass 'bogus.does.not.exist'
                }

            }
            project.evaluate()

        and: "I actually build the JAR"
            jar.copy()
            def builtJar = fileNames(project.zipTree(expectedJar))

        then: "I don't want to see jruby-complete unpacked"
            !builtJar.contains("META-INF/jruby.home/lib/ruby".toString())

        and: "I expect the new main class to be listed in the manifest"
            jar.manifest.effectiveManifest.attributes['Main-Class']?.contains('bogus.does.not.exist')

    }


    def "Setting up a java project"() {
        given: "All jar, java & shadowJar plugins have been applied"
            project = setupProject(true)
            project.apply plugin : 'java'
            Task jar = project.tasks.getByName('jar')
            Task shadowJar = project.tasks.getByName('shadowJar')
            Task compileJava = project.tasks.getByName('compileJava')

        expect:
            compileJava.taskDependencies.getDependencies(compileJava).
                contains(project.tasks.getByName(JRubyJarPlugin.BOOTSTRAP_TASK_NAME))
            jar.taskDependencies.getDependencies(jar).
                    contains(project.tasks.getByName('jrubyPrepareGems'))
            shadowJar.taskDependencies.getDependencies(shadowJar).
                    contains(project.tasks.getByName('jrubyPrepareGems'))
    }

    def "Building a ShadowJar with a custom configuration and 'java' plugin is applied"() {
        given: "Java plugin applied before JRuby Jar plugin"
            project = setupProject(true)
            project.apply plugin : 'java'
            Task jar = project.tasks.getByName('shadowJar')
            JavaCompile compile = project.tasks.getByName('compileJava') as JavaCompile

        and: "A local repository"
            final String jrubyTestVersion = '1.7.15'
            File expectedDir= new File(TESTROOT,'libs/')
            expectedDir.mkdirs()
            File expectedJar= new File(expectedDir,'test-all.jar')
            project.jruby.gemInstallDir = new File(TESTROOT,'fakeGemDir').absolutePath

            new File(project.jruby.gemInstallDir,'gems').mkdirs()
            new File(project.jruby.gemInstallDir,'gems/fake.txt').text = 'fake.content'

            project.with {
                jruby {
                    defaultRepositories = false
                    defaultVersion = jrubyTestVersion
                }
                dependencies {
                    jrubyJar 'org.spockframework:spock-core:0.7-groovy-2.0'
                }
            }

        when: "I set the default main class"
            project.configure(jar) {
                archiveName = 'test-all.jar'
                destinationDir = expectedDir
                jruby {
                    defaults 'gems'
                    mainClass 'bogus.does.not.exist'
                }

            }
            project.evaluate()

        and: "I actually build the JAR"
            jar.copy()
            def builtJar = fileNames(project.zipTree(expectedJar))

        then: "I expect to see jruby.home unpacked "
            builtJar.contains("META-INF/jruby.home/lib/ruby".toString())

        and: "To see my fake files in the 'gems' folder"
            builtJar.contains("gems/fake.txt".toString())

        and: "I expect the new main class to be listed in the manifest"
            jar.manifest.effectiveManifest.attributes['Main-Class']?.contains('bogus.does.not.exist')

    }

    def "Check code generation configuration"() {
        given:
            project= setupProject(false)
            Task generator = project.tasks.getByName(JRubyJarPlugin.BOOTSTRAP_TASK_NAME)

        expect:
            generator.jruby.getSource().toString().endsWith(BootstrapClassExtension.BOOTSTRAP_TEMPLATE_PATH)
            generator.destinationDir == new File(project.buildDir,'generated/java')
    }

    def "Run code generation"() {
        given: "That the jar plugin has been applied"
            project= setupProject(false)
            Task generator = project.tasks.getByName(JRubyJarPlugin.BOOTSTRAP_TASK_NAME)
            File expectedFile = new File(generator.destinationDir,'bootstrap.java')

        when: "The task is executed"
            project.evaluate()
            generator.copy()

        then: "Expect to find the generated file"
            expectedFile.exists()

        and: "The tokens has been replaced"
            !expectedFile.text.contains('%%LAUNCH_SCRIPT%%')
            expectedFile.text.contains(generator.jruby.initScript)
    }
}
