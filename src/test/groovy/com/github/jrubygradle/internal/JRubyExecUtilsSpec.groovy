package com.github.jrubygradle.internal

import spock.lang.*

class JRubyExecUtilsSpec extends Specification {
    def "buildArgs should handle scriptArgs with closures"() {
        given:
        List<Object> scriptArgs = [{'spock'}]

        when:
        def cmdArgs = JRubyExecUtils.buildArgs(['-S'], new File('.'), scriptArgs)

        then:
        cmdArgs instanceof List<String>
        cmdArgs.find { it == 'spock' }
    }

    def "buildArgs should handle scriptArgs with closures for non-JRuby commands"() {
        given:
        List<Object> scriptArgs = [{'spock'}]

        when:
        def cmdArgs = JRubyExecUtils.buildArgs([], new File('.'), scriptArgs)

        then:
        cmdArgs instanceof List<String>
        cmdArgs.find { it == 'spock' }
    }
}
