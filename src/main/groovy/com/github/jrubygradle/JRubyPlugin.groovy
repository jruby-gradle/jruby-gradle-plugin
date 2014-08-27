package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecDelegate
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.War

class JRubyPlugin implements Plugin<Project> {
    static final String TASK_GROUP_NAME = 'JRuby'

    static final String RUBYGEMS_RELEASE_URL = 'http://rubygems-proxy.torquebox.org/releases'

    void apply(Project project) {
        // REMOVE: project.apply plugin: 'java'

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
// MOVE:       project.configurations.create(JRubyJar.JRUBYJAR_CONFIG)
        JRubyExecDelegate.addToProject(project)

        // In order for jrubyWar to work we'll need to pull in the warbler
        // bootstrap code from this artifact
        project.afterEvaluate {
            if (project.jruby.defaultRepositories) {
                project.repositories {
                    jcenter()
                    rubygemsRelease()
                }
            }



            JRubyExec.updateJRubyDependencies(project)
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
    }

}
