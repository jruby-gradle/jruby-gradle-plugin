package com.github.jrubygradle.internal

import com.github.jrubygradle.core.GemVersion
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.github.jrubygradle.core.GemVersion.gemVersionFromGradleIvyRequirement

/**
 * Resolver to compute gem versions
 */
class GemVersionResolver {
    private static final String DBG_SEPARATOR = '                       ------------------------'
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
        try {
            configuration.resolutionStrategy {
                eachDependency { this.resolve(it) }
            }
        } catch (GradleException e) {
            logger.debug("${configuration.name}\n${DBG_SEPARATOR}\n" +
                    "                       can not be a gem\n${DBG_SEPARATOR}")
        }
    }

    // for testing and it needs to override firstRun and log methods
    GemVersionResolver() {
        this.logger = Logging.getLogger(GemVersionResolver)
    }

    // keep it not private for testing
    void firstRun() {
        logger.debug("${configuration.name}\n${DBG_SEPARATOR}\n" +
             "                       collect version range info\n${DBG_SEPARATOR}")
        Object config = configuration.copyRecursive()
        versions = [:]

        config.resolutionStrategy {
            eachDependency { this.resolve(it) }
        }

        config.resolvedConfiguration

        logger.debug("${configuration.name}\n${DBG_SEPARATOR}\n" +
             "                       apply version range info\n${DBG_SEPARATOR}")
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
            GemVersion next = gemVersionFromGradleIvyRequirement(details.requested.version)
            versions[details.requested.name] = next
            logger.debug("${configuration}      nothing collected")
            logger.debug("${configuration}      resolved  ${next}")
        }
    }

    String toString() {
        return "GemVersionResolver${versions}"
    }
}
