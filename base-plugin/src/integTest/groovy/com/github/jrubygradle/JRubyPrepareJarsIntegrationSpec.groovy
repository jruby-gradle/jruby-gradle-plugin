/*
 * Copyright (c) 2014-2019, R. Tyler Croy <rtyler@brokenco.de>,
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
package com.github.jrubygradle

import com.github.jrubygradle.testhelper.IntegrationSpecification
import spock.lang.IgnoreIf

/**
 * @author Schalk W. Cronjé.
 * @author Christian Meier
 */
@IgnoreIf({ IntegrationSpecification.OFFLINE })
class JRubyPrepareJarsIntegrationSpec extends IntegrationSpecification {

    def "Check that default 'jrubyPrepare' uses the correct directory for the jars"() {
        given:
        String testVer = testProperties.dropwizardMetricsCoreVersion
        buildFile.text = """
            ${projectWithRubyGemsRepo}

            dependencies {
                gems "io.dropwizard.metrics:metrics-core:${testVer}"
            }                
        """

        when:
        gradleRunner('jrubyPrepare', '-i').build()

        then:
        new File(projectDir, 'build/.gems/Jars.lock').text.trim() ==
            "io.dropwizard.metrics:metrics-core:${testProperties.dropwizardMetricsCoreVersion}:runtime:"
        new File(
            projectDir,
            "build/.gems/jars/io/dropwizard/metrics/metrics-core/${testVer}/metrics-core-${testVer}.jar"
        ).exists()
    }
}
