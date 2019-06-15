package com.github.jrubygradle.internal.gems

import com.github.jrubygradle.api.gems.GemInfo
import com.github.jrubygradle.api.gems.GemDependency
import com.github.jrubygradle.api.gems.JarDependency
import groovy.transform.CompileStatic

/** An implementation of GEM metadata.
 *
 * Elements in this class match directly to the Rubgems REST API.
 *
 * @author Schalk W. Cronjé
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
    List<JarDependency> jarRequirements = []
}
