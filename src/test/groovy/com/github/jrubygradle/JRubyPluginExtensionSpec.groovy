package com.github.jrubygradle

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification


/**
 * Created by schalkc on 22/08/2014.
 */
class JRubyPluginExtensionSpec extends Specification {

    def "Creating a JRubyPlugin instance"() {
        given:
            def project = ProjectBuilder.builder().build()

        when:
            def jrpe = new JRubyPluginExtension(project)

        then:
            jrpe.defaultRepositories == true
            jrpe.defaultVersion == jrpe.execVersion
            jrpe.gemInstallDir == new File(project.buildDir,'vendor').absolutePath

    }
}
