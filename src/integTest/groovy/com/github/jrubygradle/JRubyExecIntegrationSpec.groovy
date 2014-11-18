package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import com.github.jrubygradle.testhelper.BasicProjectBuilder
import com.github.jrubygradle.testhelper.VersionFinder
import org.gradle.api.Task
import spock.lang.*


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

    def project
    def execTask

    void setup() {
        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project = BasicProjectBuilder.buildWithLocalRepo(TESTROOT,FLATREPO,CACHEDIR)
        execTask = project.task(TASK_NAME,type: JRubyExec)

    }

    def "Changing the jruby version will load the correct jruby"() {
        when: "Version is set on the task"
            final String newVersion = '1.7.11'
            assert project.jruby.execVersion != newVersion
            execTask.jrubyVersion = newVersion
            project.evaluate()

            def jarName = project.configurations.getByName('jrubyExec$$'+TASK_NAME).files.find { it.toString().find('jruby-complete') }
            def matches = jarName ? (jarName =~ /.*(jruby-complete-.+.jar)/ ) : null

        then: "jruby-complete-${newVersion}.jar must be selected"
            jarName != null
            matches != null
            "jruby-complete-${newVersion}.jar".toString() ==  matches[0][1]
    }

    def "Running a Hello World script"() {
        given:
            def output = new ByteArrayOutputStream()
            project.configure(execTask) {
                script        "${TEST_SCRIPT_DIR}/helloWorld.rb"
                standardOutput output
            }

        when:
            project.evaluate()
            execTask.exec()

        then:
            output.toString() == "Hello, World\n"
    }

    def "Running a script that requires a gem"() {
        given:
            def output = new ByteArrayOutputStream()
            project.configure(execTask) {
                setEnvironment [:]
                script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
                standardOutput output
            }

        when:
            project.dependencies.add(JRubyExec.JRUBYEXEC_CONFIG,'rubygems:credit_card_validator:1.1.0@gem' )
            project.evaluate()
            execTask.exec()

        then:
            output.toString() == "Not valid\n"
    }

    def "Running a script that requires a gem, a separate jRuby and a separate configuration"() {
        given:
            def output = new ByteArrayOutputStream()
            project.with {
                configurations.create('RubyWax')
                dependencies.add('RubyWax','rubygems:credit_card_validator:1.1.0@gem')
                configure(execTask) {
                    script        "${TEST_SCRIPT_DIR}/requiresGem.rb"
                    standardOutput output
                    jrubyVersion   '1.7.11'
                    configuration 'RubyWax'
                }
            }

        when:
            project.evaluate()
            execTask.exec()

        then:
            output.toString() == "Not valid\n"
    }

    @IgnoreRest
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
                    jrubyExec "rubygems:rspec:${VersionFinder.find(FLATREPO,'rspec','gem')}@gem"
                    jrubyExec "rubygems:rspec-core:${VersionFinder.find(FLATREPO,'rspec-core','gem')}@gem"
                    jrubyExec "rubygems:rspec-support:${VersionFinder.find(FLATREPO,'rspec-support','gem')}@gem"
                }

                task('spec',type: JRubyExec) {
                    group 'JRuby'
                    description 'Execute the RSpecs in JRuby'
                    jrubyArgs '-S'
                    script 'rspec'
                }
            }



        when:
            project.evaluate()
            project.spec.execute()

        then:
            noExceptionThrown()
    }
}
