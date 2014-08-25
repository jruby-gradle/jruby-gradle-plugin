package com.lookout.jruby

import com.lookout.jruby.internal.JRubyExecDelegate
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.War

class JRubyPlugin implements Plugin<Project> {
    static final String TASK_GROUP_NAME = 'JRuby'

    static final String RUBYGEMS_RELEASE_URL = 'http://rubygems-proxy.torquebox.org/releases'

    void apply(Project project) {
        project.apply plugin: 'java'
        project.extensions.create('jruby', JRubyPluginExtension, project)

        if (!project.repositories.metaClass.respondsTo(project.repositories, 'rubygemsRelease')) {
            project.repositories.metaClass.rubygemsRelease << { ->
                maven { url RUBYGEMS_RELEASE_URL }
            }
        }

        // Set up a special configuration group for our embedding jars
        project.configurations {
            jrubyEmbeds
            gems
        }

        project.configurations.create(JRubyExec.JRUBYEXEC_CONFIG)
        project.configurations.create(JRubyWar.JRUBYWAR_CONFIG)
        project.configurations.create(JRubyJar.JRUBYJAR_CONFIG)
        JRubyExecDelegate.addToProject(project)

        // In order for jrubyWar to work we'll need to pull in the warbler
        // bootstrap code from this artifact
        project.afterEvaluate {
            if (project.jruby.defaultRepositories) {
                project.repositories {
                    jcenter()
                    rubygemsRelease()

                    // Required to pull in our warbler-bootstrap dependency
                    maven { url 'http://dl.bintray.com/rtyler/jruby' }
                }
            }

            project.dependencies {
                jrubyEmbeds group: 'com.lookout', name: 'warbler-bootstrap', version: '1.+'
            }

            // In order to update the testing cycle we need to tell
            project.tasks.test {
                environment GEM_HOME : project.extensions.getByName('jruby').gemInstallDir
                dependsOn 'jrubyPrepareGems'
            }

            JRubyExec.updateJRubyDependencies(project)
            JRubyWar.updateJRubyDependencies(project)
        }

        project.task('jrubyClean', type: Delete) {
            group TASK_GROUP_NAME
            description 'Clean up the temporary dirs used by the JRuby plugin'
            mustRunAfter 'clean'
            delete '.jarcache/'
        }

        project.task('jrubyPrepareGems', type: JRubyPrepareGems) {
            group TASK_GROUP_NAME
            description 'Prepare the gems from the `gem` dependencies, extracts into jruby.installGemDir'
            gems project.configurations.gems
            outputDir project.jruby.gemInstallDir
        }

        project.task('jrubyCacheJars', type: Copy) {
            group TASK_GROUP_NAME
            description 'Cache .jar-based dependencies into .jarcache/'

            from project.configurations.jrubyWar
            into ".jarcache"
            include '**/*.jar'
        }

        project.task('jrubyPrepare') {
            group TASK_GROUP_NAME
            description 'Pre-cache and prepare all dependencies (jars and gems)'
            dependsOn project.tasks.jrubyCacheJars, project.tasks.jrubyPrepareGems
        }

        // Only jRubyWar will depend on jrubyPrepare. Other JRubyWar tasks created by
        // build script authors will be under their own control
        // jrubyWar task will use jrubyWar as configuration
        project.task('jrubyWar', type: JRubyWar) {
            group JRubyPlugin.TASK_GROUP_NAME
            description 'Create a JRuby-based web archive'
            dependsOn project.tasks.jrubyPrepare
            classpath project.configurations.jrubyWar
        }

        project.task('jrubyJar', type: JRubyJar) {
            group JRubyPlugin.TASK_GROUP_NAME
            dependsOn project.tasks.jrubyPrepare
            dependsOn project.tasks.classes
        }
    }

}
