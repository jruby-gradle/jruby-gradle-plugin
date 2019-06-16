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
            jrubyPrepare.outputDir = '${pathAsUriStr(projectDir)}'.toURI()
        """

        when:
        build()

        then:
        new File(projectDir, "gems/slim-${slimVersion}").exists()
    }

    @IgnoreIf({ IntegrationSpecification.OFFLINE })
    void "Check if rack version gets resolved"() {
        setup:
        withPreamble """repositories.ruby.gems()
            jrubyPrepare.outputDir = '${pathAsUriStr(projectDir)}'.toURI()
        """

        withDependencies """
            gems "rubygems:sinatra:1.4.5"
            gems "rubygems:rack:[0,)"
            gems "rubygems:lookout-rack-utils:5.0.0.49"
        """

        when:
        build()

        then:
        // since we need a version range in the setup the
        // resolved version here can vary over time
        new File(projectDir, "gems/rack-1.6.11").exists()
    }

    @IgnoreIf({ IntegrationSpecification.OFFLINE })
    void "Check that GEM dependencies are locked"() {
        setup:
        File lockFile = new File(projectDir, 'gradle/dependency-locks/gems.lockfile')
        withPreamble """repositories.ruby.gems()
            jrubyPrepare.outputDir = '${pathAsUriStr(projectDir)}'.toURI()

            dependencyLocking {
                lockAllConfigurations()
            }


        """
        withDependencies """
            gems "rubygems:sinatra:1.4.5"
            gems "rubygems:rack:[0,)"
            gems "rubygems:lookout-rack-utils:5.0.0.49"
        """

        lockFile.parentFile.mkdirs()
        lockFile.text = '''
rubygems:concurrent-ruby:1.1.5
rubygems:configatron:4.5.1
rubygems:i18n:1.6.0
rubygems:log4r:1.1.10
rubygems:lookout-rack-utils:5.0.0.49
rubygems:lookout-statsd:3.2.0
rubygems:rack-graphite:1.6.0
rubygems:rack-protection:1.5.5
rubygems:rack:1.6.10
rubygems:sinatra:1.4.5
rubygems:tilt:2.0.9
'''
        when:
        build()

        then:
        // since we need a version range in the setup the
        // resolved version here can vary over time
        new File(projectDir, "gems/rack-1.6.10").exists()
    }

    @IgnoreIf({ IntegrationSpecification.OFFLINE })
    void "Check if prerelease gem gets resolved"() {
        setup:
        withDefaultRepositories()
        withPreamble """
            jrubyPrepare.outputDir = '${pathAsUriStr(projectDir)}'.toURI()
        """
        withDependencies 'gems "rubygems:jar-dependencies:0.1.16.pre"'

        when:
        build()

        then:
        new File(projectDir, "gems/jar-dependencies-0.1.16.pre").exists()
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/341')
    @IgnoreIf({ IntegrationSpecification.OFFLINE })
    void "Make an install-time gem dependency available"() {
        setup:
        withRubyGemsRepository()
        withPreamble """
            jrubyPrepare.outputDir = '${pathAsUriStr(projectDir)}'.toURI()
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

    private void withRubyGemsRepository() {
        repoSetup = projectWithRubyGemsRepo
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
        tasks.add('--refresh-dependencies')
        writeBuildFile()
        gradleRunner(tasks).build()
    }
}
