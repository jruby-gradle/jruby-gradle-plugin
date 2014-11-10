package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskInstantiationException
import org.gradle.internal.FileUtils
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.gradle.util.CollectionUtils

/** Runs a ruby script using JRuby
 *
 * @author Schalk W. Cronjé
 */
class JRubyExec extends JavaExec {

    static final String JRUBYEXEC_CONFIG = 'jrubyExec'

    static void updateJRubyDependencies(Project proj) {
        proj.dependencies {
            jrubyExec "org.jruby:jruby-complete:${proj.jruby.execVersion}"
        }

        proj.tasks.withType(JRubyExec) { t ->
            if (t.jrubyConfigurationName != proj.configurations.jrubyExec) {
                proj.dependencies.add(t.jrubyConfigurationName,
                                        "org.jruby:jruby-complete:${t.jrubyVersion}")
            }
        }
    }

    /** Script to execute.
     *
     */
    @Input
    File script

    /** Configuration to copy gems from. If {@code jRubyVersion} has not been set, {@code jRubyExec} will used as
     * configuration. However, if {@code jRubyVersion} has been set, not gems will be used unless an explicit
     * configuration has been provided
     *
     */
    @Optional
    @Input
    String configuration

    @Input
    String jrubyVersion

    /** Set the GEM directory to be used by the task. If not set, then an internal generated directory will be used.
     * The default behaviour is to allow each JRubyExec task to run in isolation from each other. By setting this
     * a script can allow different JRubyExec instances to utilise the same folder.
     *
     * @since 0.1.6
     */
    @OutputDirectory
    File gemWorkDir

    JRubyExec() {
        super()
        super.setMain 'org.jruby.Main'

        try {
            project.configurations.getByName(JRUBYEXEC_CONFIG)
        } catch(UnknownConfigurationException ) {
            throw new TaskInstantiationException('Cannot instantiate a JRubyExec instance before jruby plugin has been loaded')
        }

        jrubyVersion = project.jruby.execVersion
        jrubyConfigurationName = JRUBYEXEC_CONFIG
    }

    /** Sets the scriptname
     *
     * @param fName Path to script
     */
    void setScript(Object fName) {
        switch (fName) {
            case File:
                script=fName
                break
            case String:
                script = new File(fName)
                break
            default:
                script = new File(fName.toString())
        }
    }

    /** Sets the working directory used by the task for installing GEMs.
     * By default every task will use its own private directory, so that tasks
     * can be run in isolation avoiding potential clashes between different versions
     * of the same GEM. If the user requires multiple tasks to share the same GEM area
     * then this configuration property can be used to to provide such a folder..
     *
     * @param fName Path to script
     */
    void setGemWorkDir(Object fName) {
        switch (fName) {
            case File:
                gemWorkDir=fName
                break
            case String:
                gemWorkDir = new File(fName)
                break
            default:
                gemWorkDir = new File(fName.toString())
        }
    }

    /** Returns a list of script arguments
     */
    List<String> scriptArgs() {
        CollectionUtils.stringize(this.scriptArgs)
    }

    /** Set arguments for script
     *
     * @param args
     */
    void scriptArgs(Object... args) {
        this.scriptArgs.addAll(args as List)
    }

    /** Returns a list of jruby arguments
     */
    List<String> jrubyArgs() {
        CollectionUtils.stringize(this.jrubyArgs)
    }

    /** Set arguments for jruby
     *
     * @param args
     */
    void jrubyArgs(Object... args) {
        this.jrubyArgs.addAll(args as List)
    }

    /** Return the computed `PATH` for the task
     *
     */
    String getComputedPATH(String originalPath) {
        File path = new File(tmpGemDir(), 'bin')
        return path.absolutePath + File.pathSeparatorChar + originalPath
    }

    /** Setting the {@code jruby-complete} version allows for tasks to be run using different versions of JRuby.
     * This is useful for comparing the results of different version or running with a gem that is only
     * compatible with a specific version or when running a script with a different version that what will
     * be packaged.
     *
     * @param version String in the form '1.7.13'
     */
    void setJrubyVersion(final String version) {
        if (version == project.jruby.execVersion) {
            jrubyConfigurationName = JRUBYEXEC_CONFIG
        } else {
            final String cfgName= 'jrubyExec$$' + name
            project.configurations.maybeCreate(cfgName)
            jrubyConfigurationName = cfgName
        }
        jrubyVersion = version
    }

    @Override
    void exec() {
        if (configuration == null && jrubyConfigurationName == JRUBYEXEC_CONFIG) {
            configuration = JRUBYEXEC_CONFIG
        }

        GemUtils.OverwriteAction overwrite = project.gradle.startParameter.refreshDependencies ?  GemUtils.OverwriteAction.OVERWRITE : GemUtils.OverwriteAction.SKIP
        def jrubyCompletePath = project.configurations.getByName(jrubyConfigurationName)
        if(gemWorkDir == null) {
            gemWorkDir = tmpGemDir()
        }
        gemWorkDir.mkdirs()
        environment 'GEM_HOME' : gemWorkDir,
                    'PATH' : getComputedPATH(System.env.PATH)

        if (configuration != null) {
            GemUtils.extractGems(
                    project,
                    jrubyCompletePath,
                    project.configurations.getByName(configuration),
                    gemWorkDir,
                    overwrite
            )
        }

        super.classpath JRubyExecUtils.classpathFromConfiguration(project.configurations.getByName(jrubyConfigurationName))
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
        JRubyExecUtils.buildArgs(jrubyArgs,script,scriptArgs)
    }

    @Override
    JavaExec setMain(final String mainClassName) {
        if (mainClassName == 'org.jruby.Main') {
            super.setMain(mainClassName)
        } else {
            throw notAllowed("Setting main class for jruby to ${mainClassName} is not a valid operation")
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

    /** Returns the {@code Configuration} object this task is tied to
     */
    String getJrubyConfigurationName() {
        return this.jrubyConfigurationName
    }

    private static UnsupportedOperationException notAllowed(final String msg) {
        return new UnsupportedOperationException (msg)
    }

    private File tmpGemDir() {
        String ext = FileUtils.toSafeFileName(jrubyConfigurationName)
        if (configuration && configuration != jrubyConfigurationName) {
            ext= ext + "-${FileUtils.toSafeFileName(configuration)}"
        }
        new File( project.buildDir, "tmp/${ext}").absoluteFile
    }

    private String jrubyConfigurationName
    private List<Object>  jrubyArgs = []
    private List<Object>  scriptArgs = []

}



