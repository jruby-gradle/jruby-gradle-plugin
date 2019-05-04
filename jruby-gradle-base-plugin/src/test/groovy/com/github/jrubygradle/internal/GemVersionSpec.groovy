package com.github.jrubygradle.internal

import spock.lang.*

class GemVersionSpec extends Specification {
    void "parses single version"() {
        given:
        GemVersion subject = new GemVersion('1.2.3')

        expect:
        subject.toString() == '[1.2.3,1.2.3]'
    }

    void "parses single prerelease version"() {
        given:
        GemVersion subject = new GemVersion('1.2.pre')

        expect:
        subject.toString() == '1.2.pre'
    }

    void "parses gradle semantic version first sample"() {
        given:
        GemVersion subject = new GemVersion('1.2.3+')

        expect:
        subject.toString() == '[1.2.3,1.2.99999]'
    }

    void "parses gradle semantic version second sample"() {
        given:
        GemVersion subject = new GemVersion('1.2.+')

        expect:
        subject.toString() == '[1.2,1.2.99999]'
    }

    void "parses maven open version range"() {
        given:
        GemVersion subject = new GemVersion('[1.2.0,)')

        expect:
        subject.toString() == '[1.2,99999)'
    }

    void "parses maven version range first sample"() {
        given:
        GemVersion subject = new GemVersion('(1.2.0.0, 1.2.4)')

        expect:
        subject.toString() == '(1.2,1.2.4)'
    }

    void "parses maven version range second sample"() {
        given:
        GemVersion subject = new GemVersion('(1.2.0, 1.2.4]')

        expect:
        subject.toString() == '(1.2,1.2.4]'
    }

    void "parses maven version range third sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.0, 1.2.4)')

        expect:
        subject.toString() == '[1.2,1.2.4)'
    }

    void "parses maven version range forth sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1, 1.2.4]')

        expect:
        subject.toString() == '[1.2.1,1.2.4]'
    }

    void "parses maven version range trailing zeros"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1.0.0.0, 1.2.4]')

        expect:
        subject.toString() == '[1.2.1,1.2.4]'
    }

    void "parses maven version range trailing zeros as prereleased version"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1.0.pre.0, 1.2.4]')

        expect:
        subject.toString() == '[1.2.1.0.pre,1.2.4]'
    }

    void "intersects two versions first sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1, 1.2.4]')

        expect:
        subject.intersect('(1.2.1, 1.2.4)').toString() == '(1.2.1,1.2.4)'
    }

    void "intersects two versions second sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.0, 1.2.4]')

        expect:
        subject.intersect('(1.2.1, 1.2.3)').toString() == '(1.2.1,1.2.3)'
    }

    void "intersects two versions third sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.0, 1.2.4]')

        expect:
        subject.intersect('[1.2.1, 1.2.3]').toString() == '[1.2.1,1.2.3]'
    }

    void "intersects two versions first sample reversed"() {
        given:
        GemVersion subject = new GemVersion('(1.2.0, 1.2.4)')

        expect:
        subject.intersect('[1.2.0, 1.2.4]').toString() == '(1.2,1.2.4)'
    }

    void "intersects two versions second sample reversed"() {
        given:
        GemVersion subject = new GemVersion('(1.2.1, 1.2.3)')

        expect:
        subject.intersect('[1.2.0, 1.2.4]').toString() == '(1.2.1,1.2.3)'
    }

    void "intersects two versions third sample reversed"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1, 1.2.3]')

        expect:
        subject.intersect('[1.2.0, 1.2.4]').toString() == '[1.2.1,1.2.3]'
    }

    void "intersects two versions with non lexical ordering"() {
        given:
        GemVersion subject = new GemVersion('[1.2.10, 1.2.14]')

        expect:
        subject.intersect('[1.2.2, 1.10.14]').toString() == '[1.2.10,1.2.14]'
    }

    void "intersects two versions with different length"() {
        given:
        GemVersion subject = new GemVersion('[1.2, 1.2.14]')

        expect:
        subject.intersect('[1.2.2, 1.3]').toString() == '[1.2.2,1.2.14]'
    }

    void "intersects two versions special one"() {
        given:
        GemVersion subject = new GemVersion('[0.9.0,0.9.99999]')

        expect:
        subject.intersect('[0,)').toString() == '[0.9,0.9.99999]'
    }

    void "intersects two versions with special full range"() {
        given:
        GemVersion subject = new GemVersion('[0,)')

        expect:
        subject.intersect('[0.9.0,0.9.99999]').toString() == '[0.9,0.9.99999]'
    }

    void "intersects two versions with workaround due to upstream bug"() {
        given:
        GemVersion subject = new GemVersion('(=2.5.1.1,99999)')

        expect:
        subject.intersect('(=2.5.1.1,)').toString() == '(2.5.1.1,99999)'
    }

    void "intersects with conflict"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1, 1.2.3]')

        expect:
        subject.intersect('[1.2.4, 1.2.4]').conflict() == true
    }

    void "finds no conflicts in non-integer version ranges"() {
        given:
        GemVersion subject = new GemVersion('[1.2.bar, 1.2.foo]')

        expect:
        !subject.conflict()
    }

    void "finds conflicts in non-integer version ranges"() {
        given:
        GemVersion subject = new GemVersion('[1.2.foo, 1.2.bar]')

        expect:
        subject.conflict()
    }

    void "does not throw an exception for a '+' version"() {
        when:
        new GemVersion('+').conflict()

        then:
        notThrown(Exception)
    }
}
