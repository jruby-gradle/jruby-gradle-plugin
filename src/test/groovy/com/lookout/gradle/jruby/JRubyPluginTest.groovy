package com.lookout.gradle.jruby

import org.gradle.api.Project
import org.gradle.api.Task

import org.gradle.testfixtures.ProjectBuilder

import org.junit.Test
import org.junit.Assert
import static org.junit.Assert.*


class JRubyPluginTest {
    @Test
    public void jrubyPluginAddsTask() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'jruby'

        assertTrue(project.tasks.hello instanceof Task)
    }
}
