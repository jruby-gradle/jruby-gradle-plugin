package com.github.jrubygradle.internal

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Resolved to compute gem versions
 */
class GemVersionResolver {
    Map versions
    private final Configuration configuration
    private final Logger logger

    static void setup(Project project) {
        project.configurations.each {
            new GemVersionResolver(project.logger, it)
        }
    }

    GemVersionResolver(Logger logger, Configuration configuration) {
        this.logger = logger
        this.configuration = configuration
        configuration.resolutionStrategy {
            eachDependency { this.resolve(it) }
        }
    }

    // for testing and it needs to override firstRun and log methods
    GemVersionResolver() {
        this.logger = Logging.getLogger(GemVersionResolver)
    }

    // keep it not private for testing
    void firstRun() {
        logger.debug("${configuration.name}\n" +
             '                       --------------------------\n' +
             '                       collect version range info\n' +
             '                       --------------------------')
        Object config = configuration.copyRecursive()
        versions = [:]

        config.resolutionStrategy {
            eachDependency { this.resolve(it) }
        }

        config.resolvedConfiguration

        logger.debug("${configuration.name}\n" +
             '                       ------------------------\n' +
             '                       apply version range info\n' +
             '                       ------------------------')
    }

    void resolve(DependencyResolveDetails details) {
        if (details.requested.group != 'rubygems') {
            return
        }

        if (versions == null) {
            firstRun()
        }

        logger.debug("${configuration}: gem ${details.requested.name} ${details.requested.version}")

        GemVersion version = versions[details.requested.name]

        if (version != null) {
            GemVersion next = version.intersect(details.requested.version)

            if (next.conflict()) {
                throw new GradleException( "there is no overlap for ${versions[details.requested.name]} and ${details.requested.version}" )
            }
            versions[details.requested.name] = next

            logger.debug("${configuration}      collected ${version}")
            logger.debug("${configuration}      resolved  ${next}")

            details.useVersion(next.toString())
        }
        else {
            GemVersion next = new GemVersion(details.requested.version)
            versions[details.requested.name] = next
            logger.debug("${configuration}      nothing collected")
            logger.debug("${configuration}      resolved  ${next}")
        }
    }

    String toString() {
        return "GemVersionResolver${versions}"
    }
}
