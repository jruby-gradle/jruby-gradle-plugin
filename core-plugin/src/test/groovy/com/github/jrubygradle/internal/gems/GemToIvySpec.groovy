package com.github.jrubygradle.internal.gems


import spock.lang.Specification

class GemToIvySpec extends Specification {

    void 'Write Ivy Xml'() {
        given:
        def gem = new DefaultGemInfo(
            name: 'foo_module',
            version: '1.2.3',
            dependencies: [new DefaultGemDependency(name: 'foo', requirements: '< 13.0')],
            jarRequirements: [new DefaultJarDependency(group: 'bar', name: 'foo', requirements: '>=2.2')]
        )
        def gemToIvy = new GemToIvy('https://foo'.toURI())

        when:
        def result = gemToIvy.write(gem)

        then:
        result.contains("info organisation='rubygems' module='foo_module' revision='1.2.3'")
        result.contains("<dependency org='rubygems' name='foo' rev='(,13.0['")
        result.contains("<dependency org='bar' name='foo' rev='[2.2,)'")
    }
}