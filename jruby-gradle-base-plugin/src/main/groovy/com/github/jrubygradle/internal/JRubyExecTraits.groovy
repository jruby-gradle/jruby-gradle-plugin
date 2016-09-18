package com.github.jrubygradle.internal

import com.github.jrubygradle.GemUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.internal.FileUtils
import org.gradle.util.CollectionUtils
import org.ysb33r.gradle.olifant.StringUtils

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
@SuppressWarnings(['MethodName', 'UnnecessaryGetter'])
trait JRubyExecTraits {
    final List<String> filterEnvKeys = ['GEM_PATH', 'RUBY_VERSION', 'GEM_HOME']

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

    void configuration(Configuration newConfiguration) {
        configuration(newConfiguration.name)
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
    void script(Object scr) {
        setScript(scr)
    }

    /** Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    void setScript(Object scr) {
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

    /**
     * Prepare the Ruby and Java dependencies for the configured configuration
     *
     * This method will determine the appropriate dependency overwrite behavior
     * from the Gradle invocation. In effect, if the --refresh-dependencies flag
     * is used, already installed gems will be overwritten.
     *
     * @param project The currently executing project
     */
    void prepareDependencies(Project project) {
        GemUtils.OverwriteAction overwrite = GemUtils.OverwriteAction.SKIP

        if (project.gradle.startParameter.refreshDependencies) {
            overwrite = GemUtils.OverwriteAction.OVERWRITE
        }

        prepareDependencies(project, overwrite)
    }

    /** Prepare dependencies with a custom overwrite behavior */
    void prepareDependencies(Project project, GemUtils.OverwriteAction overwrite) {
        Configuration execConfiguration = project.configurations.findByName(configuration)

        File gemDir = getGemWorkDir().absoluteFile

        gemDir.mkdirs()

        GemUtils.extractGems(
                project,
                execConfiguration,
                execConfiguration,
                gemDir,
                overwrite
        )
        GemUtils.setupJars(
                execConfiguration,
                gemDir,
                overwrite
        )
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

    @CompileDynamic
    Map getPreparedEnvironment(Map env) {
        Map<String, Object> preparedEnv = [:]

        preparedEnv.putAll(env.findAll { Object rawkey, Object value ->
            String key = StringUtils.stringize(rawkey)
            inheritRubyEnv || !(key in filterEnvKeys || key.matches(/rvm.*/))
        })

        preparedEnv.putAll([
                'JBUNDLE_SKIP' : true,
                'JARS_SKIP' : true,
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
        return this.gemWorkDir ? project.file(this.gemWorkDir) : null
    }

    @CompileDynamic
    /* collectMany is literally wrong here, stupid codenarc */
    @SuppressWarnings('UseCollectMany')
    List<String> _convertScriptArgs() {
        return CollectionUtils.stringize(
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
            }.flatten()
        )
    }

    @CompileDynamic
    List<String> _convertJrubyArgs() {
        return CollectionUtils.stringize(this.jrubyArgs)
    }

    /** Return the computed `PATH` for the task */
    String getComputedPATH(String originalPath) {
        return JRubyExecUtils.prepareWorkingPath(getGemWorkDir(), originalPath)
    }

    @CompileDynamic
    private File tmpGemDir() {
        String ext = FileUtils.toSafeFileName(configuration)
        return new File(project.buildDir, "tmp/${ext}")
    }

    private Object script
    private Object gemWorkDir
    private final List<Object> scriptArgs = []
    private final List<Object> jrubyArgs = []
}

