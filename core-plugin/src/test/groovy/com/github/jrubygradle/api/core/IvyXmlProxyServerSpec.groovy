/*
 * Copyright (c) 2014-2020, R. Tyler Croy <rtyler@brokenco.de>,
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
package com.github.jrubygradle.api.core

import com.github.jrubygradle.internal.core.IvyXmlRatpackProxyServer
import groovyx.net.http.HttpBuilder
import groovyx.net.http.OkHttpBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class IvyXmlProxyServerSpec extends Specification {

    public static final String CREDIT_CARD = RubyGemQueryRestApiSpec.CREDIT_CARD
    public static final String TEST_IVY_PATH = "${CREDIT_CARD}/1.3.2/ivy.xml"

    IvyXmlProxyServer server
    HttpBuilder httpBuilder

    @Rule
    TemporaryFolder projectRoot

    void setup() {
        server = new IvyXmlRatpackProxyServer(
            projectRoot.root,
            'https://rubygems.org'.toURI(),
            'rubygems',
            new GemRepositoryConfiguration()
        )
        server.run()
        httpBuilder = OkHttpBuilder.configure {
            request.uri = server.bindAddress
        }
    }

    void 'Build an Ivy Xml file from a query to Rubygems'() {
        when: 'I query the local proxy server'
        httpBuilder.get {
            request.uri.path = "/rubygems/${TEST_IVY_PATH}"
        }

        then: 'The Ivy file should be generated and cached locally'
        new File(projectRoot.root,TEST_IVY_PATH)
    }
}