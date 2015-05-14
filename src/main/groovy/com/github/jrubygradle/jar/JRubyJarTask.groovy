package com.github.jrubygradle.jar

import com.github.jrubygradle.jar.internal.JRubyDirInfo

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.StopExecutionException

/**
 * @author Christian Meier
 */
class JRubyJar extends Jar {

    JRubyJar() {
        appendix = 'jruby'
    }
}
