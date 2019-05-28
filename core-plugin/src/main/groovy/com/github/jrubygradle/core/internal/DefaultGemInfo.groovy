package com.github.jrubygradle.core.internal

import com.github.jrubygradle.core.GemDependency
import com.github.jrubygradle.core.GemInfo
import groovy.transform.CompileStatic

/** An implementation of GEM metadata.
 *
 * @since 2.0
 */
@CompileStatic
class DefaultGemInfo implements GemInfo {
    String name
    String version
    String platform
    String summary
    String description
    String sha
    String rubyVersion
    String rubyGemsVersion

    boolean prerelease

    URI projectUri
    URI gemUri
    URI homepageUri
    URI documentationUri

    List<String> authors = []

    List<GemDependency> dependencies = []
    List<GemDependency> developmentDependencies = []
}
