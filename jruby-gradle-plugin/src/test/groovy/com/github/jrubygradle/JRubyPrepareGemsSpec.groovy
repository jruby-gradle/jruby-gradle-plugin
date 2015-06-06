package com.github.jrubygradle

import org.gradle.api.file.FileCollection
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.*

import static org.gradle.api.logging.LogLevel.LIFECYCLE


/**
 * @author Schalk W. Cronjé.
 */
class JRubyPrepareGemsSpec extends Specification {

    static final File TEST_GEM_DIR = new File( System.getProperty('TEST_GEM_DIR') ?: 'src/test/resources/gems')
    static final File TESTROOT = new File( "${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}/jpgs")
    static final File TEST_JRUBY_CLASSPATH = new File(System.getProperty('TEST_JRUBY_CLASSPATH'))
    static final String TASK_NAME = 'RubyWax'

    def project
    def prepTask

    void setup() {
        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()

        def matches = TEST_JRUBY_CLASSPATH.name =~ /.*jruby-complete-(.+).jar/
        assert matches != null

        project = ProjectBuilder.builder().build()
        project.with {
            buildDir = TESTROOT
            logging.level = LIFECYCLE
            apply plugin: 'com.github.jruby-gradle.base'
            jruby.defaultRepositories = false

            repositories {
                flatDir {
                    dir TEST_GEM_DIR
                }
                flatDir {
                    dir TEST_JRUBY_CLASSPATH.parentFile
                }
            }

            dependencies {
                jrubyExec ":jruby-complete:${matches[0][1]}"
            }
        }

        prepTask = project.task(TASK_NAME, type: JRubyPrepareGems)
    }

    def "Configuring JRubyPrepareGems"() {
        project.configure(prepTask) {
            outputDir TESTROOT
            gems new File(TEST_GEM_DIR,'1.gem')
            gems new File(TEST_GEM_DIR,'2.gem').absolutePath
            gems new File(TEST_GEM_DIR,'3.GEM').absolutePath
            gems new File(TEST_GEM_DIR,'2.foo').absolutePath
        }
        project.evaluate()
        FileCollection fc = prepTask.getGems()

        expect:
            prepTask.outputDir == TESTROOT
            fc.contains(new File(TEST_GEM_DIR,'1.gem'))
            fc.contains(new File(TEST_GEM_DIR,'2.gem'))
            fc.contains(new File(TEST_GEM_DIR,'3.GEM'))
            !fc.contains(new File(TEST_GEM_DIR,'2.foo'))
    }

    @Ignore
    def "Having added gems as files, please unpack them to a designated area"() {
        given:
            project.configure(prepTask) {
                outputDir TESTROOT
                gems '/Users/schalkc/.m2/repository/rubygems/erubis/2.7.0/erubis-2.7.0.gem'
                gems new File('/Users/schalkc/.m2/repository/rubygems/slim/2.0.2/slim-2.0.2.gem')
            }
            project.evaluate()
            prepTask.copy()

        expect:
            new File(prepTask.outputDir,'gems/erubis-2.7.0').exists()
    }

    @Ignore
    def "Having added gems via a configuration, please unpack them to a designated area"() {
        given:
            project.configure(prepTask) {
                outputDir TESTROOT
                gems '/Users/schalkc/.m2/repository/rubygems/erubis/2.7.0/erubis-2.7.0.gem'
                gems new File('/Users/schalkc/.m2/repository/rubygems/slim/2.0.2/slim-2.0.2.gem')
            }
            project.evaluate()
            prepTask.copy()

        expect:
            new File(prepTask.outputDir,'gems/erubis-2.7.0').exists()
    }
}
