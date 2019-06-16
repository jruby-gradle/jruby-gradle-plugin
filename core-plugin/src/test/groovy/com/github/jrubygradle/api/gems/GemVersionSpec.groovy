/*
 * Copyright (c) 2014-2019, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.api.gems

import com.github.jrubygradle.api.gems.GemVersion
import spock.lang.Specification
import spock.lang.Unroll

import static com.github.jrubygradle.api.gems.GemVersion.gemVersionFromGemRequirement
import static com.github.jrubygradle.api.gems.GemVersion.gemVersionFromGradleIvyRequirement
import static com.github.jrubygradle.api.gems.GemVersion.singleGemVersionFromMultipleGemRequirements

class GemVersionSpec extends Specification {

    @Unroll
    void "#gemRequirement (gem requirement) ⇒ #ivyNotation (ivy)"() {
        when:
        String ivy = gemVersionFromGemRequirement(gemRequirement).toString()

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

    void 'Ivy union of < 3, >= 1.2'() {
        expect:
        singleGemVersionFromMultipleGemRequirements('< 3,>= 1.2').toString() == '[1.2,3['
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
