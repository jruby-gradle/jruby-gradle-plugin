package com.github.jrubygradle.internal.gems


import com.github.jrubygradle.api.gems.GemVersion
import com.github.jrubygradle.api.gems.GemInfo
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder

import java.security.MessageDigest

import static com.github.jrubygradle.api.gems.GemVersion.gemVersionFromGemRequirement

/** Converts from Gem metadata to Ivy metadata.
 *
 * @since 2.0
 */
@CompileStatic
class GemToIvy {

    /** Create a converter from GEM metadata to Ivy metadata.
     *
     * This constructor version assumes that the group is called {@code rubygems}.
     * @param serverUri URI of the RubyGems server.
     */
    GemToIvy(URI serverUri) {
        this.serverUri = serverUri.toString()
    }

    /** Create a converter from GEM metadata to Ivy metadata.
     *
     * @param serverUri URI of the RubyGems server.
     * @param group Use a group name other than {@code rubygems}.
     */
    GemToIvy(URI serverUri, String group) {
        this.serverUri = serverUri.toString()
        this.org = group
    }

    /** Write the Ivy metadata.
     *
     * @param writer Writer for output
     * @param gem GEM metadata
     * @return {@code writer} after population with Ivy metadata in XML format.
     */
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

    /** Write the Ivy metadata to a string.
     *
     * @param gem GEM metadata.
     * @return String containing the Ivy metadata in XML format.
     */
    String write(GemInfo gem) {
        StringWriter writer = new StringWriter()
        writeTo(writer, gem)
        writer.toString()
    }

    /** Writes the SHA1 checksum of the {@code ivy.xmnl} file.
     *
     * @param ivyXml Fle containing the {@code ivy.xml} content/
     * @return Checksum file.
     */
    File writeSha1(File ivyXml) {
        File shaFile = new File(ivyXml.parentFile, "${ivyXml.name}.sha1")
        shaFile.text = MessageDigest.getInstance('SHA-1').digest(ivyXml.bytes).encodeHex().toString()
        shaFile
    }

    private String translateGemRevisionRequirements(String requirements) {
        List<GemVersion> reqs = requirements.split(/,\s*/).collect { String it ->
            gemVersionFromGemRequirement(it)
        }

        if (reqs.size() > 1) {
            reqs.min().union(reqs.max()).toString()
        } else {
            reqs[0].toString()
        }
    }

    private final String serverUri
    private final String org = 'rubygems'
}
