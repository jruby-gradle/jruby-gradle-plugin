package com.github.jrubygradle.internal

import org.gradle.api.artifacts.DependencyResolveDetails

class GemVersionResolver {

    private Map versions = [:]
  
    void resolve(DependencyResolveDetails details) {
        if (details.requested.group == 'rubygems') {
            def version = versions[details.requested.name]
            if (version != null) {
                def next = version.intersect( details.requested.version )
                if (next.conflict()) {
                    throw new RuntimeException( "there is no overlap for ${versions[details.requested.name]} and ${details.requested.version}" )
                }
                versions[details.requested.name] = next
                details.useVersion next.toString()
            }
            else {
                versions[details.requested.name] = new GemVersion(details.requested.version)
            }
        }
    }

    String toString(){
        "GemVersionResolver${versions}"
    }
}
