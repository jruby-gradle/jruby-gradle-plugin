package com.github.jrubygradle.testhelper

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

class IntegrationSpecification extends Specification {

    static final boolean OFFLINE = System.getProperty('TESTS_ARE_OFFLINE')

    static final String HELLO_WORLD = 'helloWorld.rb'
    static final String HELLO_NAME = 'helloName.rb'
    static final String REQUIRES_GEM = 'requiresGem.rb'
    static final String REQUIRE_THE_A_GEM = 'require-a-gem.rb'
    static final String ENV_VARS = 'envVars.rb'

    @Shared
    Map testProperties
    @Shared
    File flatRepoLocation
    @Shared
    File mavenRepoLocation

    @Rule
    TemporaryFolder testFolder

    File projectDir
    File buildFile
    File settingsFile

    void setupSpec() {
        testProperties = loadTestProperties()
        flatRepoLocation = new File(testProperties.flatrepo)
        mavenRepoLocation = new File(testProperties.mavenrepo)
    }

    void setup() {
        projectDir = testFolder.root
        buildFile = new File(projectDir, 'build.gradle')
        settingsFile = new File(projectDir, 'settings.gradle')

        settingsFile.text = ''
    }

    void useScript(final String name, final String relativePath = null) {
        File destination = new File(testFolder.root, relativePath ? "${relativePath}/${name}" : name)
        destination.parentFile.mkdirs()
        destination.text = this.class.getResource("/scripts/${name}").text
    }

    String findDependency(final String organisation, final String artifact, final String extension) {
        "'${VersionFinder.findDependency(flatRepoLocation, organisation, artifact, extension)}'"
    }

    String pathAsUriStr(final File path) {
        path.absoluteFile.toURI().toString()
    }

    String getProjectWithLocalRepo() {
        """
        plugins {
            id 'com.github.jruby-gradle.base'
        }

        jruby.defaultRepositories = false
        repositories {
            flatDir {
                dirs '${pathAsUriStr(flatRepoLocation)}'.toURI()
            }
        }
        """
    }

    String getProjectWithMavenRepo() {
        """
        plugins {
            id 'com.github.jruby-gradle.base'
        }

        jruby.defaultRepositories = false

        repositories { 
            flatDir { 
                dirs '${pathAsUriStr(flatRepoLocation)}'.toURI()
            }
            maven { 
                url '${pathAsUriStr(mavenRepoLocation)}'.toURI()
            } 
        }
        """
    }

    String getProjectWithDefaultAndMavenRepo() {
        """
        plugins {
            id 'com.github.jruby-gradle.base'
        }

        jruby.defaultRepositories = true
        repositories.maven { url '${pathAsUriStr(mavenRepoLocation)}'.toURI() } 
        """
    }

    GradleRunner gradleRunner(List<String> args) {
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withDebug(true)
                .withArguments(args)
                .withPluginClasspath()
                .forwardOutput()
    }

    GradleRunner gradleRunner(String... args) {
        gradleRunner(args as List)
    }

    private Map<String, String> loadTestProperties() {
        this.class.getResource('/jruby-gradle-testconfig.properties').withInputStream { strm ->
            Properties props = new Properties()
            props.load(strm)
            props as Map<String, String>
        }
    }
}