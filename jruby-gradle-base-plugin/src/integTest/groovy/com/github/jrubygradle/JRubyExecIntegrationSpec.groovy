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
    File mavenRepo

    String getOutputBuffer() {
        return output.toString()
    }

    void setup() {
        if (TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project = BasicProjectBuilder.buildWithLocalRepo(TESTROOT, FLATREPO, CACHEDIR)
        mavenRepo = project.file("../../../../../src/integTest/mavenrepo")
        execTask = project.task(TASK_NAME, type: JRubyExec)
    }

    def "Changing the jruby version will load the correct jruby"() {
        given: "Version is set on the task"
        Configuration config
        final String configName = 'integ-exec-config'
        final String newVersion = '1.7.19'
        Pattern pattern = Pattern.compile(/.*(jruby-complete-.+.jar)/)

        when:
        project.with {
            jruby.defaultRepositories = false
            repositories {
                maven {
                    url "file://" + mavenRepo.absolutePath
                }
            }
        }

        project.configure(execTask) {
            configuration configName
            jrubyVersion newVersion
        }

        project.evaluate()
        config = project.configurations.findByName(configName)

        then: "the project config should be unaffected"
        project.jruby.execVersion != newVersion

        and: "jruby-complete-${newVersion}.jar must be selected"
        config.files.find { it.name.matches(pattern) && it.name.matches(/(.*)${newVersion}.jar/) }
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

    def "Running a script that requires a gem using default embedded rubygems-servlets maven repo"() {
        // java-1.7 runs int o perm-space problems
        if (System.getProperty('java.version').startsWith('1.') ) {
            println 'skipping extra rubygems-servlet test for jdk-1.7'
            return
        }
        given:
        String version = '0.1.1'
        project.configure(execTask) {
            setEnvironment [:]
            script        "${TEST_SCRIPT_DIR}/require-a-gem.rb"
            standardOutput output
        }
        project.repositories {
            rubygems()
        }
        project.dependencies {
            jrubyExec "rubygems:a:${version}"
        }

        when:
        project.evaluate()
        execTask.exec()

        then:
        // note this test has some error output not sure where this comes from. but the actual test passes
        outputBuffer =~ /loaded 'a' gem with version ${version}/
    }

    def "Running a script that requires a gem using custom embedded rubygems-servlets maven repo"() {
        given:
        String version = '0.1.0'
        project.configure(execTask) {
            setEnvironment [:]
            script        "${TEST_SCRIPT_DIR}/require-a-gem.rb"
            standardOutput output
        }
        project.repositories {
            rubygems('http://rubygems.lasagna.io/proxy')
        }
        project.dependencies {
            jrubyExec "rubygems:a:${version}"
        }

        when:
        project.evaluate()
        execTask.exec()

        then:
        // note this test has some error output not sure where this comes from. but the actual test passes
        outputBuffer =~ /loaded 'a' gem with version ${version}/
    }

    def "Running a script that requires a gem, a separate JRuby and a separate configuration"() {
        given:
        final String newVersion = '1.7.19'
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
        project.with {
            /* see integration-tests.gradle, we're ensuring that we always have at
             * least one version of JRuby installed
             */
            jruby {
                execVersion '1.7.19'
                defaultRepositories false
            }

            /* adding our fixtured mavenRepo so we can resolve jar-dependencies properly */
            repositories {
                maven {
                    url "file://" + mavenRepo.absolutePath
                }
            }

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
