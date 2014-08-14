package com.lookout.gradle.jruby

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.artifacts.repositories.*
import org.gradle.api.tasks.bundling.War

import org.gradle.testfixtures.ProjectBuilder

import org.junit.*
import org.junit.Assert
import static org.junit.Assert.*


class JRubyPluginTest {
    def project

    @Before
    void setUp() {
        project = ProjectBuilder.builder().build()
        project.apply plugin: 'jruby'
    }

    @Test
    public void jrubyPluginAddsGemTasks() {
        assertTrue(project.tasks.jrubyCacheGems instanceof AbstractCopyTask)
        assertTrue(project.tasks.jrubyPrepareGems instanceof Task)
    }

    @Test
    public void jrubyPluginAddsJarTasks() {
        assertTrue(project.tasks.jrubyCacheJars instanceof AbstractCopyTask)
    }

    @Test
    public void jrubyPluginAddsPrimaryTasks() {
        assertTrue(project.tasks.jrubyPrepare instanceof Task)
        assertTrue(project.tasks.jrubyWar instanceof War)
        assertTrue(project.tasks.jrubyClean instanceof Delete)
    }

    @Test
    public void jrubyPluginSetsRepositoriesCorrectly() {
        assertTrue(hasRepositoryUrl(project, 'http://rubygems-proxy.torquebox.org/releases'))
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

    //
    //  Helper methods for testing
    ////////////////////////////////////////////////////////////////////////////

    private boolean hasRepositoryUrl(Project p, String url) {
        boolean result = false
        p.repositories.each { ArtifactRepository r ->
            if (r.url.toString() == url) {
                result = true
            }
        }
        return result
    }
}
