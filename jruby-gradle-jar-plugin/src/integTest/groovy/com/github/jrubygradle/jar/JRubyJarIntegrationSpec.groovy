package com.github.jrubygradle.jar

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

import spock.lang.*

@Ignore('raises a groovy version conflict at runtime')
class JRubyJarIntegrationSpec extends Specification {
    @Rule
    final TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    String pluginDependencies

    def setup() {
        buildFile = testProjectDir.newFile('build.gradle')
        def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.json")

        if (pluginClasspathResource == null) {
            throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
        }

        pluginDependencies = pluginClasspathResource.text
    }

    def "executing the jrubyJar default task produces a jar artifact"() {
        given:
        buildFile << """
buildscript {
    dependencies {
        classpath files(${pluginDependencies})
    }
}
apply plugin: 'com.github.jruby-gradle.jar'

repositories { jcenter() }

jrubyJar {
}
    """

        when:
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withArguments('jrubyJar')
                .build()

        then:
        File[] artifacts = (new File(testProjectDir.root, ['build', 'libs'].join(File.separator))).listFiles()
        artifacts && artifacts.size() == 1
        result.task(":jrubyJar").outcome == TaskOutcome.SUCCESS
    }
}