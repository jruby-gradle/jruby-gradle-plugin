package com.github.jrubygradle

import com.github.jrubygradle.testhelper.IntegrationSpecification
import org.gradle.testkit.runner.BuildResult
import spock.lang.IgnoreIf
import spock.lang.Issue
import spock.lang.PendingFeature

import static com.github.jrubygradle.internal.JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG

class JRubyExecIntegrationSpec extends IntegrationSpecification {
    static final String DEFAULT_TASK_NAME = 'RubyWax'
    static final String DEFAULT_JRUBYEXEC_CONFIG = JRubyPlugin.DEFAULT_CONFIGURATION

    String jrubyExecConfig
    String preamble
    String dependenciesConfig
    String additionalContent
    String jrubyScript

    void "Changing the jruby version will load the correct jruby"() {
        setup: "Version is set on the task"
        final String configName = 'integExecConfig'
        final String altVersion = olderJRubyVersion

        withPreamble """
            configurations {
                ${configName}
            }
        """

        withDependencies """
            ${configName} 'org.jruby:jruby-complete:${altVersion}'
        """

        withJRubyExecConfig """
            configuration '${configName}'
            jruby.jrubyVersion '${altVersion}'
        """

        withAdditionalContent """
            task validateVersion {
                doLast {
                    assert jruby.jrubyVersion != '${altVersion}'
                }
            }
            
            task printConfiguration {
                doLast {
                    configurations.${configName}.files.each {
                        println it.name
                    }
                }
            }
        """

        when:
        BuildResult result = build('validateVersion', 'printConfiguration')

        then:
        "jruby-complete-${altVersion}.jar must be selected"
        result.output.contains("${altVersion}.jar")
    }

    void "Running a script that requires a gem, a separate JRuby and a separate configuration"() {
        setup:
        final String altVersion = olderJRubyVersion
        final String altConfiguration = 'waxOnWaxOff'

        useScript(REQUIRES_GEM)
        withPreamble """
            configurations {
                ${altConfiguration}
            }
        """
        withDependencies "${altConfiguration} ${withCreditCardValidator()}"
        withJRubyExecConfig """
            jruby.jrubyVersion  '${altVersion}'
            jruby.gemConfiguration '${altConfiguration}'            
        """

        when:
        BuildResult result = build()

        then:
        result.output =~ /Not valid/
    }

    void "Running a Hello World script"() {
        setup:
        useScript(HELLO_WORLD)

        when:
        BuildResult result = build()

        then:
        result.output =~ /Hello, World/
    }

    void "Running a script that requires a gem"() {
        setup:
        useScript(REQUIRES_GEM)
        withJRubyExecConfig 'setEnvironment [:]'
        withDependencies "${DEFAULT_JRUBYEXEC_CONFIG} ${withCreditCardValidator()}"

        when:
        BuildResult result = build()

        then:
        result.output =~ /Not valid/
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/77')
    void "Running rspec from a script should not cause a gemWorkDir failure"() {
        setup:
        withPreamble "jruby.jrubyVersion '${olderJRubyVersion}'"

        withDependencies """
            ${DEFAULT_JRUBYEXEC_CONFIG} ${findDependency('', 'rspec', 'gem')}
            ${DEFAULT_JRUBYEXEC_CONFIG} ${findDependency('', 'rspec-core', 'gem')}
            ${DEFAULT_JRUBYEXEC_CONFIG} ${findDependency('', 'rspec-support', 'gem')}
        """

        withJRubyExecConfig """
            group 'JRuby'
            description 'Execute the RSpecs in JRuby'
            jrubyArgs '-S'
            script 'rspec'
        """

        when:
        BuildResult result = build()

        then:
        noExceptionThrown()
        result.output =~ /No examples found./
    }

    @Override
    void useScript(final String name, final String relativePath = null) {
        super.useScript(name, relativePath)
        jrubyScript = "script '${relativePath ? relativePath + '/' : ''}${name}'"
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

    private void withJRubyExecConfig(String jrubyExecConfig) {
        this.jrubyExecConfig = """
        task ${DEFAULT_TASK_NAME}( type: JRubyExec ) {
            ${jrubyExecConfig}    
        }
        """
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

    private void withAdditionalContent(String content) {
        additionalContent = content
    }

    private void writeBuildFile() {

        String jrubyExec

        if (jrubyScript && jrubyExecConfig) {
            jrubyExec = """
            ${jrubyExecConfig}

            ${DEFAULT_TASK_NAME} {
                ${jrubyScript}
            }
            """
        } else if (jrubyExecConfig) {
            jrubyExec = jrubyExecConfig
        } else if (jrubyScript) {
            withJRubyExecConfig(jrubyScript)
            jrubyExec = jrubyExecConfig
        }

        buildFile.text = """
        import com.github.jrubygradle.JRubyExec

        ${projectWithMavenRepo}

        ${preamble ?: ''}

        ${dependenciesConfig ?: ''}
        
        ${jrubyExec ?: ''}    
        
        ${additionalContent ?: ''}
        """
    }

    private String getOlderJRubyVersion() {
        testProperties.olderJRubyVersion
    }

    private String withCreditCardValidator() {
        findDependency('', 'credit_card_validator', 'gem')
    }

}
