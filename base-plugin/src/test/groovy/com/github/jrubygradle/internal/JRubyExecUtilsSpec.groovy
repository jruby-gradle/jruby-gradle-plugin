package com.github.jrubygradle.internal

import org.gradle.api.InvalidUserDataException
import org.ysb33r.grolifant.api.OperatingSystem
import spock.lang.Issue
import spock.lang.Specification

import static com.github.jrubygradle.internal.JRubyExecUtils.buildArgs

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
}
