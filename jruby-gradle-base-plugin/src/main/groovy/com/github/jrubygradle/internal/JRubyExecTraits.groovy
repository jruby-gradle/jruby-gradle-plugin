package com.github.jrubygradle.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.internal.FileUtils
import org.gradle.util.CollectionUtils

/** Provides common traits for JRuby script execution across the {@code JRubyExec}
 * task and {@project.jrubyexec} extension.
 *
 * This trait is primarily meant as a plugin-internal interface/implementation which allows
 * for the asy set up and invocation of a JRuby environment unlike {@code JRubyExec}
 * it is not meant to directly set up or execute a Ruby script.
 *
 * It's functions are primarily:
 *   * Prepare Ruby gem dependencies
 *   * Prepare JVm (jar) dependencies
 *   * Set up the execution environment
 *
 * After that, it is up to the actual subclass extending JRubyExecAbstractTask to
 * decide how it wants to execute JRuby
 *
 * @author Schalk W. Cronjé
 * @since 0.1.18
 */
@CompileStatic
trait JRubyExecTraits {
    final List<String>  FILTER_ENV_KEYS = ['GEM_PATH', 'RUBY_VERSION', 'GEM_HOME']

    /** Allow JRubyExec to inherit a Ruby env from the shell (e.g. RVM)
     *
     * @since 0.1.10 (Moved from {@code JRubyExec})
     */
    @Optional
    @Input
    boolean inheritRubyEnv = false

    /** Configuration to copy gems from. If {@code jRubyVersion} has not been set, {@code jRubyExec} will used as
     * configuration. However, if {@code jRubyVersion} has been set, not gems will be used unless an explicit
     * configuration has been provided
     */
    private String configuration = JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG

    @Input
    String getConfiguration() {
        return configuration
    }

    @CompileDynamic
    void configuration(String newConfiguration) {
        if (project instanceof Project) {
            project.configurations.maybeCreate(newConfiguration)
        }
        configuration = newConfiguration
    }

    /** Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    void script(def scr) {
        setScript(scr)
    }

    /** Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    void setScript(def scr) {
        this.script = scr
    }

    /** Set alternative GEM unpack directory to use
     *
     * @param workingDir New working directory (convertible to file)
     */
    void gemWorkDir(Object workingDir) {
        gemWorkDir = workingDir
    }

    /**
     * Returns the directory that will be used to unpack Gems into
     *
     * @return Target directory
     * @since 0.1.9
     */
    @Optional
    @Input
    @CompileDynamic
    File getGemWorkDir() {
        if (gemWorkDir) {
            return project.file(gemWorkDir)
        }
        return tmpGemDir()
    }

    /** Add arguments for script
     *
     * @param args Arguments to be aqdded to script arguments
     */
    void scriptArgs(Object... args) {
        this.scriptArgs.addAll(args as List)
    }

    /** Add arguments for script in a closure style
     *
     * @param args Arguments to be aqdded to script arguments
     */
    void scriptArgs(Closure c) {
        this.scriptArgs << c
    }

    /** Add arguments for jruby
     *
     * @param args Additional arguments to be passed to JRuby itself.
     */
    void jrubyArgs(Object... args) {
        this.jrubyArgs.addAll(args as List)
    }


    File _convertScript() {
        switch (this.script) {
            case null:
                return null
            case File:
                return this.script as File
            case String:
                return new File(this.script as String)
            default:
                return new File(this.script.toString())
        }
    }

    Map getPreparedEnvironment(Map env) {
        Map<String, Object> preparedEnv = [:]

        preparedEnv.putAll(env.findAll { String key, Object value ->
            inheritRubyEnv || !(key in FILTER_ENV_KEYS || key.matches(/rvm.*/))
        })

        preparedEnv.putAll([
                'JBUNDLE_SKIP' : 'true',
                'JARS_SKIP' : 'true',
                'PATH' : getComputedPATH(System.getenv().get(JRubyExecUtils.pathVar())),
                'GEM_HOME' : getGemWorkDir().absolutePath,
                'GEM_PATH' : getGemWorkDir().absolutePath,
                'JARS_HOME' : new File(getGemWorkDir().absolutePath, 'jars'),
                'JARS_LOCK' : new File(getGemWorkDir().absolutePath, 'Jars.lock')
        ])

        return preparedEnv
    }

    // Internal functions intended to be used by plugin itself.

    File _convertGemWorkDir(Project project) {
        this.gemWorkDir ? project.file(this.gemWorkDir) : null
    }

    @CompileDynamic
    List<String> _convertScriptArgs() {
        CollectionUtils.stringize(
            this.scriptArgs.collect { arg ->
                /* In order to support closures in scriptArgs for lazy
                 * evaluation, we need to evaluate the closure if it is present
                 */
                if (arg instanceof Closure) {
                    (arg as Closure).call()
                }
                else {
                    arg
                }
            } .flatten()
        )
    }

    @CompileDynamic
    List<String> _convertJrubyArgs() {
        CollectionUtils.stringize(this.jrubyArgs)
    }

    /** Return the computed `PATH` for the task */
    String getComputedPATH(String originalPath) {
        JRubyExecUtils.prepareWorkingPath(getGemWorkDir(), originalPath)
    }

    @CompileDynamic
    private File tmpGemDir() {
        String ext = FileUtils.toSafeFileName(configuration)
        return new File(project.buildDir, "tmp/${ext}")
    }

    private Object script
    private Object gemWorkDir
    private List<Object> scriptArgs = []
    private List<Object> jrubyArgs = []
}

