package com.github.jrubygradle

import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.bundling.Jar
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.IgnoreRest
import spock.lang.Specification

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author R. Tyler Croy
 * @author Schalk W. Cronjé
 *
 */
class JRubyJarPluginSpec extends Specification {
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}/jrjps")
    static final File WARBLER_LOCATION = new File("${System.getProperty('WARBLER_LOCATION') ?: 'build/tmp/test/repo'}")
    static final String TASK_NAME = 'JarJar'

    def project
    def jarTask

    static Set<String> fileNames(FileCollection fc) {
        Set<String> names = []
        fc.asFileTree.visit { fvd ->
            names.add(fvd.relativePath.toString())
        }
        return names
    }

    void setup() {

        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()

        project = ProjectBuilder.builder().build()
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.jar'
        jarTask = project.task('JarJar', type: Jar)
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
            jarTask.manifest.attributes.'Main-Class' == JRubyJarConfigurator.DEFAULT_MAIN_CLASS
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

        then: "Then the attribute should be set to the default in the manifest"
            jarTask.manifest.attributes.'Main-Class' == JRubyJarConfigurator.DEFAULT_MAIN_CLASS

        and: "The appropriate files included"
            fileNames(jarTask.source) == (['MANIFEST.MF','gems','gems/fake.txt','data','data/data.txt'] as Set<String>)
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

    def "Building a Jar"() {
        given: "A local repository"
            File expectedDir= new File(TESTROOT,'libs/')
            expectedDir.mkdirs()
            File expectedJar= new File(expectedDir,'test.jar')
            project.jruby.gemInstallDir = new File(TESTROOT,'fakeGemDir').absolutePath
            new File(project.jruby.gemInstallDir,'gems').mkdirs()
            new File(project.jruby.gemInstallDir,'gems/fake.txt').text = 'fake.content'

            project.with {
                jruby {
                    defaultRepositories = false
                }
                repositories {
                    ivy {
                        url  WARBLER_LOCATION
                        layout('pattern') {
                            artifact '[module]-[revision](.[ext])'
                        }
                    }
                }
            }

        when: "I set the default main class"
            project.configure(jarTask) {
                archiveName = 'test.jar'
                destinationDir = expectedDir
                jruby {
                    defaults 'mainClass','gems'
                }

            }
            project.evaluate()

        and: "I actually build the JAR"
            jarTask.copy()

        then: "I expect to see the JarMain.class embedded in the JAR"
            expectedJar.exists()
            fileNames(project.zipTree(expectedJar)).contains('com/lookout/jruby/JarMain.class')
            !fileNames(project.zipTree(expectedJar)).contains('com/lookout/jruby/WarMain.class')
    }
}
