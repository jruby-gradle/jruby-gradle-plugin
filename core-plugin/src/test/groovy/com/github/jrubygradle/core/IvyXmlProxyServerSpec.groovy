package com.github.jrubygradle.core

import com.github.jrubygradle.core.internal.IvyXmlRatpackProxyServer
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
        server = new IvyXmlRatpackProxyServer(projectRoot.root, 'https://rubygems.org'.toURI(), 'rubygems')
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