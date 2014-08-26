package com.github.jrubygradle

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.artifacts.repositories.*
import org.gradle.api.tasks.bundling.*

import org.gradle.testfixtures.ProjectBuilder

import org.junit.*

import static org.junit.Assert.*
import static org.gradle.api.logging.LogLevel.*


class JRubyPluginTest {
    static final File TESTROOT = new File(System.getProperty('TESTROOT') ?: 'build/tmp/test/unittests')

    def project

    @Before
    void setUp() {
        if(TESTROOT.exists()) {
            TESTROOT.deleteDir()
        }
        TESTROOT.mkdirs()
        project = ProjectBuilder.builder().build()
        project.logging.level = LIFECYCLE
        project.apply plugin: 'com.github.jruby-gradle.base'
    }

    @Test
    public void jrubyPluginAddsGemTasks() {
        assertTrue(project.tasks.jrubyPrepareGems instanceof JRubyPrepareGems)
    }

    @Test
    public void jrubyPluginAddsJarTasks() {
        assertTrue(project.tasks.jrubyCacheJars instanceof AbstractCopyTask)
    }

    @Test
    public void jrubyPluginAddsPrimaryTasks() {
        assertTrue(project.tasks.jrubyPrepare instanceof Task)
        assertTrue(project.tasks.jrubyWar instanceof War)
        assertTrue(project.tasks.jrubyJar instanceof Jar)
        assertTrue(project.tasks.jrubyClean instanceof Delete)
    }

    @Test
    public void jrubyPluginSetsRepositoriesCorrectly() {
        project.evaluate()
        assertTrue(hasRepositoryUrl(project, 'http://rubygems-proxy.torquebox.org/releases'))
    }

    @Test
    public void jrubyPluginConvertGemFileNameToGemName() {
        String filename = "rake-10.3.2.gem"
        String gem_name = "rake-10.3.2"

        assertEquals(gem_name, GemUtils.gemFullNameFromFile(filename))
    }

    @Test
    public void defaultRubyGemRepository() {
        project.evaluate()
        assertTrue project.repositories.metaClass.respondsTo(project.repositories,'rubygemsRelease') != null
        def repo = project.repositories.rubygemsRelease()
        assertTrue(hasRepositoryUrl(project, 'http://rubygems-proxy.torquebox.org/releases'))
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
