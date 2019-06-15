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
package com.github.jrubygradle.internal.core

import com.github.jrubygradle.api.gems.GemInfo
import com.github.jrubygradle.api.gems.JarDependency
import com.github.jrubygradle.internal.gems.DefaultGemDependency
import com.github.jrubygradle.internal.gems.DefaultGemInfo
import com.github.jrubygradle.internal.gems.DefaultJarDependency
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpException
import okhttp3.OkHttpClient

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.NativeHandlers.Parsers.json
import static groovyx.net.http.OkHttpBuilder.configure

/** Implementation for a RubyGems REST API client based upon
 * HttpBuilder-ng.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class DefaultRubyGemRestApi implements com.github.jrubygradle.api.core.RubyGemQueryRestApi {

    /** Creates a client from a URI
     *
     * @param serverUri URI as a String. ONly the scheme plus host
     * parts should be provided.
     */
    DefaultRubyGemRestApi(final String serverUri) {
        this.httpBuilder = getHttpBuilder(serverUri.toURI())
    }

    /** Creates a client from a URI
     *
     * @param serverUri Only the scheme plus host
     * parts should be provided.
     */
    DefaultRubyGemRestApi(final URI serverUri) {
        this.httpBuilder = getHttpBuilder(serverUri)
    }

    /** Returns all versions of a specific GEM.
     *
     * @param gemName Name of GEM.
     * @return List of versions
     * @throw {@link com.github.jrubygradle.api.core.ApiException} if a networking error occurred.
     */
    @Override
    @SuppressWarnings('CatchThrowable')
    List<String> allVersions(String gemName) {
        try {
            extractVersions(getData(V1, "versions/${gemName}"))
        } catch (Throwable e) {
            throw new com.github.jrubygradle.api.core.ApiException("Count not retrieve list of versions for ${gemName}", e)
        }
    }

    /** Returns the latest version for a specific GEM.
     *
     * @param gemName Name of GEM.
     * @return Latest version.
     * @throw {@link com.github.jrubygradle.api.core.ApiException} if a networking error occurred or the GEM does not exist.
     */
    @Override
    @SuppressWarnings('CatchThrowable')
    String latestVersion(String gemName) {
        String version
        try {
            version = extractVersion(getData(V1, "versions/${gemName}/latest"))
        } catch (Throwable e) {
            throw new com.github.jrubygradle.api.core.ApiException("Failed to retrieve latest version of ${gemName}", e)
        }
        if (version == 'unknown') {
            throw new com.github.jrubygradle.api.core.ApiException("Cound not retrieve latest version of ${gemName}. Maybe it does not exist")
        }
        version
    }

    /** Retrieves the GEM metadata.
     *
     * @param gemName Name of GEM.
     * @param gemVersion Version of the GEM.
     * @return {@link GemInfo} instance.
     * @throw {@link com.github.jrubygradle.api.core.ApiException} if a networking error occurred or the GEM does not exist.
     */
    @Override
    @SuppressWarnings('CatchThrowable')
    GemInfo metadata(String gemName, String gemVersion) {
        try {
            extractMetadata(getData(V2, "rubygems/${gemName}/versions/${gemVersion}"))
        } catch (HttpException e) {
            throw new com.github.jrubygradle.api.core.ApiException(":${gemName}:${gemVersion} not found", e)
        } catch (Throwable e) {
            throw new com.github.jrubygradle.api.core.ApiException("Could not obtain information for :${gemName}:${gemVersion}.", e)
        }
    }

    private Object getData(final String useApiVersion, String relativePath) {
        httpBuilder.get {
            request.uri.path = "/${useApiVersion}/${relativePath}.json"
            response.parser(JSON[0]) { config, resp ->
                json(config, resp)
            }
        }
    }

    private static HttpBuilder getHttpBuilder(URI uri) {
        configure {
            request.uri = uri
            client.clientCustomizer { OkHttpClient.Builder builder ->
                builder.followRedirects(true)
                builder.followSslRedirects(true)
            }
        }
    }

    @CompileDynamic
    private String extractVersion(Object jsonParser) {
        jsonParser.version
    }

    @CompileDynamic
    private List<String> extractVersions(Object jsonParser) {
        jsonParser*.number
    }

    @CompileDynamic
    private GemInfo extractMetadata(Object jsonParser) {
        DefaultGemInfo metadata = new DefaultGemInfo(
            name: jsonParser.name,
            version: jsonParser.version,
            platform: jsonParser.platform,
            description: jsonParser.description,
            summary: jsonParser.summary,
            sha: jsonParser.sha,
            rubyVersion: jsonParser.ruby_version,
            rubyGemsVersion: jsonParser.rubygems_version,
            projectUri: jsonParser.project_uri?.toURI(),
            gemUri: jsonParser.gem_uri?.toURI(),
            homepageUri: jsonParser.homepage_uri?.toURI(),
            documentationUri: jsonParser.documentation_uri?.toURI(),
            authors: ((String) jsonParser.authors).split(COMMA_SPACE).toList() ?: [],
            prerelease: jsonParser.prerelease
            // licenses arrayList
        )

        if (jsonParser.dependencies?.runtime) {
            metadata.dependencies.addAll( Transform.toList(jsonParser.dependencies.runtime) {
                new DefaultGemDependency(name: it.name, requirements: it.requirements)
            })
        }

        if (jsonParser.dependencies?.development) {
            metadata.developmentDependencies.addAll(jsonParser.dependencies.development.collect {
                new DefaultGemDependency(name: it.name, requirements: it.requirements)
            })
        }

        if (jsonParser.requirements) {
            metadata.jarRequirements.addAll(findJarRequirements(jsonParser.requirements))
        }

        metadata
    }

    private List<JarDependency> findJarRequirements(Iterable<String> reqs) {
        reqs.findAll { String it ->
            it.startsWith('jar ')
        }.collect {
            String[] parts = ((String) it)[4..-1].split(COMMA_SPACE, 2)
            String[] name_parts = parts[0].split(':', 2)
            if (name_parts.size() == 1) {
                new DefaultJarDependency(
                    name: parts[0],
                    requirements: parts[1]
                )
            } else {
                new DefaultJarDependency(
                    group: name_parts[0],
                    name: name_parts[1],
                    requirements: parts[1]
                )
            }
        } as List<JarDependency>
    }

    private final HttpBuilder httpBuilder
    static private final String V1 = 'api/v1'
    static private final String V2 = 'api/v2'
    static private final String COMMA_SPACE = ', '
}
