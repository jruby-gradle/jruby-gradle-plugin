package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecDelegate
import com.github.jrubygradle.internal.GemVersionResolver
import com.github.jrubygradle.internal.JRubyExecUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 *
 */
class JRubyPlugin implements Plugin<Project> {
    static final String TASK_GROUP_NAME = 'JRuby'

    static final String RUBYGEMS_RELEASE_URL = 'http://rubygems.lasagna.io/proxy/maven/releases'

    void apply(Project project) {
        project.extensions.create('jruby', JRubyPluginExtension, project)

        if (!project.repositories.metaClass.respondsTo(project.repositories, 'rubygemsRelease')) {
            project.repositories.metaClass.rubygemsRelease << { ->
                maven { url RUBYGEMS_RELEASE_URL }
            }
        }

        // Set up a special configuration group for our embedding jars
        project.configurations.create('gems')
        project.configurations.create(JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG)
        JRubyExecDelegate.addToProject(project)

        project.afterEvaluate {
            if (project.jruby.defaultRepositories) {
                project.repositories {
                    jcenter()
                    rubygemsRelease()
                }
            }
            GemVersionResolver.setup(project)
            JRubyExec.updateJRubyDependencies(project)
        }

        project.task('jrubyPrepare', type: JRubyPrepare) {
            group TASK_GROUP_NAME
            description 'Prepare the gems/jars from the `gem` dependencies, extracts the gems into jruby.installGemDir and sets up the jars in jruby.installGemDir/jars'
            dependencies project.configurations.gems
            outputDir project.jruby.gemInstallDir
        }

        project.task('generateGradleRb', type: GenerateGradleRb) {
            group TASK_GROUP_NAME
            description 'Generate a gradle.rb stub for executing Ruby binstubs'
            dependsOn project.tasks.jrubyPrepare
        }
    }
}
