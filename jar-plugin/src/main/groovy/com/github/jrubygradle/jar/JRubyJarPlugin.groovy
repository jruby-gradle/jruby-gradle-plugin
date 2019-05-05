package com.github.jrubygradle.jar

import static com.github.jrubygradle.jar.JRubyJar.DEFAULT_JRUBYJAR_CONFIG

import groovy.transform.PackageScope
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.testing.Test

/**
 * @author Schalk W. Cronjé
 * @author Christian Meier
 */
class JRubyJarPlugin implements Plugin<Project> {
    private static final String TEST_TASK_NAME = 'test'

    void apply(Project project) {
        project.apply plugin: 'com.github.jruby-gradle.base'
        project.apply plugin: 'java-base'
        project.configurations.maybeCreate(DEFAULT_JRUBYJAR_CONFIG)
        project.tasks.create('jrubyJar', JRubyJar)

        updateTestTask(project)

        project.afterEvaluate {
            checkJRubyVersions(project)
        }
    }

    /**
     * Check our configured jruby versions to see if any of them are old enough
     * to cause problems with a packed jar
     * <https://github.com/jruby-gradle/jruby-gradle-plugin/issues/191>
     */
    void checkJRubyVersions(Project project) {
        project.tasks.each { Task task ->
            if ((task instanceof JRubyJar) && (task.scriptName != JRubyJar.Type.LIBRARY)) {
                if (isJRubyVersionDeprecated(task.jrubyVersion)) {
                    project.logger.warn('The task `{}` is using JRuby {} which may cause unexpected behavior, see <http://jruby-gradle.org/errors/jar-deprecated-jrubyversion> for more',
                            task.name, task.jrubyVersion)
                }
            }
        }
    }

    /**
     * Determine whether the version of the JRuby provided is deprecated as far
     * as the jar plugin is concerned. Deprecated means that the version is unlikely
     * to produce a useful artifact due to missing functionality in JRuby core
     *
     * @param version
     * @return True if we consider this version deprecated/problematic for the jar plugin
     */
    boolean isJRubyVersionDeprecated(String version) {
        return (version.matches(/1.7.1(\d+)/)) as boolean
    }

    @PackageScope
    void updateTestTask(Project project) {
        // In order to update the testing cycle we need to tell unit tests where to
        // find GEMs. We are assuming that if someone includes this plugin, that they
        // will be writing tests that includes jruby and that they might need some
        // GEMs as part of the tests.
        Closure testConfiguration = { Task t ->
            environment GEM_HOME: project.jruby.gemInstallDir
            environment JARS_HOME: project.jruby.jarInstallDir
            dependsOn 'jrubyPrepare'
        }

        try {
            Task t = project.tasks.getByName(TEST_TASK_NAME)
            if (t instanceof Test) {
                project.configure(t, testConfiguration)
            }
        }
        catch (UnknownTaskException) {
            project.tasks.whenTaskAdded { Task t ->
                if (t.name == TEST_TASK_NAME && t instanceof Test) {
                    project.configure(t, testConfiguration)
                }
            }
        }
    }
}
