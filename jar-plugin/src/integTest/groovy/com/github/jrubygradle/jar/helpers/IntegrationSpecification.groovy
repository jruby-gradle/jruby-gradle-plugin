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
package com.github.jrubygradle.jar.helpers

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

class IntegrationSpecification extends Specification {
    static final boolean OFFLINE = System.getProperty('TESTS_ARE_OFFLINE')

    @Shared
    Map testProperties
    @Shared
    File flatRepoLocation
    @Shared
    File mavenRepoLocation

    @Rule
    TemporaryFolder testFolder

    File projectDir
    File buildFile
    File settingsFile

    void setupSpec() {
        testProperties = loadTestProperties()
        flatRepoLocation = new File(testProperties.flatrepo)
        mavenRepoLocation = new File(testProperties.mavenrepo)
    }

    void setup() {
        projectDir = testFolder.root
        buildFile = new File(projectDir, 'build.gradle')
        settingsFile = new File(projectDir, 'settings.gradle')

        settingsFile.text = 'rootProject.name="testproject"'
    }

    GradleRunner gradleRunner(List<String> args) {
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withDebug(true)
                .withArguments(args)
                .withPluginClasspath()
                .forwardOutput()
    }

    String pathAsUriStr(final File path) {
        path.absoluteFile.toURI().toString()
    }

    private Map<String, String> loadTestProperties() {
        this.class.getResource('/jruby-gradle-testconfig.properties').withInputStream { strm ->
            Properties props = new Properties()
            props.load(strm)
            props as Map<String, String>
        }
    }
}