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