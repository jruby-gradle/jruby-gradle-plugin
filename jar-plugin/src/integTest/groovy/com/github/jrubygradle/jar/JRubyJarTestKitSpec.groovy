/*
 * Copyright (c) 2014-2020, R. Tyler Croy <rtyler@brokenco.de>,
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

import com.github.jrubygradle.jar.helpers.IntegrationSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.Issue

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static com.github.jrubygradle.jar.JRubyJar.DEFAULT_MAIN_CLASS

@Ignore
class JRubyJarTestKitSpec extends IntegrationSpecification {

    public static final String DEFAULT_TASK_NAME = 'jrubyJar'

    File rubyFile
    String jrubyJarConfig
    String repoSetup
    String deps
    String preamble
    String additionalContent

    def setup() {
        rubyFile = new File(projectDir, 'main.rb')
        writeRubyStubFile()
        withLocalRepositories()
    }

    void "executing the jrubyJar default task produces a jar artifact"() {
        when:
        BuildResult result = build()

        then:
        result.task(":jrubyJar").outcome == TaskOutcome.SUCCESS
        new File(projectDir, "build/libs/testproject-jruby.jar").exists()
    }

    @SuppressWarnings('GStringExpressionWithinString')
    void "executing the jrubyJar task produces an executable artifact"() {
        setup:
        withJRubyJarConfig """
            initScript 'main.rb'
        """

        withAdditionalContent '''
            task validateJar(type: JavaExec) {
                main = '-jar'
                dependsOn jrubyJar
                environment [:]
                workingDir "${buildDir}/libs"
                args jrubyJar.outputs.files.singleFile.absolutePath
            }
        '''

        when:
        BuildResult result = build('validateJar')

        then:
        result.task(":jrubyJar").outcome == TaskOutcome.SUCCESS
        result.task(":validateJar").outcome == TaskOutcome.SUCCESS

        and: "the should not be a jruby-mains.jar or jruby-complete.jar inside the archive"
        !jarEntries.findAll { ZipEntry entry ->
            entry.name.matches(/(.*)jruby-complete-(.*).jar/) || entry.name.matches(/(.*)jruby-mains-(.*).jar/)
        }

        and:
        result.output.contains('Hello from JRuby')
    }

    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/183")
    @SuppressWarnings('GStringExpressionWithinString')
    void "creating a new task based on JRubyJar produces a jar artifact"() {
        setup:
        withAdditionalContent '''
            task someDifferentJar(type: JRubyJar) { 
                initScript 'main.rb' 
            }
            
            task validateJar(type: JavaExec) {
                dependsOn someDifferentJar
                environment [:]
                workingDir "${buildDir}/libs"
                main = '-jar'
                args someDifferentJar.outputs.files.singleFile.absolutePath
            }
        '''

        when:
        BuildResult result = build('validateJar')

        then:
        result.task(":validateJar").outcome == TaskOutcome.SUCCESS
        result.output.contains("Hello from JRuby")
    }

    void "Building a Jar with a custom configuration and 'java' plugin is applied"() {
        setup:
        withPreamble 'apply plugin: "java"'
        withJRubyJarConfig "mainClass 'bogus.does.not.exist'"

        when:
        build()

        then: "I expect to see jruby.home unpacked inside the jar"
        jarEntries.find { entry ->
            entry.name == 'META-INF/jruby.home/bin/'
        }

        and: "I expect the new main class to be listed in the manifest"
        jarManifestContent.contains 'Main-Class: bogus.does.not.exist'
    }

    void "Adding a default main class"() {
        setup:
        withPreamble 'apply plugin: "java"'
        withJRubyJarConfig "defaultMainClass()"

        when:
        build()

        then: "Then the attribute should be set to the default in the manifest"
        jarManifestContent.contains "Main-Class: ${DEFAULT_MAIN_CLASS}"
    }

    private void withJRubyJarConfig(String content) {
        jrubyJarConfig = """
        jrubyJar {
            ${content}
        }
        """
    }

    private void withAdditionalContent(String content) {
        this.additionalContent = content
    }

    private void withLocalRepositories() {
        this.repoSetup = """
            repositories { 
                flatDir { 
                    dirs '${pathAsUriStr(flatRepoLocation)}' 
                }
                maven { 
                    url '${pathAsUriStr(mavenRepoLocation)}'
                } 
            }
        """
    }

    private void withRepoSetup(String content) {
        this.repoSetup = """
        repositories {
            ${content}
        }
        """
    }

    private void withDependencies(String content) {
        this.deps = """
        dependencies {
            ${content}
        }
        """
    }

    private void withPreamble(String content) {
        this.preamble = content
    }

    private void writeBuildFile() {
        buildFile.text = """
        import com.github.jrubygradle.jar.JRubyJar

        plugins {
            id 'com.github.jruby-gradle.jar'
        }
        
        ${preamble ?: ''}

        jruby.defaultRepositories = false
        
        ${repoSetup ?: ''}

        ${deps ?: ''}

        ${jrubyJarConfig ?: ''}

        ${additionalContent ?: ''}
        """
    }

    private void writeRubyStubFile() {
        rubyFile.text = """
            puts "Hello from JRuby: #{JRUBY_VERSION}"
        """
    }

    private BuildResult build() {
        build(DEFAULT_TASK_NAME)
    }

    private BuildResult build(String taskName, String... additionalTasks) {
        writeBuildFile()
        List<String> tasks = ['-i', '-s', taskName]
        tasks.addAll(additionalTasks)
        gradleRunner(tasks).build()
    }

    @SuppressWarnings('UnnecessaryDefInMethodDeclaration')
    private def getJarEntries() {
        ZipFile zip = new ZipFile("${projectDir}/build/libs/testproject-jruby.jar")
        zip.entries()
    }

    private String getJarManifestContent() {
        ZipFile zip = new ZipFile("${projectDir}/build/libs/testproject-jruby.jar")

        ZipEntry entry = zip.entries().find { entry ->
            entry.name == 'META-INF/MANIFEST.MF'
        }

        zip.getInputStream(entry).text
    }
}
