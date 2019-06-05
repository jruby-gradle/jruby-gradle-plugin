package com.github.jrubygradle.internal.gems


import spock.lang.Specification

class GemToIvySpec extends Specification {

    void 'Write Ivy Xml'() {
        given:
        def gem = new DefaultGemInfo(
            name: 'foo_module',
            version: '1.2.3'
        )
        def gemToIvy = new GemToIvy('https://foo'.toURI())

        when:
        def result = gemToIvy.write(gem)

        then:
        result.contains("info organisation='rubygems' module='foo_module' revision='1.2.3'")
    }
}