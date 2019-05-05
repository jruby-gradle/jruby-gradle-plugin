package com.github.jrubygradle.jar

import spock.lang.Specification

class JRubyPluginInstanceSpec extends Specification {
    JRubyJarPlugin plugin

    void setup() {
        plugin = new JRubyJarPlugin()
    }

    void "isJRubyVersionDeprecated"() {
        expect:
        plugin.isJRubyVersionDeprecated(version) == expected

        where:
        version   | expected
        '9.0.0.0' | false
        '1.7.20'  | false
        '1.7.11'  | true
        '1.7.19'  | true
    }

}