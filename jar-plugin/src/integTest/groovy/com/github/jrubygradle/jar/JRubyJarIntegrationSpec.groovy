/*
 * Copyright (c) 2014-2023, R. Tyler Croy <rtyler@brokenco.de>,
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
package com.github.jrubygradle.jar

import org.gradle.api.Project
import spock.lang.Ignore
import spock.lang.Specification

/*
 * A series of tests which expect to use the JRubyJar task in more of an integration
 * test fashion, i.e. evaluating the Project, etc
 */

@Ignore
class JRubyJarIntegrationSpec extends Specification {
    static final File TESTROOT = new File("${System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests'}/jrjps")
    static final File TESTREPO_LOCATION = new File("${System.getProperty('TESTREPO_LOCATION') ?: 'build/tmp/test/repo'}")

    Project project

    def setup() {
        project = ProjectBuilder.builder().build()
        project.gradle.startParameter.offline = true

        project.buildscript {
            repositories {
                flatDir dirs: TESTREPO_LOCATION.absolutePath
            }
        }
        project.buildDir = TESTROOT
        project.with {
            logging.level = LIFECYCLE
            apply plugin: 'com.github.jruby-gradle.jar'
            jruby.defaultRepositories = false

            repositories {
                flatDir dirs: TESTREPO_LOCATION.absolutePath
            }
        }
    }

}

