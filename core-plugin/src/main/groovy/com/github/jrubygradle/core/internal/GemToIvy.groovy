package com.github.jrubygradle.core.internal

import com.github.jrubygradle.core.GemInfo
import com.github.jrubygradle.core.GemVersion
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder

import static com.github.jrubygradle.core.GemVersion.gemVersionFromGemRequirement

/** Converts from Gem metadata to Ivy metadata.
 *
 * @since 2.0
 */
@CompileStatic
class GemToIvy {

    GemToIvy(URI serverUri) {
        this.serverUri = serverUri.toString()
    }

    GemToIvy(URI serverUri, String group) {
        this.serverUri = serverUri.toString()
        this.org = group
    }

    @CompileDynamic
    @SuppressWarnings('NoDef')
    Writer writeTo(Writer writer, GemInfo gem) {
        def xml = new MarkupBuilder(writer)

        xml.'ivy-module'(
            'xmlns:xsi': 'http://www.w3.org/2001/XMLSchema-instance',
            'xsi:noNamespaceSchemaLocation': 'http://ant.apache.org/ivy/schemas/ivy.xsd',
            version: '2.0'
        ) {
            info(organisation: this.org, module: gem.name, revision: gem.version /*, publication: */) {
                if (gem.description || gem.homepageUri) {
                    if (gem.homepageUri) {
                        description(homepage: gem.homepageUri) {
                            gem.description ?: ''
                        }
                    } else {
                        description {
                            gem.description ?: ''
                        }
                    }
                }
                /* <license name='' url=''> 1..n */
            }

            publications {
                artifact(type: 'gem')
            }

            if (gem.dependencies) {
                dependencies {
                    gem.dependencies.each { dep ->
                        dependency(org: this.org, name: dep.name, rev: translateGemRevisionRequirements(dep.requirements))
                    }
                }
            }
        }

        writer
    }

    String write(GemInfo gem) {
        StringWriter writer = new StringWriter()
        writeTo(writer, gem)
        writer.toString()
    }

    private String translateGemRevisionRequirements(String requirements) {
        List<GemVersion> reqs = requirements.split(/,\s*/).collect { String it ->
            gemVersionFromGemRequirement(it)
        }

        if (reqs.size() > 1) {
            ivyFormatFromRange(reqs.min().union(reqs.max()))
        } else {
            ivyFormatFromRange(reqs[0])
        }
    }

    private String ivyFormatFromRange(GemVersion range) {
        "${range.lowInclusive ? '[' : ']'}${range.low},${range.high}${range.highInclusive ? ']' : '['}"
    }

    private final String serverUri
    private final String org = 'rubygems'
}
