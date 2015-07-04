package com.github.jrubygradle.internal

import org.gradle.api.Project
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.Configuration
import org.gradle.api.logging.Logger


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
    }

    // keep it not private for testing
    void firstRun() {
        log({"${configuration.name}\n" +
             "                       --------------------------\n" +
             "                       collect version range info\n" +
             "                       --------------------------"})
        Object config = this.configuration.copyRecursive()
        this.versions = [:]
        config.resolutionStrategy {
            eachDependency { this.resolve(it) }
        }
        config.resolvedConfiguration
        log({"${configuration.name}\n" +
             "                       ------------------------\n" +
             "                       apply version range info\n" +
             "                       ------------------------"})
    }

    void resolve(DependencyResolveDetails details) {
        if (details.requested.group == 'rubygems') {
            if (versions == null) {
                firstRun()
            }
            log( { "${configuration.name}: gem ${details.requested.name} ${details.requested.version}" } )
            def version = versions[details.requested.name]
            if (version != null) {
                def next = version.intersect( details.requested.version )
                if (next.conflict()) {
                    throw new RuntimeException( "there is no overlap for ${versions[details.requested.name]} and ${details.requested.version}" )
                }
                versions[details.requested.name] = next
                log( { "${configuration.name}      collected ${version}" } )
                log( { "${configuration.name}      resolved  ${next}" } )
                details.useVersion next.toString()
            }
            else {
                def next = new GemVersion(details.requested.version)
                versions[details.requested.name] = next
                log( { "${configuration.name}      nothing collected" } )
                log( { "${configuration.name}      resolved  ${next}" } )
            }
        }
    }

    void log(Closure message) {
        if (this.logger.isDebugEnabled()) {
            this.logger.debug(message.call())
        }
    }

    String toString(){
        "GemVersionResolver${versions}"
    }
}
