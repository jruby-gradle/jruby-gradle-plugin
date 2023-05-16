/*
 * Copyright (c) 2014-2023, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.jar

import com.github.jrubygradle.JRubyPrepare
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static com.github.jrubygradle.jar.JRubyJar.DEFAULT_MAIN_CLASS

/**
 * @author R. Tyler Croy
 * @author Schalk W. Cronjé
 * @author Christian Meier
 *
 */
class JRubyJarPluginSpec extends Specification {

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

        project.with {
            apply plugin: 'com.github.jruby-gradle.jar'
            jruby.defaultRepositories = false

            repositories {
                flatDir dirs: project.file('fakerepo')
                maven {
                    url "file://" + project.file('fakerepo').absolutePath
                }
            }
        }

        return project
    }

    void setup() {
        project = setupProject()
        jarTask = project.tasks.getByName('jrubyJar')
    }

    void 'Checking tasks exist'() {
        expect:
        project.tasks.getByName('jrubyJar')
    }

    void 'Checking appendix'() {
        expect:
        project.tasks.getByName('jrubyJar').appendix == 'jruby'
    }

    void "Fails on adding non-existing initScript"() {
        given:
        def TESTROOT = new File(project.projectDir, 'fake')
        new File(TESTROOT, 'gems').mkdirs()
        new File(TESTROOT, 'gems/fake.txt').text = 'fake.content'

        when: "Setting a default main class"
        project.configure(jarTask) {
            initScript 'not.existing'
        }
        jarTask.applyConfig()

        then: "Then expecting use error"
        thrown(InvalidUserDataException)
    }

    void "Adding a default extracting main class"() {
        when: "Setting a default extracting main class"
        project.configure(jarTask) {
            extractingMainClass()
        }
        jarTask.applyConfig()

        then: "Then the attribute should be set to the default in the manifest"
        jarTask.manifest.attributes.'Main-Class' == JRubyJar.EXTRACTING_MAIN_CLASS
    }

    void "Adding a custom main class"() {
        when: "Setting a default main class"
        project.configure(jarTask) {
            mainClass 'org.scooby.doo.snackMain'
        }
        jarTask.applyConfig()

        then: "Then the attribute should be set accordingly in the manifest"
        jarTask.manifest.attributes.'Main-Class' == 'org.scooby.doo.snackMain'
    }

    void "Adding a main class and additional manifest attributes"() {
        when: "Setting a main class"
        project.configure(jarTask) {
            mainClass 'org.scooby.doo.snackMain'
            manifest.attributes('Class-Path': 'gangway.jar zoinks.jar')
        }
        jarTask.applyConfig()

        then: "Then the Main-Class attribute does not erase other attributes"
        jarTask.manifest.attributes.'Class-Path' == 'gangway.jar zoinks.jar'
    }

    void "Setting up a java project"() {
        given: "All jar, java plugins have been applied"
        def TESTROOT = new File(project.projectDir, 'TESTROOT')
        project = setupProject()
        project.apply plugin: 'java'
        Task jar = project.tasks.getByName('jrubyJar')

        and: "A local repository"
        File expectedDir = new File(TESTROOT, 'libs/')
        expectedDir.mkdirs()
        project.configure(jar) {
            initScript library()
        }
        jar.applyConfig()

        expect:
        jar.taskDependencies.getDependencies(jar).contains(project.tasks.getByName('prepareJRubyJar'))
    }

    void 'Checking setting no mainClass'() {
        when:
        project.file('app.rb') << ''
        jarTask.initScript('app.rb')
        jarTask.applyConfig()

        then:
        jarTask.manifest.attributes['Main-Class'] == DEFAULT_MAIN_CLASS
    }

    void 'Checking setting of mainClass once'() {
        when:
        project.file('app.rb') << ''
        jarTask.initScript('app.rb')
        jarTask.mainClass('org.example.Main')
        jarTask.applyConfig()

        then:
        jarTask.manifest.attributes['Main-Class'] == 'org.example.Main'
    }

    void 'Checking setup runnable jrubyJar task'() {
        when:
        jarTask.initScript(jarTask.runnable())
        jarTask.applyConfig()

        then:
        jarTask.manifest.attributes.containsKey('Main-Class')
    }

    void 'Checking valid library config'() {
        when:
        jarTask.initScript(jarTask.library())
        jarTask.applyConfig()

        then:
        !jarTask.manifest.attributes.containsKey('Main-Class')
    }

    void 'Checking invalid library config'() {
        when:
        jarTask.initScript(jarTask.library())
        jarTask.extractingMainClass()
        jarTask.applyConfig()

        then:
        Exception e = thrown()
        e.message == 'can not have mainClass for library'
    }

    void "prepareTask should be an instance of JRubyPrepare"() {
        expect:
        jarTask.dependsOn.find { (it instanceof JRubyPrepare) && (it.name == 'prepareJRubyJar') }
    }
}