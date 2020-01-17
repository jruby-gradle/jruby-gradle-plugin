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
package com.github.jrubygradle.api.gems

import com.github.jrubygradle.api.core.RepositoryHandlerExtension
import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionSelector

import java.util.regex.Pattern

/** Defines groups which contains GEMs and controls GEM resolving rules.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class GemResolverStrategy {

    public static final String NAME = 'gemResolverStrategy'

    /** Is this group/organisation a GEM group ?
     *
     * @param groupName Name of group/organisation.
     * @return {@code true} is group is a GEM group.
     */
    boolean isGemGroup(final String groupName) {
        groups.contains(groupName)
    }

    /** Add a new group for GEMs.
     *
     * @param groupName Name of group to add.
     */
    void addGemGroup(final String groupName) {
        groups.add(groupName)
    }

    /** Exclude a configuration from being resolved using the GEM
     *  version resolver strategy.
     *
     * @param configs Configurations to be excluded
     */
    void excludeConfigurations(Configuration... configs) {
        this.excludedConfigurations.addAll(configs*.name)
    }

    /** Exclude a configuration from being resolved using the GEM
     *  version resolver strategy.
     *
     * @param configs Configurations to be excluded
     */
    void excludeConfigurations(String... configs) {
        this.excludedConfigurations.addAll(configs)
    }

    /** Exclude a module from being resolved using the GEM version resolver
     * strategy.
     *
     * @param name Module name. Never {@code null}.
     * @param version Version. Can be {@code null}.
     */
    void excludeModule(String name, String version = null) {
        excludedModules.add(new Matcher(
            module: Pattern.compile(Pattern.quote(name)),
            version: version ? Pattern.compile(Pattern.quote(version)) : null
        ))
    }

    /** Exclude a module from being resolved using the GEM version resolver
     * strategy.
     *
     * @param namePattern Pattern for name. Never {@code null}.
     * @param versionPattern Pattern for version. Can be {@code null}
     */
    void excludeModule(Pattern namePattern, Pattern versionPattern = null) {
        excludedModules.add(new Matcher(module: namePattern, version: versionPattern))
    }

    /** Whether the GEM version resolving strategy should be applied for a specific module.
     *
     * In most cases this will always be {@code true} unless a specific rule excludes it.
     *
     * @param mvs Module version selector
     * @return Whether the Bundler-like version selector atregty may be applied
     */
    boolean useGemVersionResolver(ModuleVersionSelector mvs) {
        isGemGroup(mvs.group) && excludedModules.find { it.match(mvs.name, mvs.version) }
    }

    /** Whether the GEM version resolving strategy should be applied to a specific configuration.
     *
     * In most cases this will always be {@code true} unless a specific rule excludes it.
     *
     * @param configurationName Name fo configuration
     * @return Whether the Bundler-like version selector strategy may be applied
     */
    boolean useGemVersionResolver(String configurationName) {
        configurationName in excludedConfigurations
    }

    @EqualsAndHashCode
    private class Matcher {
        Pattern module
        Pattern version

        boolean match(String name, String ver) {
            name =~ module && (this.version == null || ver ==~ this.version)
        }
    }

    private final Set<Matcher> excludedModules = [].toSet()
    private final Set<String> excludedConfigurations = [].toSet()
    private final Set<String> groups = [RepositoryHandlerExtension.DEFAULT_GROUP_NAME].toSet()
}
