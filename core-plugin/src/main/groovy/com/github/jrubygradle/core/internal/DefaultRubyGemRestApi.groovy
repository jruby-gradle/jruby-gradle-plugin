package com.github.jrubygradle.core.internal

import com.github.jrubygradle.core.ApiException
import com.github.jrubygradle.core.GemInfo
import com.github.jrubygradle.core.RubyGemQueryRestApi
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovyx.net.http.HttpBuilder
import groovyx.net.http.HttpException
import okhttp3.OkHttpClient

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.NativeHandlers.Parsers.json
import static groovyx.net.http.OkHttpBuilder.configure

/**
 * @since 2.0
 */
@CompileStatic
class DefaultRubyGemRestApi implements RubyGemQueryRestApi {

    DefaultRubyGemRestApi(final String serverUri) {
        this.httpBuilder = getHttpBuilder(serverUri.toURI())
    }

    DefaultRubyGemRestApi(final URI serverUri) {
        this.httpBuilder = getHttpBuilder(serverUri)
    }

    @Override
    @SuppressWarnings('CatchThrowable')
    List<String> allVersions(String gemName) {
        try {
            extractVersions(getData(V1, "versions/${gemName}"))
        } catch (Throwable e) {
            throw new ApiException("Count not retrieve list of versions for ${gemName}", e)
        }
    }

    @Override
    @SuppressWarnings('CatchThrowable')
    String latestVersion(String gemName) {
        String version
        try {
            version = extractVersion(getData(V1, "versions/${gemName}/latest"))
        } catch (Throwable e) {
            throw new ApiException("Failed to retrieve latest version of ${gemName}", e)
        }
        if (version == 'unknown') {
            throw new ApiException("Cound not retrieve latest version of ${gemName}. Maybe it does not exist")
        }
        version
    }

    @Override
    @SuppressWarnings('CatchThrowable')
    GemInfo metadata(String gemName, String gemVersion) {
        try {
            extractMetadata(getData(V2, "rubygems/${gemName}/versions/${gemVersion}"))
        } catch (HttpException e) {
            throw new ApiException(":${gemName}:${gemVersion} not found", e)
        } catch (Throwable e) {
            throw new ApiException("Could not obtain information for :${gemName}:${gemVersion}.", e)
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
            authors: ((String) jsonParser.authors).split(', ').toList() ?: [],
            prerelease: jsonParser.prerelease
            // licenses arrayList
        )

        if (jsonParser.dependencies?.runtime) {
            metadata.dependencies.addAll(jsonParser.dependencies.runtime.collect {
                new DefaultGemDependency(name: it.name, requirements: it.requirements)
            })
        }

        if (jsonParser.dependencies?.development) {
            metadata.developmentDependencies.addAll(jsonParser.dependencies.development.collect {
                new DefaultGemDependency(name: it.name, requirements: it.requirements)
            })
        }

        metadata
    }

    private final HttpBuilder httpBuilder
    static private final String V1 = 'api/v1'
    static private final String V2 = 'api/v2'
}
