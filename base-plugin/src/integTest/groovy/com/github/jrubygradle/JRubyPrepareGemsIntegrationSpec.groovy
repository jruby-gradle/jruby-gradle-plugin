package com.github.jrubygradle

import com.github.jrubygradle.testhelper.IntegrationSpecification
import org.gradle.testkit.runner.BuildResult
import spock.lang.IgnoreIf
import spock.lang.Issue

/**
 * @author Schalk W. Cronjé.
 */
class JRubyPrepareGemsIntegrationSpec extends IntegrationSpecification {

    static final String DEFAULT_TASK_NAME = 'jrubyPrepare'

    String repoSetup = projectWithLocalRepo
    String preamble
    String dependenciesConfig

    void "Check that default 'jrubyPrepareGems' uses the correct directory"() {
        setup:
        withDependencies "gems ${slimGem}"
        withPreamble """
            jruby.gemInstallDir = '${projectDir.absolutePath}'
        """

        when:
        build()

        then:
        new File(projectDir, "gems/slim-${slimVersion}").exists()
    }

    @IgnoreIf({ IntegrationSpecification.OFFLINE })
    void "Check if rack version gets resolved"() {
        setup:
        withDefaultRepositories()
        withPreamble """
            jruby.gemInstallDir = '${projectDir.absolutePath}'
        """
        withDependencies """
            gems "rubygems:sinatra:1.4.5"
            gems "rubygems:rack:[0,)"
            gems "rubygems:lookout-rack-utils:3.1.0.12"
        """

        when:
        build()

        then:
        // since we need a version range in the setup the
        // resolved version here can vary over time
        new File(projectDir, "gems/rack-1.5.5").exists()
    }

    @IgnoreIf({ IntegrationSpecification.OFFLINE })
    void "Check if prerelease gem gets resolved"() {
        setup:
        withDefaultRepositories()
        withPreamble """
            jruby.gemInstallDir = '${projectDir.absolutePath}'
        """
        withDependencies 'gems "rubygems:jar-dependencies:0.1.16.pre"'

        when:
        build()

        then:
        new File(projectDir, "gems/jar-dependencies-0.1.16.pre").exists()
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/341')
    @IgnoreIf({ IntegrationSpecification.OFFLINE })
    void "Make a install-time gem dependency available"() {
        setup:
        withDefaultRepositories()
        withPreamble """
            jruby.gemInstallDir = '${projectDir.absolutePath}'
        """
        withDependencies 'gems "rubygems:childprocess:1.0.1"'

        when:
        build()

        then:
        new File(projectDir, "gems/childprocess-1.0.1").exists()
    }

    private void withDefaultRepositories() {
        repoSetup = projectWithDefaultAndMavenRepo
    }

    private void withDependencies(String deps) {
        this.dependenciesConfig = """
        dependencies {
            ${deps}
        }
        """
    }

    private void withPreamble(String content) {
        preamble = content
    }

    private void writeBuildFile() {
        buildFile.text = """
        ${repoSetup}

        ${preamble ?: ''}

        ${dependenciesConfig ?: ''}
        """
    }

    private String getSlimVersion() {
        testProperties.slimVersion
    }

    private String getSlimGem() {
        "'rubygems:slim:${slimVersion}@gem'"
    }

    private BuildResult build() {
        build(DEFAULT_TASK_NAME)
    }

    private BuildResult build(String taskName, String... moreTasks) {
        List<String> tasks = [taskName]
        tasks.addAll(moreTasks)
        tasks.add('-i')
        tasks.add('-s')
        writeBuildFile()
        gradleRunner(tasks).build()
    }
}
