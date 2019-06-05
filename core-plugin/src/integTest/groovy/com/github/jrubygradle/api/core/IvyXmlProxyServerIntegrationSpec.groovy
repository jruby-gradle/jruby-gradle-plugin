package com.github.jrubygradle.api.core

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.regex.Pattern


class IvyXmlProxyServerIntegrationSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder

    File projectDir
    File buildFile
    File testKitDir

    void setup() {
        projectDir = new File(temporaryFolder.root, 'test-project')
        buildFile = new File(projectDir, 'build.gradle')
        testKitDir = new File(temporaryFolder.root, '.gradle')
        testKitDir.deleteDir()
        projectDir.deleteDir()
        testKitDir.mkdirs()
        projectDir.mkdirs()
    }

    void 'Startup a server inside a Gradle project'() {
        when:
        buildFile.text = '''
        plugins {
            id 'com.github.jruby-gradle.core'
        }
        
        repositories {
            ruby.gems()
        }
        
        configurations {
            something
        }
        
        dependencies {
            something 'rubygems:credit_card_validator:1.3.2'    
        }
        '''

        BuildResult result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withTestKitDir(testKitDir)
            .withArguments(['dependencies', '--configuration=something',  '-i', '-s'])
            .forwardOutput()
            .withDebug(true)
            .build()

        then:
        result.output.contains('rubygems:credit_card_validator:1.3.2')
        result.output.contains('rubygems:base_app:[1.0.5,) ->')
    }

    void 'Download a collection of GEMs'() {
        setup:
        withBuildFile '''
        dependencies {
            something 'rubygems:credit_card_validator:1.3.2'    
        }
        '''

        when:
        build()

        then:
        new File(projectDir,'build/something/credit_card_validator-1.3.2.gem').exists()
        new File(projectDir,'build/something/base_app-1.0.6.gem').exists()
    }


    void 'Download Asciidoctor Reveal.JS GEM and friends'() {
        setup:
        withBuildFile '''
        dependencies {
            something 'rubygems:asciidoctor-revealjs:2.0.0'    
        }
        '''

        when:
        build()

        then:
        findFiles ~/^asciidoctor-2.*gem$/
    }

    void 'Resolve childprocess GEM which contains an open range Rake'() {
        setup:
        withBuildFile '''
        dependencies {
            something 'rubygems:childprocess:1.0.1'    
        }
        '''

        when:
        build()

        then:
        findFiles ~/^childprocess-1.0.1.gem$/
        findFiles ~/^rake-.*gem$/
    }

    private List<File> findFiles(Pattern pat) {
        new File(projectDir,'build/something').listFiles( new FilenameFilter() {
            @Override
            boolean accept(File dir, String name) {
                name =~ pat
            }
        }) as List<File>
    }

    private BuildResult build() {
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withTestKitDir(testKitDir)
            .withArguments(['copyGems', '-s'])
            .forwardOutput()
            .withDebug(true)
            .build()
    }

    private void withBuildFile(String content) {
        buildFile.text = """
        plugins {
            id 'com.github.jruby-gradle.core'
        }

        repositories {
            ruby.gems()
        }

        configurations {
            something
        }

        dependencies {
            something 'rubygems:asciidoctor-revealjs:2.0.0'
        }

        task copyGems(type: Copy) {
            from configurations.something
            into "\${buildDir}/something"
        }
        
        ${content}
        """
    }
}
