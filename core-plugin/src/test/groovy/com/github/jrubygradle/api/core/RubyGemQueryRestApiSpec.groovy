package com.github.jrubygradle.api.core

import com.github.jrubygradle.internal.core.DefaultRubyGemRestApi
import org.ysb33r.grolifant.api.Version
import spock.lang.Specification

class RubyGemQueryRestApiSpec extends Specification {

    public static final String NON_EXISTANT_GEM = 'fasdfasdfasdasdasdfads'
    public static final String CREDIT_CARD = 'credit_card_validator'

    RubyGemQueryRestApi rubygems

    void setup() {
        rubygems = api
    }

    void 'List of versions for non-existant GEM throws exception'() {
        when:
        rubygems.allVersions(NON_EXISTANT_GEM)

        then:
        thrown(ApiException)
    }

    void 'Latest version of non-existant GEM throws exception'() {
        when:
        rubygems.latestVersion(NON_EXISTANT_GEM)

        then:
        thrown(ApiException)
    }

    void 'GEM info on non-existant GEM throws exception'() {
        when:
        rubygems.metadata(NON_EXISTANT_GEM, '1.2.3')

        then:
        thrown(ApiException)
    }

    void 'Extract versions for an existing GEM'() {
        when:
        def versions = rubygems.allVersions(CREDIT_CARD)

        then:
        versions.size() > 1

        when:
        String creditCardValidatorVersion = versions.max(new Comparator<String>() {
            @Override
            int compare(String o1, String o2) {
                Version.of(o1) <=> Version.of(o2)
            }
        })
        def ccv = rubygems.latestVersion(CREDIT_CARD)

        then:
        ccv == creditCardValidatorVersion
    }

    void 'Extract metadata for an existing GEM'() {
        when:
        String testVersion = '1.3.2'
        def metadata = rubygems.metadata(CREDIT_CARD, testVersion)

        then:
        verifyAll {
            metadata.name == CREDIT_CARD
            metadata.version == testVersion
            metadata.authors.size() == 9
            metadata.summary != null
            metadata.sha != null
            metadata.platform == 'ruby'
        }
    }

    private RubyGemQueryRestApi getApi() {
        new DefaultRubyGemRestApi('https://rubygems.org')
    }
}