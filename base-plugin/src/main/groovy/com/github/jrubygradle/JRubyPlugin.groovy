package com.github.jrubygradle

import com.github.jrubygradle.api.core.JRubyCorePlugin
import com.github.jrubygradle.internal.JRubyExecDelegate
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration

import static org.ysb33r.grolifant.api.TaskProvider.registerTask

/** Base plugin for JRuby.
 *
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 *
 */
@CompileStatic
class JRubyPlugin implements Plugin<Project> {
    public static final String TASK_GROUP_NAME = 'JRuby'
    public static final String DEFAULT_CONFIGURATION = 'gems'
    public static final String DEFAULT_PREPARE_TASK = 'jrubyPrepare'
    public static final String PROJECT_JRUBYEXEC = 'jrubyexec'

    void apply(Project project) {
        project.apply plugin: JRubyCorePlugin
        Configuration gems = project.configurations.create(DEFAULT_CONFIGURATION)

        JRubyPluginExtension jruby = project.extensions.create(
            JRubyPluginExtension.NAME,
            JRubyPluginExtension,
            project
        )
        jruby.gemConfiguration = gems

        JRubyExecDelegate.addToProject(project, PROJECT_JRUBYEXEC)

        registerTask(
            project,
            'generateGradleRb',
            GenerateGradleRb
        ).configure(generateGradleRbConfiguration(project))
    }

    private Action<? super Task> generateGradleRbConfiguration(Project project) {
        new Action<GenerateGradleRb>() {
            @Override
            void execute(GenerateGradleRb ggrb) {
                ggrb.with {
                    group = TASK_GROUP_NAME
                    description = 'Generate a gradle.rb stub for executing Ruby binstubs'
                    dependsOn DEFAULT_PREPARE_TASK
                    gemInstallDir { "${project.buildDir}/.gems" }
                }
            }
        } as Action<? super Task>
    }
}
