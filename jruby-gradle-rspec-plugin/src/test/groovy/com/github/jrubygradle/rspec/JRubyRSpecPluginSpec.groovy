package com.github.jrubygradle.rspec

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import org.gradle.process.internal.ExecException

import java.nio.file.Files

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author Christian Meier
 *
 */
class JRubyRSpecPluginSpec extends Specification {
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}")
    static final File TESTREPO_LOCATION = new File("${System.getProperty('TESTREPO_LOCATION') ?: 'build/tmp/test/repo'}")
    //static final String jrubyTestVersion = '1.7.21'

    def project
    def specDir

    static String captureStdout( Closure closure ){
        OutputStream output = new ByteArrayOutputStream()
        PrintStream out = System.out
        try {
          System.out = new PrintStream(output)
          closure.call()
        }
        finally {
          System.out = out
        }
        output.toString()
    }

    static Set<String> fileNames(FileCollection fc) {
        Set<String> names = []
        fc.asFileTree.visit { fvd ->
            names.add(fvd.relativePath.toString())
        }
        return names
    }

    static Project setupProject() {
        Project project = ProjectBuilder.builder().build()

        //project.gradle.startParameter.offline = true

        project.buildscript {
            repositories {
              flatDir dirs : TESTREPO_LOCATION.absolutePath
            }
        }
        project.buildDir = TESTROOT
        //project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.rspec'
        //project.jruby.defaultRepositories = false
        project.repositories {
            flatDir dirs : TESTREPO_LOCATION.absolutePath
        }

        return project
    }

    void setup() {

        if(TESTROOT.exists()) {
          //  TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()

        project = setupProject()
        specDir = new File(project.projectDir, 'spec').getAbsoluteFile()
    }

    def 'Checking tasks exist'() {
        expect:
            project.tasks.getByName('rspec')
    }

    def "Checking configurations exist"() {
        given:
            def cfg = project.configurations

        expect:
            cfg.getByName('rspec')
    }

    def "Checking jruby-complete jar is configured"() {
        given:
            project.evaluate()
            def cfg = project.configurations.getByName('rspec')

        expect:
            cfg.files.find { it.name.startsWith('jruby-complete-') }
            cfg.files.find { it.name.startsWith('rspec-') }
    }

    def "Run rspec with defaults and not specs"() {
        given:
            project.evaluate()
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
        expect:
            output.contains( 'No examples found.' )
    }

    def "Throw exception on test failure"() {
        when:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/failing/spec').getAbsoluteFile().toPath())
            project.evaluate()
            project.tasks.getByName('rspec').run()
        then:
            thrown(ExecException)
    }

    def "Run rspec"() {
        given:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/simple/spec').getAbsoluteFile().toPath())
            project.evaluate()
            project.tasks.getByName('rspec').run()
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
            println output
        expect:
            output.contains( '3 examples, 0 failures' )
    }

    def "Run rspec tasks separated"() {
        given:
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/simple/spec').getAbsoluteFile().toPath())
            project.dependencies {
               rspec 'rubygems:leafy-metrics:0.6.0'
               rspec 'org.slf4j:slf4j-simple:1.6.4'
            }
            Task task = project.tasks.create( 'mine', RSpec)
            project.evaluate()
            String outputMine = captureStdout {
                task.run()
            }
            println outputMine

            specDir.delete()
            Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/more/spec').getAbsoluteFile().toPath())
            String output = captureStdout {
                project.tasks.getByName('rspec').run()
            }
            println output
        expect:
            outputMine.contains( '3 examples, 0 failures' )
            output.contains( '2 examples, 0 failures' )
    }

    // def "Run rspec tasks separated reversed"() {
    //     given:
    //         Files.createSymbolicLink(specDir.toPath(), new File('src/test/resources/more/spec').getAbsoluteFile().toPath())
    //         project.dependencies {
    //            rspec 'rubygems:leafy-metrics:0.6.0'
    //            rspec 'org.slf4j:slf4j-simple:1.6.4'
    //         }
    //         project.evaluate()
    //         String output = captureStdout {
    //             project.tasks.getByName('rspec').run()
    //         }
    //         println output
    //     expect:
    //         output.contains( '3 examples, 0 failures' )
    // }

}
