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
package com.github.jrubygradle.testhelper


import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import static com.github.jrubygradle.JRubyExecExtensionIntegrationSpec.BCPROV_NAME

class IntegrationSpecification extends Specification {

    public static final boolean OFFLINE = System.getProperty('TESTS_ARE_OFFLINE')

    public static final String HELLO_WORLD = 'helloWorld.rb'
    public static final String HELLO_NAME = 'helloName.rb'
    public static final String REQUIRES_GEM = 'requiresGem.rb'
    public static final String REQUIRE_THE_A_GEM = 'require-a-gem.rb'
    public static final String ENV_VARS = 'envVars.rb'

    @Shared
    Map testProperties

    @Shared
    File flatRepoLocation

    @Shared
    File mavenRepoLocation

    @Shared
    Map artifactVersions

    @Rule
    TemporaryFolder testFolder

    File projectDir
    File buildFile
    File settingsFile

    void setupSpec() {
        testProperties = loadTestProperties()
        flatRepoLocation = new File(testProperties.flatrepo)
        mavenRepoLocation = new File(testProperties.mavenrepo)

        artifactVersions = [
            'credit_card_validator': testProperties.creditCardValidatorVersion,
            'rspec'                : testProperties.rspecVersion,
            'rspec-core'           : testProperties.rspecVersion,
            'rspec-support'        : testProperties.rspecVersion,
            'metrics-core'         : testProperties.dropwizardMetricsCoreVersion,
            (BCPROV_NAME)          : testProperties.bcprovVersion
        ]
    }

    void setup() {
        projectDir = testFolder.root
        buildFile = new File(projectDir, 'build.gradle')
        settingsFile = new File(projectDir, 'settings.gradle')

        settingsFile.text = ''
    }

    void useScript(final String name, final String relativePath = null) {
        File destination = new File(testFolder.root, relativePath ? "${relativePath}/${name}" : name)
        destination.parentFile.mkdirs()
        destination.text = this.class.getResource("/scripts/${name}").text
    }


    String findDependency(final String organisation, final String artifact, final String extension) {
        String ver = artifactVersions[artifact]
        if (!ver) {
            throw new RuntimeException("No version specified for ${artifact}")
        }
        "'${organisation ?: 'rubygems'}:${artifact}:${ver}@${extension}'"
    }

    String pathAsUriStr(final File path) {
        path.absoluteFile.toURI().toString()
    }

    String getProjectWithMavenRepo() {
        """
        plugins {
            id 'com.github.jruby-gradle.base'
        }

        repositories { 
            flatDir { 
                dirs '${pathAsUriStr(flatRepoLocation)}'.toURI()
            }
            maven { 
                url '${pathAsUriStr(mavenRepoLocation)}'.toURI()
            } 
        }
        """
    }

    String getProjectWithDefaultAndMavenRepo() {
        """
        plugins {
            id 'com.github.jruby-gradle.base'
        }

        jruby.defaultRepositories = true
        repositories.maven { url '${pathAsUriStr(mavenRepoLocation)}'.toURI() } 
        """
    }

    String getProjectWithRubyGemsRepo() {
        """
        plugins {
            id 'com.github.jruby-gradle.base'
        }

        repositories {
            flatDir { 
                dirs '${pathAsUriStr(flatRepoLocation)}'.toURI()
            } 
        
            ruby.gems()
        }
        """
    }

    GradleRunner gradleRunner(List<String> args) {
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withDebug(true)
            .withArguments(args)
            .withPluginClasspath()
            .forwardOutput()
    }

    GradleRunner gradleRunner(String... args) {
        gradleRunner(args as List)
    }

    private Map<String, String> loadTestProperties() {
        this.class.getResource('/jruby-gradle-testconfig.properties').withInputStream { strm ->
            Properties props = new Properties()
            props.load(strm)
            props as Map<String, String>
        }
    }
}