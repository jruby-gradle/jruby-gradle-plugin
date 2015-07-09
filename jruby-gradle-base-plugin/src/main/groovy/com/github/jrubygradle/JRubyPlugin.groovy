package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecDelegate
import com.github.jrubygradle.internal.GemVersionResolver
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
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
        project.configurations.create('gems')
        project.configurations.create(JRubyExec.JRUBYEXEC_CONFIG)
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

        Task jrpg = project.tasks.create 'jrubyPrepareGems'
        jrpg.dependsOn 'jrubyPrepare'
        jrpg << { logger.info "'jrubyPrepareGems' is deprecated and will be removed in a future version. Use 'jrubyPrepare' instead." }
        jrpg.group 'Deprecated'
        jrpg.description "Prepare the gems/jars from the `gem` dependencies, extracts the gems into jruby.installGemDir and sets up the jars in jruby.installGemDir/jars"

        project.task('jrubyPrepareJars', type: JRubyPrepareJars) {
            logger.info 'Obsolete tasks - does nothing anymore.'
            outputDir project.jruby.gemInstallDir
        }

        project.task('jrubyPrepare', type: JRubyPrepare) {
            group TASK_GROUP_NAME
            description 'Prepare the gems/jars from the `gem` dependencies, extracts the gems into jruby.installGemDir and sets up the jars in jruby.installGemDir/jars'
            gems project.configurations.gems
            outputDir project.jruby.gemInstallDir
        }

        project.task('jrubyGenerateGradleRb', type: GenerateGradleRb) {
            group TASK_GROUP_NAME
            description 'Generate a gradle.rb stub for executing Ruby binstubs'
            dependsOn project.tasks.jrubyPrepare
        }
    }
}
