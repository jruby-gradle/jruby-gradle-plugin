package com.github.jrubygradle.api.gems

import com.github.jrubygradle.api.gems.GemVersion
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.jrubygradle.api.gems.GemVersion.gemVersionFromGradleIvyRequirement

class GemVersionSpec extends Specification {

    @Unroll
    void "#gemRequirement (gem) ⇒ #ivyNotation (ivy)"() {
        when:
        String ivy = GemVersion.gemVersionFromGemRequirement(gemRequirement).toString()

        then:
        ivy == ivyNotation

        where:
        gemRequirement | ivyNotation
        '= 1.0.0'      | '1.0.0'
        '> 2.0'        | ']2.0,)'
        '>= 2.2.0'     | '[2.2.0,)'
        '<= 3.0'       | '(,3.0]'
        '< 2.3.0'      | '(,2.3.0['
        '~> 1.0'       | '[1.0.0,2.0['
        '~> 2.2'       | '[2.2.0,3.0['
        '~> 2.2.0'     | '[2.2.0,2.3.0['
    }

    @Unroll
    void "toString: #ivyVer (ivy) ⇒ #ivyRange (range)"() {
        when:
        GemVersion subject = gemVersionFromGradleIvyRequirement(ivyVer)

        then:
        subject.toString() == ivyRange

        where:
        ivyVer                  | ivyRange
        '1.2.3'                 | '1.2.3'
        '1.2.pre'               | '1.2.pre'
        '1.2.3+'                | '[1.2.3,1.2.99999]'
        '1.2.+'                 | '[1.2.0,1.2.99999]'
        '[1.2.0,)'              | '[1.2.0,)'
        '(1.2.0.0,1.2.4)'       | '(1.2.0.0,1.2.4)'
        '(1.2.0,1.2.4]'         | '(1.2.0,1.2.4]'
        '[1.2.0,1.2.4)'         | '[1.2.0,1.2.4)'
        '[1.2.1,1.2.4]'         | '[1.2.1,1.2.4]'
        '[1.2.1.0.0.0,1.2.4]'   | '[1.2.1.0.0.0,1.2.4]'
        '[1.2.1.0.pre.0,1.2.4]' | '[1.2.1.0.pre.0,1.2.4]'
    }

    @Unroll
    void "Intersect: #ivyLeft ∩ #ivyRight ⇒ Gem #gemVer"() {
        when:
        GemVersion lhs = gemVersionFromGradleIvyRequirement(ivyLeft)
        GemVersion rhs = gemVersionFromGradleIvyRequirement(ivyRight)

        then:
        lhs.intersect(rhs).toString() == gemVer

        where:
        ivyLeft             | ivyRight            | gemVer
        '[1.2.1,1.2.4]'     | ']1.2.1,1.2.4['     | ']1.2.1,1.2.4['
        '[1.2.0,1.2.4]'     | ']1.2.1,1.2.3['     | ']1.2.1,1.2.3['
        '[1.2.0,1.2.4]'     | '[1.2.1,1.2.3]'     | '[1.2.1,1.2.3]'
        ']1.2.0,1.2.4['     | '[1.2.0,1.2.4]'     | ']1.2.0,1.2.4['
        ']1.2.1,1.2.3['     | '[1.2.0,1.2.4]'     | ']1.2.1,1.2.3['
        '[1.2.1,1.2.3]'     | '[1.2.0,1.2.4]'     | '[1.2.1,1.2.3]'
        '[1.2.10,1.2.14]'   | '[1.2.2,1.10.14]'   | '[1.2.10,1.2.14]'
        '[1.2,1.2.14]'      | '[1.2.2,1.3]'       | '[1.2.2,1.2.14]'
        '[0.9.0,0.9.99999]' | '[0,)'              | '[0.9.0,0.9.99999]'
        '[0,)'              | '[0.9.0,0.9.99999]' | '[0.9.0,0.9.99999]'
        ']2.5.1.1,99999]'   | ']2.5.1.1,)'        | ']2.5.1.1,99999]'
    }

    void "intersects with conflict"() {
        given:
        GemVersion subject = gemVersionFromGradleIvyRequirement('[1.2.1,1.2.3]')

        expect:
        subject.intersect('[1.2.4, 1.2.4]').conflict() == true
    }

    void "finds no conflicts in non-integer version ranges"() {
        given:
        GemVersion subject = gemVersionFromGradleIvyRequirement('[1.2.bar,1.2.foo]')

        expect:
        !subject.conflict()
    }

    void "does not throw an exception for a '+' version"() {
        when:
        gemVersionFromGradleIvyRequirement('+').conflict()

        then:
        notThrown(Exception)
    }
}
