package com.github.jrubygradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Ignore
import spock.lang.*
import static org.gradle.api.logging.LogLevel.LIFECYCLE


/**
 * Created by schalkc on 20/08/2014.
 */
@Stepwise
class JRubyPluginIntegrationSpec extends Specification {

    static final boolean TESTS_ARE_OFFLINE = System.getProperty('TESTS_ARE_OFFLINE') != null
    static final File TESTROOT = new File(System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests')

    def project

    void setup() {
        project = ProjectBuilder.builder().build()
        project.buildDir = TESTROOT
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.base'
    }

    @Ignore
    def "dummy test - need to decide what to do with jrubywar test"() {
        expect:
            true
    }
    //@IgnoreIf({TESTS_ARE_OFFLINE})
//    @Ignore
//    def "jrubyWar task needs to add jruby-complete jar"() {
//        given: "That we have a test version that is different that the compiled-in defaultVersion"
//            final String useVersion = '1.7.3'
//            assert useVersion != project.jruby.defaultVersion
//            new File(TESTROOT,'libs').mkdirs()
//            new File(TESTROOT,'classes/main').mkdirs()
//
//        when: "We change the default version and the rubyWar task is executed (via copy)"
//            project.jruby {
//                defaultVersion useVersion
//            }
//            def jrw = project.tasks.jrubyWar
//            project.evaluate()
//            jrw.copy()
//            def jar = project.configurations.jrubyWar.files.find { it.toString().find('jruby-complete') }
//            def jarMatch = (jar !=null) ? (jar =~ /.*(jruby-complete-.+.jar)/) : jar
//
//        then: "We expect the task to have completed succesfully"
//            jrw.outputs.files.singleFile.exists()
//
//        and: "We expect to have a jruby-complete-XXX.jar"
//            jar != null
//
//        and: "jruby-complete-XXX.jar must match ${useVersion}"
//            jarMatch != null
//            "jruby-complete-${useVersion}.jar".toString() == jarMatch[0][1]
//    }
}

