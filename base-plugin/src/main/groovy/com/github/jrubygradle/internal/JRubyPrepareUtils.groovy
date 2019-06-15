package com.github.jrubygradle.internal

import com.github.jrubygradle.JRubyPrepare
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.UnknownTaskException
import org.ysb33r.grolifant.api.TaskProvider

import static com.github.jrubygradle.JRubyPlugin.DEFAULT_CONFIGURATION
import static com.github.jrubygradle.JRubyPlugin.DEFAULT_PREPARE_TASK
import static com.github.jrubygradle.JRubyPlugin.TASK_GROUP_NAME

/** Utilities to deal with JRubPrepare tasks
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class JRubyPrepareUtils {

    /** JRubyPrepare task name by convention
     *
     * @param configurationName Name of a GEM configuration.
     * @return Associated task name.
     */
    static String taskName(String configurationName) {
        configurationName == DEFAULT_CONFIGURATION ? DEFAULT_PREPARE_TASK :
            "jrubyPrepare${configurationName.capitalize()}"
    }

    /** GEM working (unpack) relative path by convention.
     *
     * @param configurationName  Name of a GEM configuration.
     * @return Associated relative path to project directory.
     */
    static String gemRelativePath(String configurationName) {
        configurationName == DEFAULT_CONFIGURATION ? '.gems' :
            ".gems-${configurationName}"
    }

    /** Registers (or creates) a JRUbyPrepare tasks based upon a configuration name
     *
     * @param project Associated project
     * @param configurationName
     * @return Grolifant {@code TaskProvider}.
     */
    static TaskProvider<JRubyPrepare> registerPrepareTask(
        final Project project,
        final String configurationName
    ) {
        final String taskName = taskName(configurationName)
        final String gemDir = gemRelativePath(configurationName)

        try {
            TaskProvider.taskByName(project, taskName)
        } catch (UnknownTaskException e) {
            TaskProvider<JRubyPrepare> prepare = TaskProvider.registerTask(project, taskName, JRubyPrepare)
            Action<JRubyPrepare> configurator = new Action<JRubyPrepare>() {
                void execute(JRubyPrepare jp) {
                    jp.with {
                        group = TASK_GROUP_NAME
                        description = "Prepare the gems/jars from the `${configurationName}` dependencies"
                        dependencies(project.configurations.getByName(configurationName))
                        outputDir { "${project.buildDir}/${gemDir}" }
                    }
                }
            }
            prepare.configure(configurator as Action<? extends Task>)
            prepare
        }

    }
}