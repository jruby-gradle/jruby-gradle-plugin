package com.github.jrubygradle

import com.github.jrubygradle.testhelper.BasicProjectBuilder
import spock.lang.Specification


/**
 * @author Schalk W. Cronjé
 */
class JRubyGenerateGradleRbIntegrationSpec extends Specification {

    static final File CACHEDIR = new File( System.getProperty('TEST_CACHEDIR') ?: 'build/tmp/integrationTest/cache')
    static final File FLATREPO = new File( System.getProperty('TEST_FLATREPO') ?: 'build/tmp/integrationTest/flatRepo')
    static final File TESTROOT = new File( "${System.getProperty('TESTROOT') ?: 'build/tmp/integrationTest'}/jggris")
    static final String TASK_NAME = 'RubyWax'

    void setup() {
        if (TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
    }

    def "Generate gradle.rb"() {
        given: "A set of gems"
            def project = BasicProjectBuilder.buildWithLocalRepo(TESTROOT,FLATREPO,CACHEDIR)
            def prepTask = project.task(TASK_NAME, type: GenerateGradleRb)
            def expected = new File(prepTask.destinationDir, prepTask.baseName)
            project.evaluate()


        when: "The load path file is generated "
            prepTask.execute()

        then: "Expect to be in the configured destinationDir and be called gradle.rb"
            expected.exists()

        and: "The GEM_HOME to include gemInstallDir"
            expected.text.find("export GEM_HOME=\"${project.jruby.gemInstallDir}\"")

        and: "The java command invoked with the -cp flag"
            expected.text.find(project.configurations.jrubyExec.asPath)

    }
}
