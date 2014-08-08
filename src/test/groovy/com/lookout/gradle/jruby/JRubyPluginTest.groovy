package com.lookout.gradle.jruby

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.War

import org.gradle.testfixtures.ProjectBuilder

import org.junit.Test
import org.junit.Assert
import static org.junit.Assert.*


class JRubyPluginTest {
    @Test
    public void jrubyPluginAddsGemTasks() {
        Project project = ProjectBuilder.builder().build()
        ['runtime', 'compile'].each { project.configurations.create(it) }
        project.apply plugin: 'jruby'

        assertTrue(project.tasks.jrubyCacheGems instanceof AbstractCopyTask)
        assertTrue(project.tasks.jrubyCacheJars instanceof AbstractCopyTask)

        assertTrue(project.tasks.jrubyPrepareGems instanceof Task)
        assertTrue(project.tasks.jrubyPrepare instanceof Task)

        assertTrue(project.tasks.jrubyWar instanceof War)
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
