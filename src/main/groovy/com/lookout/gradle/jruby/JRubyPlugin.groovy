package com.lookout.gradle.jruby

import org.gradle.api.Plugin
import org.gradle.api.Project

class JRubyPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.task('hello') << {
            println "world?"
        }
    }
}
