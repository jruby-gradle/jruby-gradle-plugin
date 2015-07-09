package com.github.jrubygradle

import com.github.jrubygradle.testhelper.BasicProjectBuilder
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author Schalk W. Cronjé.
 * @author Christian Meier
 */
class JRubyPrepareJarsIntegrationSpec extends Specification {

    static final File CACHEDIR = new File( System.getProperty('TEST_CACHEDIR') ?: 'build/tmp/integrationTest/cache')
    static final File FLATREPO = new File( System.getProperty('TEST_FLATREPO') ?: 'build/tmp/integrationTest/flatRepo')
    static final boolean TESTS_ARE_OFFLINE = System.getProperty('TESTS_ARE_OFFLINE') != null
    static final File TESTROOT = new File( "${System.getProperty('TESTROOT') ?: 'build/tmp/integrationTest'}/jpjis")
    static final String TASK_NAME = 'RubyWax'
    static final String LEAFY_VERSION = '0.4.0'
    static final String OUR_GEM = "rubygems:leafy-complete:${LEAFY_VERSION}"

    void setup() {
        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
    }

    def "Check that default 'jrubyPrepareJars' uses the correct directory"() {
        given:
            def project=BasicProjectBuilder.buildWithLocalRepo(TESTROOT,FLATREPO,CACHEDIR)
            def prepTask = project.task(TASK_NAME, type: JRubyPrepareGems)
            def jrpg = project.tasks.jrubyPrepareGems
            project.jruby.gemInstallDir = TESTROOT.absolutePath

            project.dependencies {
                gems 'io.dropwizard.metrics:metrics-core:3.1.0'
            }
            project.evaluate()
            jrpg.copy()

        expect:
            new File(jrpg.outputDir, 'Jars.lock').text.trim() == 'io.dropwizard.metrics:metrics-core:3.1.0:runtime:'
            new File(jrpg.outputDir, 'jars/io/dropwizard/metrics/metrics-core/3.1.0/metrics-core-3.1.0.jar').exists()
    }
}
