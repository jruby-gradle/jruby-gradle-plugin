package com.lookout.gradle.jruby

import org.gradle.api.*
import org.gradle.api.tasks.*

import org.gradle.testfixtures.ProjectBuilder

import org.junit.Test
import org.junit.Assert
import static org.junit.Assert.*


class JRubyPluginTest {
    @Test
    public void jrubyPluginAddsGemTasks() {
        Project project = ProjectBuilder.builder().build()
        project.configurations { runtime }
        project.apply plugin: 'jruby'

        assertTrue(project.tasks.cachegems instanceof AbstractCopyTask)
        assertTrue(project.tasks.preparegems instanceof Task)
    }

    @Test
    public void jrubyPluginExtractSkipsExtracted() {
    }

    @Test
    public void jrubyPluginConvertGemFileNameToGemName() {
        String filename = "rake-10.3.2.gem"
        String gem_name = "rake-10.3.2"

        assertEquals(gem_name, JRubyPlugin.gemFullNameFromFile(filename))
    }
}
