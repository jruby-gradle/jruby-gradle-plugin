/*
 * Copyright (c) 2014-2020, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.api.core

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.IgnoreIf
import spock.lang.Issue
import spock.lang.Specification
import spock.lang.Unroll

import java.util.regex.Pattern

@IgnoreIf({ System.getProperty('TESTS_ARE_OFFLINE') })
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
            .withArguments(['dependencies', '--configuration=something', '-i', '-s'])
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
        new File(projectDir, 'build/something/credit_card_validator-1.3.2.gem').exists()
        new File(projectDir, 'build/something/base_app-1.0.6.gem').exists()
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

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/290')
    void 'Resolve ranges with =, != and non-dotted versions'() {
        setup:
        withBuildFile '''
        dependencies {
            something 'rubygems:github-pages:106'    
        }
        '''

        when:
        build()

        then:
        findFiles ~/^github-pages-106.gem$/
    }

    void 'Resolve GEM with JAR dependencies'() {
        setup:
        withBuildFile '''
        repositories.jcenter()
        dependencies {
            something 'rubygems:jruby-openssl:0.10.2'    
        }
        '''

        when:
        build()

        then:
        findFiles ~/^jruby-openssl-0.10.2-java.gem$/
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/325')
    void 'Resolve a prerelease GEM by excluding from GEM strategy'() {
        setup:
        withBuildFile '''
        gemResolverStrategy {
            excludeModule ~/^asciidoctor-pdf$/, ~/.+(alpha|beta).*/ 
        }
        
        dependencies {
            something 'rubygems:asciidoctor-pdf-cjk-kai_gen_gothic:0.1.1'
            something 'rubygems:asciidoctor-pdf:1.5.0.alpha.8'
        }
        '''

        when:
        build()

        then:
        findFiles(~/^asciidoctor-pdf.*\.gem$/).size() == 3
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/380')
    void 'Resolve transitive which contains a single digit twiddle-wakka'() {
        setup:
        withBuildFile '''
        dependencies {
            something 'rubygems:asciidoctor-bibtex:0.3.1'
            something 'rubygems:bibtex-ruby:4.4.7', {
                force = true
            }    
        }
        '''

        when:
        build()

        then:
        findFiles ~/^asciidoctor-bibtex-0.3.1.gem$/
        findFiles ~/^bibtex-ruby-4.4.7.gem$/
    }

    void 'Resolve a transitive dependency which is jruby-specific'() {
        setup:
        withBuildFile '''
        dependencies {
            something 'rubygems:rubocop:0.77.0'
        }
        '''

        when:
        build()

        then:
        findFiles ~/^jaro_winkler-1.5.\d+-java.gem$/
    }

    private List<File> findFiles(Pattern pat) {
        new File(projectDir, 'build/something').listFiles(new FilenameFilter() {
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

    private void withBuildFile(String content, boolean prerelease = false) {
        buildFile.text = """
        plugins {
            id 'com.github.jruby-gradle.core'
        }

        repositories {
            ruby.gems {
                prerelease = ${prerelease}
            }
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
