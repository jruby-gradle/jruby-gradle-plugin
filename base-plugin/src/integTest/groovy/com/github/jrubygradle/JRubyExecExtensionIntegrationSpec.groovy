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

/*
class JRubyExecDelegateSpec extends Specification {
    static final String ABS_FILE_PREFIX = System.getProperty('os.name').toLowerCase().startsWith('windows') ? 'C:' : ''

    Project project
    JRubyExecDelegate jred

    void setup() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.github.jruby-gradle.base'
        jred = JRubyExecDelegate.createJRubyExecDelegate(project)
    }

    void "When just passing script, scriptArgs, jrubyArgs, expect local properties to be updated"() {
        given:
        def xplatformFileName = project.file('path/to/file')
        xplatformFileName.parentFile.mkdirs()
        xplatformFileName.text = ''
        def cl = {
            script 'path/to/file'
            jrubyArgs 'c', 'd', '-S'
            scriptArgs '-x'
            scriptArgs '-y', '-z'
            jrubyArgs 'a', 'b'
            gemWorkDir 'path/to/file'
        }
        jred.configure(cl)

        expect:
        jred.passthrough.size() == 0
        jred.script == xplatformFileName
        jred._convertScriptArgs() == ['-x', '-y', '-z']
        jred._convertJrubyArgs() == ['c', 'd', '-S', 'a', 'b']
        jred.buildArgs() == ['-rjars/setup', 'c', 'd', '-S', 'a', 'b', xplatformFileName.toString(), '-x', '-y', '-z']
        jred._convertGemWorkDir(project) == project.file('path/to/file')
    }

    void "When passing absolute file and absolute file, expect check for existence to be executed"() {
        given:
        def cl = {
            script ABS_FILE_PREFIX + '/path/to/file'
            jrubyArgs 'c', 'd', '-S'
            scriptArgs '-x'
            scriptArgs '-y', '-z'
            jrubyArgs 'a', 'b'
        }
        cl.delegate = jred
        cl.call()
        when:
        jred.buildArgs()

        then:
        thrown(InvalidUserDataException)
    }

    void "When just passing arbitrary javaexec, expect them to be stored"() {
        given:
        def cl = {
            environment 'XYZ', '123'
            executable '/path/to/file'
            jvmArgs '-x'
            jvmArgs '-y', '-z'
        }
        cl.delegate = jred
        cl.call()

        expect:
        jred.valuesAt(0) == ['XYZ', '123']
        jred.valuesAt(1) == '/path/to/file'
        jred.valuesAt(2) == '-x'
        jred.valuesAt(3) == ['-y', '-z']
        jred.keyAt(0) == 'environment'
        jred.keyAt(1) == 'executable'
        jred.keyAt(2) == 'jvmArgs'
        jred.keyAt(3) == 'jvmArgs'

    }

    void "When using a conditional, expect specific calls to be passed"() {
        given:
        def cl = {
            if (condition == 1) {
                jvmArgs '-x'
            } else {
                jvmArgs '-y'
            }
        }
        cl.delegate = jred
        cl.call()

        expect:
        jred.valuesAt(0) == parameter

        where:
        condition | parameter
        1         | '-x'
        5         | '-y'

    }

    void "Prevent main from being called"() {
        when:
        def cl = {
            main 'some.class'
        }
        cl.delegate = jred
        cl.call()

        then:
        thrown(UnsupportedOperationException)
    }

    void "Prevent args from being called"() {
        when:
        def cl = {
            args '-x', '-y'
        }
        cl.delegate = jred
        cl.call()

        then:
        thrown(UnsupportedOperationException)
    }

    void "Prevent setArgs from being called"() {
        when:
        def cl = {
            setArgs '-x', '-y'
        }
        cl.delegate = jred
        cl.call()

        then:
        thrown(UnsupportedOperationException)
    }

}

 */