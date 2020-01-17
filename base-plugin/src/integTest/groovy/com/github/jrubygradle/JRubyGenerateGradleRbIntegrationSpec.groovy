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
package com.github.jrubygradle

import com.github.jrubygradle.testhelper.IntegrationSpecification
import org.ysb33r.grolifant.api.OperatingSystem
import spock.lang.IgnoreIf

/**
 * @author Schalk W. Cronjé
 */
@IgnoreIf({System.getProperty('TESTS_ARE_OFFLINE')})
class JRubyGenerateGradleRbIntegrationSpec extends IntegrationSpecification {

    static final String DEFAULT_TASK_NAME = 'RubyWax'

    @IgnoreIf({ OperatingSystem.current().isWindows() })
    def "Generate gradle.rb"() {
        given: "A set of gems"
        buildFile.text = """
            import com.github.jrubygradle.GenerateGradleRb
    
            ${projectWithRubyGemsRepo}
    
            task ${DEFAULT_TASK_NAME} (type: GenerateGradleRb)  {
                gemInstallDir 'build/gems'
            }
        """

        def expected = new File(projectDir, 'gradle.rb')

        when: "The load path file is generated "
        gradleRunner(DEFAULT_TASK_NAME, '-i', '-s').build()

        then: "Expect to be in the configured destinationDir and be called gradle.rb"
        expected.exists()

        when:
        String content = expected.text

        then: "The GEM_HOME to include gemInstallDir"
        content.contains "export GEM_HOME=\"${new File(projectDir, 'build/gems').absolutePath}"

        and: "The JARS_HOME is set"
        content.contains('export JARS_HOME=')

        and: "The java command invoked with the -cp flag"
        // with this test setup it is just jrubyExec.asPath
        content.contains "-cp ${flatRepoLocation.absolutePath}"
    }
}
