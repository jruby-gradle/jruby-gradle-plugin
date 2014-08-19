package com.lookout.jruby

import org.gradle.api.*
import org.gradle.api.tasks.*
import org.gradle.api.artifacts.repositories.*
import org.gradle.api.tasks.bundling.War

import org.gradle.testfixtures.ProjectBuilder

import org.junit.*

import static org.junit.Assert.*


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
        project.apply plugin: 'com.lookout.jruby'
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
    public void jrubyPluginCustomGemRepoUrl() {
        def url = 'http://junit.maven/releases'
        project.jruby.defaultGemRepo = url
        assertTrue(hasRepositoryUrl(project, url))
    }

    @Test
    public void jrubyPluginExtractSkipsExtracted() {
    }

    @Test
    public void jrubyPluginConvertGemFileNameToGemName() {
        String filename = "rake-10.3.2.gem"
        String gem_name = "rake-10.3.2"

        assertEquals(gem_name, GemUtils.gemFullNameFromFile(filename))
    }

    // NOTE: This test will fail if no network is available
    // It really needs Spock's @IgnoreIf annotation
    // See https://gist.github.com/ysb33r/74574a45c67c9e9e8187
    @Test
    public void jrubyWarTaskNeedsToAddJrubyCompleteJar() {
        final String useVersion = '1.7.3'
        assertNotEquals "We need the test version to be different from the defaultVersion that is compiled in",
                useVersion,project.jruby.defaultVersion

        project.jruby {
            defaultVersion useVersion
        }

        Task jrw = project.tasks.jrubyWar
        new File(TESTROOT,'libs').mkdirs()
        new File(TESTROOT,'classes/main').mkdirs()
        project.buildDir = TESTROOT
        project.evaluate()
        jrw.copy()

        assertTrue jrw.outputs.files.singleFile.exists()

        def item = project.configurations.jrubyWar.files.find { it.toString().find('jruby-complete') }
        assertNotNull 'Would expected to have a jruby-complete-XXX.jar',item

        def matches = item =~ /.*(jruby-complete-.+.jar)/
        assertNotNull 'jruby-complete-XXX.jar did not match', matches
        assertEquals "jruby-complete-${useVersion}.jar".toString(), matches[0][1]

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
