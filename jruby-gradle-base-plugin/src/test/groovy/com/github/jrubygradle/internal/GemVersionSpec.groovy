package com.github.jrubygradle.internal

import spock.lang.*

class GemVersionSpec extends Specification {
    def "parses single version"() {
        given:
        GemVersion subject = new GemVersion('1.2.3')

        expect:
        subject.toString() == '[1.2.3,1.2.3]'
    }

    def "parses single prerelease version"() {
        given:
        GemVersion subject = new GemVersion('1.2.pre')

        expect:
        subject.toString() == '1.2.pre'
    }

    def "parses gradle sematic version first sample"() {
        given:
        GemVersion subject = new GemVersion('1.2.3+')

        expect:
        subject.toString() == '[1.2.3,1.2.99999]'
    }

    def "parses gradle sematic version second sample"() {
        given:
        GemVersion subject = new GemVersion('1.2.+')

        expect:
        subject.toString() == '[1.2,1.2.99999]'
    }

    def "parses maven open version range"() {
        given:
        GemVersion subject = new GemVersion('[1.2.0,)')

        expect:
        subject.toString() == '[1.2,99999)'
    }

    def "parses maven version range first sample"() {
        given:
        GemVersion subject = new GemVersion('(1.2.0.0, 1.2.4)')

        expect:
        subject.toString() == '(1.2,1.2.4)'
    }

    def "parses maven version range second sample"() {
        given:
        GemVersion subject = new GemVersion('(1.2.0, 1.2.4]')

        expect:
        subject.toString() == '(1.2,1.2.4]'
    }

    def "parses maven version range third sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.0, 1.2.4)')

        expect:
        subject.toString() == '[1.2,1.2.4)'
    }

    def "parses maven version range forth sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1, 1.2.4]')

        expect:
        subject.toString() == '[1.2.1,1.2.4]'
    }

    def "parses maven version range trailing zeros"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1.0.0.0, 1.2.4]')

        expect:
        subject.toString() == '[1.2.1,1.2.4]'
    }

    def "parses maven version range trailing zeros as prereleased version"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1.0.pre.0, 1.2.4]')

        expect:
        subject.toString() == '[1.2.1.0.pre,1.2.4]'
    }

    def "intersects two versions first sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1, 1.2.4]')

        expect:
        subject.intersect('(1.2.1, 1.2.4)').toString() == '(1.2.1,1.2.4)'
    }

    def "intersects two versions second sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.0, 1.2.4]')

        expect:
        subject.intersect('(1.2.1, 1.2.3)').toString() == '(1.2.1,1.2.3)'
    }

    def "intersects two versions third sample"() {
        given:
        GemVersion subject = new GemVersion('[1.2.0, 1.2.4]')

        expect:
        subject.intersect('[1.2.1, 1.2.3]').toString() == '[1.2.1,1.2.3]'
    }

    def "intersects two versions first sample reversed"() {
        given:
        GemVersion subject = new GemVersion('(1.2.0, 1.2.4)')

        expect:
        subject.intersect('[1.2.0, 1.2.4]').toString() == '(1.2,1.2.4)'
    }

    def "intersects two versions second sample reversed"() {
        given:
        GemVersion subject = new GemVersion('(1.2.1, 1.2.3)')

        expect:
        subject.intersect('[1.2.0, 1.2.4]').toString() == '(1.2.1,1.2.3)'
    }

    def "intersects two versions third sample reversed"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1, 1.2.3]')

        expect:
        subject.intersect('[1.2.0, 1.2.4]').toString() == '[1.2.1,1.2.3]'
    }

    def "intersects two versions with non lexical ordering"() {
        given:
        GemVersion subject = new GemVersion('[1.2.10, 1.2.14]')

        expect:
        subject.intersect('[1.2.2, 1.10.14]').toString() == '[1.2.10,1.2.14]'
    }

    def "intersects two versions with different length"() {
        given:
        GemVersion subject = new GemVersion('[1.2, 1.2.14]')

        expect:
        subject.intersect('[1.2.2, 1.3]').toString() == '[1.2.2,1.2.14]'
    }

    def "intersects two versions special one"() {
        given:
        GemVersion subject = new GemVersion('[0.9.0,0.9.99999]')

        expect:
        subject.intersect('[0,)').toString() == '[0.9,0.9.99999]'
    }

    def "intersects two versions special one"() {
        given:
        GemVersion subject = new GemVersion('[0,)')

        expect:
        subject.intersect('[0.9.0,0.9.99999]').toString() == '[0.9,0.9.99999]'
    }

    def "intersects with conflict"() {
        given:
        GemVersion subject = new GemVersion('[1.2.1, 1.2.3]')

        expect:
        subject.intersect('[1.2.4, 1.2.4]').conflict() == true
    }
}
