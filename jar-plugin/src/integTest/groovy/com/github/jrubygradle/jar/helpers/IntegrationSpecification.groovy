package com.github.jrubygradle.jar.helpers

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification


class IntegrationSpecification extends Specification {
    static final boolean OFFLINE = System.getProperty('TESTS_ARE_OFFLINE')

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

        settingsFile.text = 'rootProject.name="testproject"'
    }

    GradleRunner gradleRunner(List<String> args) {
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withDebug(true)
                .withArguments(args)
                .withPluginClasspath()
                .forwardOutput()
    }

    private Map<String, String> loadTestProperties() {
        this.class.getResource('/jruby-gradle-testconfig.properties').withInputStream { strm ->
            Properties props = new Properties()
            props.load(strm)
            props as Map<String, String>
        }
    }
}