package com.github.jrubygradle

import com.github.jrubygradle.testhelper.IntegrationSpecification
import org.gradle.testkit.runner.BuildResult

/**
 * @author Schalk W. Cronjé
 */
class JRubyExecExtensionIntegrationSpec extends IntegrationSpecification {

    static final String DEFAULT_TASK_NAME = 'inlineJRubyExec'
    static final String BCPROV_NAME = 'bcprov-jdk15on'

    void "Run a script with minimum parameters"() {
        setup:
        useScript(HELLO_WORLD, 'src')
        createJRubyExecProject """
            script 'src/${HELLO_WORLD}'
        """

        when: "I call jrubyexec with only a script name"
        BuildResult result = build()

        then: "I expect the Ruby script to be executed"
        result.output =~ /Hello, World/
    }

    void "Run an inline script"() {
        setup:
        createJRubyExecProject """
            jrubyArgs '-e', "puts 'Hello, World'"
        """

        when: "I call jrubyexec with only script text"
        BuildResult result = build()

        then: "I expect the Ruby script to be executed"
        result.output =~ /Hello, World/
    }

    void "Run a script with no subpath and arguments"() {
        useScript(HELLO_NAME)
        createJRubyExecProject """
            script '${HELLO_NAME}'
            scriptArgs 'Stan'
        """

        when:
        BuildResult result = build()

        then: "only the appropriate parameters should be passed"
        result.output =~ /Hello, Stan/
    }

    void "Running a script that requires a jar"() {
        setup:
        def leadingDir = '.gems/jars/org/bouncycastle/'
        def artifactPath = "${BCPROV_NAME}/${bcprovVer}/${BCPROV_NAME}-${bcprovVer}"
        def withPattern = ~/.*\["file:.+${leadingDir}${artifactPath}\.jar"\].*/

        def jarToUse = findDependency('org.bouncycastle', BCPROV_NAME, 'jar')
        createJRubyExecProject withJarToUse(jarToUse), '''
            jrubyArgs '-e'
            jrubyArgs 'print $CLASSPATH'
        '''

        when:
        BuildResult result = build()

        then:
        result.output.readLines().any { it.matches withPattern }
    }

    void "Running a script that requires a gem, a separate jRuby and a separate configuration"() {
        setup:
        useScript(REQUIRES_GEM)
        createJRubyExecProject withCreditCardValidator(), """
            script '${REQUIRES_GEM}'
            jrubyArgs '-T1'
        """

        when:
        BuildResult result = build()

        then:
        result.output =~ /Not valid/
    }

    void "Running a script that requires environment variables"() {
        // This tests that the passthrough invocation
        // happens for overloaded versions of environment
        // and that the environment variables are passed
        // on to the script
        setup:
        final String envVarName = 'TEST_ENV_VAR'
        final String envVarValue = 'Test Value'

        useScript(ENV_VARS)
        createJRubyExecProject """
            environment    '${envVarName}', '${envVarValue}'
            environment    TEST_A: 'A123', TEST_B: 'B123'
            script         '${ENV_VARS}'
        """

        when:
        BuildResult result = build()
        String outputBuffer = result.output

        then:
        outputBuffer =~ /TEST_ENV_VAR=Test Value/
        outputBuffer =~ /TEST_A=A123/
        outputBuffer =~ /TEST_B=B123/
    }

    private BuildResult build() {
        gradleRunner(DEFAULT_TASK_NAME, '-i', '-s').build()
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    private void createJRubyExecProject(String jrubyexecConfig) {
        createJRubyExecProject('', jrubyexecConfig)
    }

    @SuppressWarnings('BuilderMethodWithSideEffects')
    private void createJRubyExecProject(String preamble, String jrubyexecConfig) {
        buildFile.text = """
        ${projectWithLocalRepo}

        ${preamble}

        task ${DEFAULT_TASK_NAME} {
            doLast {
                jrubyexec {
                    ${jrubyexecConfig}
                }
            }

            dependsOn jrubyPrepare
        }
        """
    }

    private String withJarToUse(String jarFormat) {
        """
            dependencies {
                gems ${jarFormat}
            }
        """
    }

    private String withCreditCardValidator() {
        withJarToUse(findDependency('', 'credit_card_validator', 'gem'))
    }

    private String getBcprovVer() {
        testProperties.bcprovVersion
    }
}