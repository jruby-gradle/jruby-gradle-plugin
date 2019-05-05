package com.github.jrubygradle.jar

import com.github.jrubygradle.jar.helpers.IntegrationSpecification
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.IgnoreIf
import spock.lang.Issue

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class JRubyJarTestKitSpec extends IntegrationSpecification {

    public static final String DEFAULT_TASK_NAME = 'jrubyJar'

    File rubyFile
    String jrubyJarConfig
    String repoSetup
    String deps
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

    void "executing the jrubyJar task produces an executable artifact"() {
        given:
        withJRubyConfig """
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
        ZipFile zip = new ZipFile("${projectDir}/build/libs/testproject-jruby.jar")
        !zip.entries().findAll { ZipEntry entry ->
            entry.name.matches(/(.*)jruby-complete-(.*).jar/) || entry.name.matches(/(.*)jruby-mains-(.*).jar/)
        }

        and:
        result.output.contains('Hello from JRuby')
    }

    @Issue("https://github.com/jruby-gradle/jruby-gradle-plugin/issues/183")
    void "creating a new task based on JRubyJar produces a jar artifact"() {
        given:
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

    @IgnoreIf({ IntegrationSpecification.OFFLINE })
    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/pull/271')
    def 'using a more recent jar-dependencies should work'() {
        given:
        withRepoSetup """
            maven { url 'http://rubygems-proxy.torquebox.org/releases' }
            mavenCentral()
        """
        withDependencies "jrubyJar 'rubygems:jar-dependencies:0.2.3'"
        withJRubyConfig "initScript 'main.rb'"

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
        result.task(":validateJar").outcome == TaskOutcome.SUCCESS
        result.output.contains("Hello from JRuby")
    }

    private void withJRubyConfig(String content) {
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
                repositories.flatDir dirs: '${flatRepoLocation.absolutePath}'
                repositories.maven { url 'file://${mavenRepoLocation.absolutePath}' } 
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

    private void writeBuildFile() {
        buildFile.text = """
        import com.github.jrubygradle.jar.JRubyJar

        plugins {
            id 'com.github.jruby-gradle.jar'
        }
        
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
        List<String> tasks = ['-i', taskName]
        tasks.addAll(additionalTasks)
        gradleRunner(tasks).build()
    }
}
