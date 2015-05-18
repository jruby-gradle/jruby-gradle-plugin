package com.github.jrubygradle.jar

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification


/**
 * @author Christian Meier
 *
 */
class JRubyJarConfiguratorSpec extends Specification {
    
    Jar jarTask
    JRubyJarConfigurator configurator

    void setup() {
        Project project = ProjectBuilder.builder().build()
        jarTask = project.tasks.create( name: 'jar', type: JRubyJar )
        configurator = new JRubyJarConfigurator(jarTask)
    }
  
    def 'Checking appendix'() {
        expect:
            jarTask.appendix == 'jruby'
    }
  
    def 'Checking setting no mainClass'() {
        when:
            configurator.initScript('app.rb')
            configurator.applyConfig()

        then:
            jarTask.manifest.attributes['Main-Class'] == JRubyJarConfigurator.DEFAULT_MAIN_CLASS
    }

    def 'Checking setting of mainClass once'() {
        when:
            configurator.initScript('app.rb')
            configurator.mainClass('org.example.Main')
            configurator.applyConfig()

        then:
            jarTask.manifest.attributes['Main-Class'] == 'org.example.Main'
    }

    def 'Checking setting of mainClass twice'() {
        when:
            configurator.initScript(configurator.runnable())
            configurator.defaultMainClass()
            configurator.mainClass('org.example.Main')
            configurator.applyConfig()

        then:
            Exception e = thrown()
            e.message == 'mainClass can be set only once'
    }

    def 'Checking setting no initScript'() {
        when:
            configurator.applyConfig()

        then:
            Exception e = thrown()
            e.message == 'there is no initScript configured'
    }

    def 'Checking setting of initScript once'() {
        when:
            configurator.initScript('app.rb')
            configurator.applyConfig()

        then:
            jarTask.manifest.attributes.containsKey('Main-Class')
    }

    def 'Checking setting of initScript twice'() {
        when:
            configurator.initScript('app.rb')
            configurator.initScript('app.rb')

        then:
            Exception e = thrown()
            e.message == 'initScript can be set only once'
    }

    def 'Checking setup runnable jrubyJar'() {
        when:
            configurator.initScript(configurator.runnable())
            configurator.applyConfig()

        then:
            jarTask.manifest.attributes.containsKey('Main-Class')
    }

    def 'Checking valid library config'() {
        when:
            configurator.initScript(configurator.library())
            configurator.applyConfig()
        then:
            !jarTask.manifest.attributes.containsKey('Main-Class')
    }

    def 'Checking invalid library config'() {
        when:
            configurator.initScript(configurator.library())
            configurator.extractingMainClass()
            configurator.applyConfig()
        then:
            Exception e = thrown()
            e.message == 'can not have mainClass for library'
    }
}
