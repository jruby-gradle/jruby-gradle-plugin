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
package com.github.jrubygradle.internal

import org.gradle.api.InvalidUserDataException
import org.ysb33r.grolifant.api.core.OperatingSystem
import spock.lang.Issue
import spock.lang.Specification

import static com.github.jrubygradle.internal.JRubyExecUtils.buildArgs
import static com.github.jrubygradle.internal.JRubyExecUtils.prepareJRubyEnvironment

@SuppressWarnings(['BuilderMethodWithSideEffects'])
class JRubyExecUtilsSpec extends Specification {
    static final boolean IS_WINDOWS = OperatingSystem.current().isWindows()

    void "The version string in a jruby jar filename must be extracted correctly"() {

        expect:
        version == JRubyExecUtils.jrubyJarVersion(new File(jarName))

        where:
        jarName                          || version
        'jruby-complete-9.0.0.0.rc2.jar' || '9.0.0.0.rc2'
        'jruby-complete-9.0.0.0.jar'     || '9.0.0.0'
        'jruby-complete-22.999.888.jar'  || '22.999.888'
        'jruby-complete.jar'             || null
    }

    void "The version information in a jruby jar filename must be extracted correctly"() {

        expect:
        triplet == JRubyExecUtils.jrubyJarVersionTriple(new File(jarName))

        where:
        jarName                          || triplet
        'jruby-complete-9.0.0.0.rc2.jar' || [major: 9, minor: 0, patchlevel: 0]
        'jruby-complete-9.0.0.0.jar'     || [major: 9, minor: 0, patchlevel: 0]
        'jruby-complete-22.999.888.jar'  || [major: 22, minor: 999, patchlevel: 888]
        'jruby-complete.jar'             || [:]
    }

    void "buildArgs() should raise with no script or jrubyArgs"() {
        when:
        buildArgs([], [], null, [])

        then:
        thrown(InvalidUserDataException)
    }

    void "buildArgs() should raise if jrubyArgs (-S) but no script is present"() {
        when:
        buildArgs([], ['-S'], null, [])

        then:
        thrown(InvalidUserDataException)
    }

    void "buildArgs() should raise if script looks absolute but doesn't exist"() {
        given:
        String filename = (IS_WINDOWS ? 'K:' : '') + '/tmp/the-most-unlikely-file-ever'

        when:
        buildArgs([], [], new File(filename), [])

        then:
        thrown(InvalidUserDataException)
    }

    @Issue('https://github.com/jruby-gradle/jruby-gradle-plugin/issues/152')
    void "buildArgs() with a script but no jrubyArgs should add '-S' for the JRuby invocation"() {
        given:
        List<String> args

        when:
        args = buildArgs([], [], new File('rspec'), [])

        then:
        args.size() > 0
        args == ['-rjars/setup', '-S', 'rspec']
    }

    void "buildArgs() with expressive jrubyArgs should be acceptable instead of needing `script`"() {
        given:
        List<String> args

        when:
        args = buildArgs([], ['-S', 'rspec'], null, [])

        then:
        args.size() > 0
        args == ['-rjars/setup', '-S', 'rspec']
    }

    void "Prepare a basic JRuby environment"() {
        when:
        Map preparedEnv = prepareJRubyEnvironment([:], false, new File('tmp/foo'))

        then:
        preparedEnv.size() > 0
    }

    void "Filter out RVM environment values by default for JRuby environment"() {
        when:
        File gemWorkDir = new File('tmp/foo')
        Map preparedEnv = prepareJRubyEnvironment([
            'GEM_HOME'       : '/tmp/spock',
            'RUBY_VERSION'   : 'notaversion',
            'rvm_ruby_string': 'jruby-head',
        ], false, gemWorkDir)

        then:
        preparedEnv['GEM_HOME'] == gemWorkDir.absolutePath
        !preparedEnv.containsKey('rvm_ruby_string')
    }
}
