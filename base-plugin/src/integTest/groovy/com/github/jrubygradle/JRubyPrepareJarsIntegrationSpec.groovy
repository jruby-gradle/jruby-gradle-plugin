package com.github.jrubygradle

import com.github.jrubygradle.testhelper.IntegrationSpecification

/**
 * @author Schalk W. Cronjé.
 * @author Christian Meier
 */
class JRubyPrepareJarsIntegrationSpec extends IntegrationSpecification {

    def "Check that default 'jrubyPrepare' uses the correct directory for the jars"() {
        given:
        buildFile.text = """
            ${projectWithLocalRepo}
            jruby.gemInstallDir = '${pathAsUriStr(projectDir)}'.toURI()

            dependencies {
                gems 'io.dropwizard.metrics:metrics-core:3.1.0'
            }                
        """

        when:
        gradleRunner('jrubyPrepare', '-i').build()

        then:
        new File(projectDir, 'Jars.lock').text.trim() == 'io.dropwizard.metrics:metrics-core:3.1.0:runtime:'
        new File(projectDir, 'jars/io/dropwizard/metrics/metrics-core/3.1.0/metrics-core-3.1.0.jar').exists()
    }
}
