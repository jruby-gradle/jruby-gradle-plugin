package com.github.jrubygradle.internal

import spock.lang.*

class JRubyExecUtilsSpec extends Specification {
    @Ignore
    def "buildArgs should handle scriptArgs with closures"() {
        given:
        List<String> scriptArgs = ['spock']

        when:
        def cmdArgs = JRubyExecUtils.buildArgs([], null, scriptArgs)

        then:
        cmdArgs instanceof List<String>
    }
}
