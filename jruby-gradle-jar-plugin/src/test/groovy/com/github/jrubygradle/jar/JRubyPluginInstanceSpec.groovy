package com.github.jrubygradle.jar

import spock.lang.Specification


/**
 * @author Schalk W. Cronjé
 */

class JRubyPluginInstanceSpec extends Specification {
    JRubyJarPlugin plugin

    def setup() {
        plugin = new JRubyJarPlugin()
    }

    def "isJRubyVersionDeprecated()"() {
        expect:
        plugin.isJRubyVersionDeprecated(version) == expected

        where:
        version   | expected
        '9.1.2.0' | false
        '9.0.0.0' | false
        '1.7.20'  | false
        '1.7.11'  | true
        '1.7.19'  | true
    }
}
