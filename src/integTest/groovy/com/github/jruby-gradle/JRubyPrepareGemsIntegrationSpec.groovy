package com.lookout.jruby

import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore
import spock.lang.IgnoreIf
import spock.lang.IgnoreRest
import spock.lang.Specification

import static org.gradle.api.logging.LogLevel.LIFECYCLE

/**
 * @author Schalk W. Cronjé.
 */
class JRubyPrepareGemsIntegrationSpec extends Specification {

    static final boolean TESTS_ARE_OFFLINE = System.getProperty('TESTS_ARE_OFFLINE') != null
    static final File TESTROOT = new File( "${System.getProperty('TESTROOT') ?: 'build/tmp/integrationTest'}/jpgis")
    static final String TASK_NAME = 'RubyWax'
    static final String OUR_GEM = 'rubygems:slim:2.0.2'

    def project
    def prepTask

    void setup() {
        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()

        project = ProjectBuilder.builder().build()
        project.with {
            buildDir = TESTROOT
            logging.level = LIFECYCLE
            apply plugin: 'com.lookout.jruby'
        }

        prepTask = project.task(TASK_NAME, type: JRubyPrepareGems)
    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Unpack our gem as normal"() {
        given:
            project.dependencies {
                gems OUR_GEM
            }
            project.configure(prepTask) {
                outputDir TESTROOT
                gems project.configurations.gems
            }
            project.evaluate()
            prepTask.copy()

        expect:
            new File(prepTask.outputDir,'gems/slim-2.0.2').exists()
            new File(prepTask.outputDir,'gems/temple-0.6.8').exists()
            new File(prepTask.outputDir,'gems/tilt-2.0.1').exists()
    }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Unpack our gem, but without transitives"() {
        given:
            project.dependencies {
                gems (OUR_GEM) {
                    transitive = false
                }
            }
            project.configure(prepTask) {
                outputDir TESTROOT
                gems project.configurations.gems
            }
            project.evaluate()
            prepTask.copy()

        expect:
            new File(prepTask.outputDir,'gems/slim-2.0.2').exists()
            !new File(prepTask.outputDir,'gems/temple-0.6.8').exists()
            !new File(prepTask.outputDir,'gems/tilt-2.0.1').exists()
   }

    @IgnoreIf({TESTS_ARE_OFFLINE})
    def "Check that default 'jrubyPrepareGems' uses the correct directories"() {
        given:
            def jrpg = project.tasks.jrubyPrepareGems
            project.jruby.gemInstallDir = TESTROOT.absolutePath

            project.dependencies {
                gems OUR_GEM
            }
            project.evaluate()
            jrpg.copy()

        expect:
            new File(jrpg.outputDir,'gems/slim-2.0.2').exists()
            new File(jrpg.outputDir,'gems/temple-0.6.8').exists()
            new File(jrpg.outputDir,'gems/tilt-2.0.1').exists()
    }
}