package com.github.jrubygradle.internal

import org.gradle.api.Project

/** Utilities for add Warbler Bootstrap Dependencies so that other jruby gradle plugins can utilise common code
 *
 * @author Schalk W. Cronjé
 * @since 0.1.2
 */
class WarblerBootstrap {

    static final String GROUP = 'com.lookout'
    static final String ARTIFACT = 'warbler-bootstrap'

    /** Adds the dependency to the Project using {@code jruby , warbkerBootstrapVersion} as the version
     * Dependency will be added to the {@code jrubyEmbeds configuration.
     */
    static void addDependency(Project project) {
        project.dependencies {
            jrubyEmbeds group: GROUP, name: ARTIFACT, version: project.extensions.getByName('jruby').warblerBootstrapVersion
        }
    }

    /** Adds a specific version the dependency to the Project.
     * Dependency will be added to the {@code jrubyEmbeds configuration.
     *
     * @param project Project to add dependency to
     * @param version Version of warbler-bootstrap
     */
    static void addDependency(Project project,final String version) {
        project.dependencies {
            jrubyEmbeds group: GROUP, name: ARTIFACT, version: version
        }
    }

    /** Adds a specific version of the dependency to a name configuration of the Project.
     *
     * @param project Project to add dependency to
     * @param version Version of warbler-bootstrap
     * @param configuration Name of configuration
     */
    static void addDependency(Project project,final String version,final String configuration) {
        project.dependencies.add(configuration,"${GROUP}:${ARTIFACT}:${version}")
    }
}
