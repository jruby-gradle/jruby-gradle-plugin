package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecTraits
import com.github.jrubygradle.internal.JRubyExecUtils
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.process.JavaExecSpec

/** Runs a ruby script using JRuby
 *
 * @author Schalk W. Cronjé
 */
class JRubyExec extends JavaExec implements JRubyExecTraits {
    static String jarDependenciesGemLibPath(File gemDir) {
        new File(gemDir, "gems/jar-dependencies-${JRubyExecUtils.JAR_DEPENDENCIES_VERSION}/lib").absolutePath
    }

    /**
     * Ensure that our JRuby depedencies are updated properly for the default jrubyExec configuration
     * and all other JRubyExec tasks
     *
     * This function also ensures that we have a proper version of jar-dependencies
     * on older versions of JRuby so jar requires work properly on those version
     *
     * @param project
     * @since 1.0.0
     */
    static void updateJRubyDependencies(Project project) {
        JRubyExecUtils.updateJRubyDependenciesForConfiguration(project,
                JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG,
                project.jruby.execVersion)

        project.tasks.withType(JRubyExec) { JRubyExec task ->
            /* Only update non-default configurations */
            if (task.configuration != JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG) {
                JRubyExecUtils.updateJRubyDependenciesForConfiguration(project,
                        task.configuration,
                        task.jrubyVersion)
            }
        }
    }

    JRubyExec() {
        super()
        super.setMain 'org.jruby.Main'

        try {
            project.configurations.getByName(JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG)
        } catch(UnknownConfigurationException ) {
            throw new TaskInstantiationException('Cannot instantiate a JRubyExec instance before jruby plugin has been loaded')
        }

        project.afterEvaluate { this.validateTaskConfiguration() }
    }

    /** Script to execute.
     * @return The path to the script (or nul if not set)
     */
    @Optional
    @Input
    File getScript() {
        _convertScript()
    }

    private String customJRubyVersion
    /** If it is required that a JRubyExec task needs to be executed with a different version of JRuby that the
     * globally configured one, it can be done by setting it here.
     */
    @Input
    String getJrubyVersion() {
        if (customJRubyVersion == null) {
            return project.jruby.execVersion
        }
        return customJRubyVersion
    }

    /** Setting the {@code jruby-complete} version allows for tasks to be run using different versions of JRuby.
     * This is useful for comparing the results of different version or running with a gem that is only
     * compatible with a specific version or when running a script with a different version that what will
     * be packaged.
     *
     * @param version String in the form '1.7.13'
     * @since 0.1.18
     */
    void jrubyVersion(final String ver) {
        setJrubyVersion(ver)
    }

    /** Setting the {@code jruby-complete} version allows for tasks to be run using different versions of JRuby.
     * This is useful for comparing the results of different version or running with a gem that is only
     * compatible with a specific version or when running a script with a different version that what will
     * be packaged.
     *
     * @param version String in the form '1.7.13'
     */
    void setJrubyVersion(final String version) {
        customJRubyVersion = version
        JRubyExecUtils.updateJRubyDependenciesForConfiguration(project, configuration, version)
    }

    /** Returns a list of script arguments
     */
    @Optional
    @Input
    List<Object> getScriptArgs() {
        _convertScriptArgs()
    }

    /** Returns a list of jruby arguments
     */
    @Optional
    @Input
    List<String> getJrubyArgs() {
        _convertJrubyArgs()
    }

    @Override
    void exec() {
        Configuration execConfiguration = project.configurations.findByName(configuration)
        logger.info("Executing with configuration: ${configuration}")
        prepareDependencies(project)

        setEnvironment getPreparedEnvironment(environment)
        super.classpath JRubyExecUtils.classpathFromConfiguration(execConfiguration)
        super.setArgs(getArgs())
        super.exec()
    }

    /** getArgs gets overridden in order to add JRuby options, script name and script arguments in the correct order.
     *
     * There are three modes of behaviour
     * <ul>
     *   <li> script set. no jrubyArgs, or jrubyArgs does not contain {@code -S} - Normal way to execute script. A check
     *   whether the script exists will be performed.
     *   <li> script set. jrubyArgs contains {@code -S} - If script is not absolute, no check will be performed to see
     *   if the script exists and will be assumed that the script can be found using the default ruby path mechanism.
     *   <li> script not set, but jrubyArgs set - Set up to execute jruby with no script. This should be a rarely used otion.
     * </ul>
     *
     * @throw {@code org.gradle.api.InvalidUserDataException} if mode of behaviour cannot be determined.
     */
    @Override
    List<String> getArgs() {
        // just add the extra load-path even if it does not exists
        List<String> extra = ['-I', jarDependenciesGemLibPath(getGemWorkDir())]
        JRubyExecUtils.buildArgs(extra, jrubyArgs, getScript(), scriptArgs)
    }

    @Override
    JavaExec setMain(final String mainClassName) {
        if (mainClassName == 'org.jruby.Main') {
            super.setMain(mainClassName)
        } else {
            throw notAllowed("Setting main class for JRuby to ${mainClassName} is not a valid operation")
        }
    }

    @Override
    JavaExec setArgs(Iterable<?> applicationArgs) {
        throw notAllowed('Use jvmArgs / scriptArgs instead')
    }

    @Override
    JavaExec args(Object... args) {
        throw notAllowed('Use jvmArgs / scriptArgs instead')
    }

    @Override
    JavaExecSpec args(Iterable<?> args) {
        throw notAllowed('Use jvmArgs / scriptArgs instead')
    }

    /** Verify that we are in a good configuration for execution */
    void validateTaskConfiguration() {
        if ((jrubyVersion != project.jruby.execVersion) &&
            (configuration == JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG)) {
            String message = """\
The \"${name}\" task cannot be configured wth a custom JRuby (${jrubyVersion})
and still use the default \"${JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG}\" configuration

Please see this page for more details: <http://jruby-gradle.org/errors/jrubyexec-version-conflict/>"""

            throw new ProjectConfigurationException(message)
        }
    }

    private static UnsupportedOperationException notAllowed(final String msg) {
        return new UnsupportedOperationException (msg)
    }
}
