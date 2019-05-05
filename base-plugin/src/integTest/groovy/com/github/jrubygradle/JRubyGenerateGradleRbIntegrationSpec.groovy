package com.github.jrubygradle

import com.github.jrubygradle.testhelper.IntegrationSpecification
import org.ysb33r.grolifant.api.OperatingSystem
import spock.lang.IgnoreIf

/**
 * @author Schalk W. Cronjé
 */
class JRubyGenerateGradleRbIntegrationSpec extends IntegrationSpecification {

    static final String DEFAULT_TASK_NAME = 'RubyWax'

    @IgnoreIf({ OperatingSystem.current().isWindows() })
    def "Generate gradle.rb"() {
        given: "A set of gems"
        buildFile.text = """
            import com.github.jrubygradle.GenerateGradleRb
    
            ${projectWithLocalRepo}
    
            task ${DEFAULT_TASK_NAME} (type: GenerateGradleRb)  
        """

        def expected = new File(projectDir, 'gradle.rb')

        when: "The load path file is generated "
        gradleRunner(DEFAULT_TASK_NAME, '-i').build()

        then: "Expect to be in the configured destinationDir and be called gradle.rb"
        expected.exists()

        when:
        String content = expected.text

        then: "The GEM_HOME to include gemInstallDir"
        expected.text.contains "export GEM_HOME=\"${new File(projectDir, 'build/gems').absolutePath}"

        and: "The JARS_HOME is set"
        expected.text.contains('export JARS_HOME=')

        and: "The java command invoked with the -cp flag"
        // with this test setup it is just jrubyExec.asPath
        expected.text.contains "-cp ${flatRepoLocation.absolutePath}"
    }
}
