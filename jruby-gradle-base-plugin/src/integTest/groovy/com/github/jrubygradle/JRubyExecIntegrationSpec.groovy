package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import com.github.jrubygradle.testhelper.BasicProjectBuilder
import com.github.jrubygradle.testhelper.VersionFinder
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import spock.lang.*

import java.util.regex.Pattern


/**
 * Created by schalkc on 20/08/2014.
 */
@Stepwise
class JRubyExecIntegrationSpec extends Specification {
    static final File CACHEDIR = new File( System.getProperty('TEST_CACHEDIR') ?: 'build/tmp/integrationTest/cache')
    static final File FLATREPO = new File( System.getProperty('TEST_FLATREPO') ?: 'build/tmp/integrationTest/flatRepo')
    static final File TEST_SCRIPT_DIR = new File( System.getProperty('TEST_SCRIPT_DIR') ?: 'src/integTest/resources/scripts')
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/integration-tests'}/jreis")
    static final String TASK_NAME = 'RubyWax'

    Project project
    JRubyExec execTask
    ByteArrayOutputStream output = new ByteArrayOutputStream()

    String getOutputBuffer() {
        return output.toString()
    }

    void setup() {
        if (TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project = BasicProjectBuilder.buildWithLocalRepo(TESTROOT, FLATREPO, CACHEDIR)
        execTask = project.task(TASK_NAME, type: JRubyExec)
    }

    def "Changing the jruby version will load the correct jruby"() {
        given: "Version is set on the task"
        Configuration config
        final String newVersion = '1.7.11'
        assert project.jruby.execVersion != newVersion
        execTask.jrubyVersion newVersion
        Pattern pattern = Pattern.compile(/.*(jruby-complete-.+.jar)/)

        when:
        project.evaluate()
        config = project.configurations.findByName('jrubyExec')

        then: "jruby-complete-${newVersion}.jar must be selected"
        config.files.find { it.name.matches(pattern) && it.name.matches(/${newVersion}/) }
    }

    def "Running a Hello World script"() {
        given:
        project.configure(execTask) {
            script        "${TEST_SCRIPT_DIR}/helloWorld.rb"
            standardOutput output
        }

        when:
        project.evaluate()
        execTask.exec()

        then:
        outputBuffer =~ /Hello, World/
    }

    def "Running a script that requires a gem"() {
        given:
        project.configure(execTask) {
            setEnvironment [:]
            script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
            standardOutput output
        }

        when:
        project.dependencies.add(JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG,
                            VersionFinder.findDependency(FLATREPO, '', 'credit_card_validator', 'gem'))
        project.evaluate()
        execTask.exec()

        then:
        outputBuffer =~ /Not valid/
    }

    def "Running a script that requires a gem, a separate JRuby and a separate configuration"() {
        given:
        final String newVersion = '1.7.11'
        assert project.jruby.execVersion != newVersion
        project.with {
            configurations.create('RubyWax')
            dependencies.add('RubyWax', VersionFinder.findDependency(FLATREPO, '', 'credit_card_validator', 'gem'))
            // we need it from flatrepo and not from regular repo. needed only for jruby <1.7.20
            dependencies.add('RubyWax', VersionFinder.findDependency(FLATREPO ,'rubygems', 'jar-dependencies', 'gem'))
            configure(execTask) {
                script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
                standardOutput output
                jrubyVersion   newVersion
                configuration 'RubyWax'
            }
        }

        when:
        project.evaluate()
        execTask.exec()

        then:
        outputBuffer =~ /Not valid/
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/77')
    def "Running rspec from a script should not cause a gemWorkDir failure" () {
        given:
        def jrubyVersions = FLATREPO.listFiles(
                [ accept : { File dir,String name ->
                    name ==~ /^jruby-complete.+\.jar/
                }] as FilenameFilter
        )

        assert jrubyVersions.size()

        project.with {
            jruby.execVersion = JRubyExecUtils.jrubyJarVersion(jrubyVersions[0])

            dependencies {
                jrubyExec VersionFinder.findDependency(FLATREPO,'','rspec','gem')
                jrubyExec VersionFinder.findDependency(FLATREPO,'','rspec-core','gem')
                jrubyExec VersionFinder.findDependency(FLATREPO,'','rspec-support','gem')
            }

            task('spec',type: JRubyExec) {
                group 'JRuby'
                description 'Execute the RSpecs in JRuby'
                jrubyArgs '-S'
                script 'rspec'
                standardOutput output
            }
        }

        when:
        project.evaluate()
        project.spec.execute()

        then:
        noExceptionThrown()
        outputBuffer =~ /No examples found./
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/73')
    def "Running a script that has a custom gemdir"() {
        given:
        File customGemDir = new File(TESTROOT, 'customGemDir')
        project.configure(execTask) {
            setEnvironment [:]
            script "${TEST_SCRIPT_DIR}/requiresGem.rb"
            standardOutput output
            gemWorkDir customGemDir
        }

        when:
        project.dependencies.add(
            JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG,
            VersionFinder.findDependency(FLATREPO, '', 'credit_card_validator', 'gem')
        )
        project.evaluate()
        execTask.exec()

        then:
        outputBuffer =~ /Not valid/
        customGemDir.exists()
    }
}
