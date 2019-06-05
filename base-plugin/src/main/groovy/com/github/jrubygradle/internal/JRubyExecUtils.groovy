package com.github.jrubygradle.internal

import com.github.jrubygradle.JRubyPlugin
import com.github.jrubygradle.JRubyPluginExtension
import com.github.jrubygradle.api.gems.GemOverwriteAction
import com.github.jrubygradle.api.gems.GemUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.ysb33r.grolifant.api.OperatingSystem
import org.ysb33r.grolifant.api.StringUtils

import java.util.regex.Matcher

import static com.github.jrubygradle.api.gems.GemOverwriteAction.OVERWRITE
import static com.github.jrubygradle.api.gems.GemOverwriteAction.SKIP
import static org.ysb33r.grolifant.api.StringUtils.stringize

/**
 * @author Schalk W. Cronjé.
 */
@CompileStatic
class JRubyExecUtils {

    public static final List<String> FILTERED_ENV_KEYS = ['GEM_PATH', 'RUBY_VERSION', 'GEM_HOME']
    public static final String DEFAULT_JRUBYEXEC_CONFIG = JRubyPlugin.DEFAULT_CONFIGURATION

    private static final String JRUBY_COMPLETE = 'jruby-complete'
    private static final String BINPATH_FLAG = '-S'

    /** Extract a list of files from a configuration that is suitable for a jruby classpath
     *
     * @param cfg Configuration to use
     * @return
     */
    static Set<File> classpathFromConfiguration(Configuration cfg) {
        return cfg.files.findAll { File f -> f.name.startsWith(JRUBY_COMPLETE) }
    }

    /** Extract the jruby-complete-XXX.jar classpath
     *
     * @param cfg Configuration
     * @return Returns the classpath as a File or null if the jar was not found
     */
    static File jrubyJar(Configuration cfg) {
        return cfg.files.find { it.name.startsWith(JRUBY_COMPLETE) }
    }

    /** Extracts the JRuby version number from a jruby-complete-XXX.jar filename
     *
     * @param jar JRuby Jar
     * @return Version string or null
     *
     * @since 0.1.9
     */
    @CompileDynamic
    static String jrubyJarVersion(final File jar) {
        Matcher matches = jar.name =~ /${JRUBY_COMPLETE}-(.+)\.jar/
        if (matches) {
            return matches[0][1]
        }
        return null
    }

    /** Extracts the JRuby version number as a triplet from a jruby-complete-XXX.jar filename
     *
     * @param jar JRuby Jar
     * @return Version string map [major,minor,patchlevel] or null
     *
     * @since 0.1.16* @deprecated This method is no longer used and will be removed in a later
     *  version
     */
    @CompileDynamic
    @Deprecated
    static Map jrubyJarVersionTriple(final File jar) {
        String version = jrubyJarVersion(jar)
        if (!version) {
            return [:]
        }

        Matcher matches = version =~ /(\d{1,2})\.(\d{1,3})\.(\d{1,3}).*/

        if (!matches.matches() || (matches[0].size() != 4)) {
            return [:]
        }

        return [
            major     : matches[0][1].toInteger(),
            minor     : matches[0][2].toInteger(),
            patchlevel: matches[0][3].toInteger()
        ]
    }

    /** Extract the jruby-complete-XXX.jar as a FileCollection
     *
     * @param cfg FileCollection
     * @return Returns the classpath as a File or null if the jar was not found
     */
    static FileCollection jrubyJar(FileCollection fc) {
        fc.filter { File f -> f.name.startsWith(JRUBY_COMPLETE) }
    }

    /** Resolves a script location object.
     *
     * @paream project Project context for script.
     * @param script Script to resolve.
     * @return Resolved script location. Will be {@code null} if {@code script == null},
     */
    static File resolveScript(Project project, Object script) {
        if (script) {
            File intermediate = script instanceof File ? (File) script : new File(stringize(script))
            if (intermediate.absolute) {
                intermediate
            } else {
                intermediate.parentFile ? project.file(script) : intermediate
            }
        } else {
            null
        }
    }

    /** Builds a list of arguments that can be used to run JRuby.
     *
     * @param jrubyArgs JRuby-specific arguments
     * @param script Script to run.
     * @param scriptArgs Script arguments
     * @return List of resolved arguments.
     */
    static List<String> buildArgs(List<Object> jrubyArgs, File script, List<Object> scriptArgs) {
        buildArgs([], jrubyArgs, script, scriptArgs)
    }

    /**
     * Construct the correct set of arguments based on the parameters to invoke jruby-complete.jar with
     *
     * @param extra
     * @param jrubyArgs
     * @param script
     * @param scriptArgs
     * @return sequential list of arguments to pass jruby-complete.jar
     */
    static List<String> buildArgs(List<Object> extra,
                                  List<Object> jrubyArgs,
                                  File script,
                                  List<Object> scriptArgs) {
        List<Object> cmdArgs = extra
        // load Jars.lock on startup
        cmdArgs.add('-rjars/setup')
        boolean hasInlineScript = jrubyArgs.contains('-e')
        boolean useBinPath = jrubyArgs.contains(BINPATH_FLAG)

        /* Default to adding the -S option if we don't have an expression to evaluate
         * <https://github.com/jruby-gradle/jruby-gradle-plugin/issues/152>
         */
        if (!hasInlineScript && script && !jrubyArgs.contains(BINPATH_FLAG)) {
            jrubyArgs.add(BINPATH_FLAG)
            useBinPath = true
        }

        cmdArgs.addAll(jrubyArgs)

        if (useBinPath && script != null) {
            if (script.isAbsolute() && (!script.exists())) {
                throw new InvalidUserDataException("${script} does not exist")
            }
            cmdArgs.add(script.toString())
        } else if (script == null) {
            if (useBinPath && (jrubyArgs.size() <= 1)) {
                throw new InvalidUserDataException('No `script` property defined and no inline script provided')
            }

            if (jrubyArgs.isEmpty()) {
                throw new InvalidUserDataException('Cannot build JRuby execution arguments with either `script` or `jrubyArgs` set')
            }
        }

        cmdArgs.addAll(scriptArgs)
        StringUtils.stringize(cmdArgs)
    }

    /** Get the name of the system search path environmental variable
     *
     * @return Name of variable
     * @since 0.1.11
     */
    static String pathVar() {
        OperatingSystem.current().pathVar
    }

    /** Create a search path that includes the GEM working directory
     *
     * @param gemWorkDir GEM work dir instance
     * @param originalPath The original platform-specific search path
     * @return A search suitable for the specific operating system the job will run on
     * @since 0.1.11
     */
    static String prepareWorkingPath(File gemWorkDir, String originalPath) {
        File path = new File(gemWorkDir, 'bin')
        return path.absolutePath + File.pathSeparatorChar + originalPath
    }

    /**
     * Update the given configuration on the project with the appropriate versions
     * of JRuby and supplemental dependencies to execute JRuby successfully
     */
    static void updateJRubyDependenciesForConfiguration(Project project, String configuration, String version) {
        Configuration c = project.configurations.findByName(configuration)

        /* Only define this dependency if we don't already have it */
        if (!(c.dependencies.find { it.name == JRUBY_COMPLETE })) {
            project.dependencies.add(configuration, "org.jruby:jruby-complete:${version}")
        }
    }

//    /**
//     * Prepare the Ruby and Java dependencies for the configured configuration
//     *
//     * This method will determine the appropriate dependency overwrite behavior
//     * from the Gradle invocation. In effect, if the --refresh-dependencies flag
//     * is used, already installed gems will be overwritten.
//     *
//     * @param project The currently executing project
//     */
//    static void prepareDependencies(Project project) {
//        GemOverwriteAction overwrite = SKIP
//
//        if (project.gradle.startParameter.refreshDependencies) {
//            overwrite = OVERWRITE
//        }
//
//        prepareDependencies(project, overwrite)
//    }

    static void prepareDependencies(
        Project project,
        File gemWorkDir,
        JRubyPluginExtension jruby,
        Configuration gemConfiguration
    ) {
        GemOverwriteAction overwrite = SKIP

        if (project.gradle.startParameter.refreshDependencies) {
            overwrite = OVERWRITE
        }

        prepareDependencies(project, gemWorkDir, jruby, gemConfiguration, overwrite)
    }

    static void prepareDependencies(
        Project project,
        File gemWorkDir,
        JRubyPluginExtension jruby,
        Configuration gemConfiguration,
        GemOverwriteAction overwrite
    ) {
        File gemDir = gemWorkDir.absoluteFile

        gemDir.mkdirs()

        GemUtils.extractGems(
            project,
            jruby.jrubyConfiguration,
            gemConfiguration,
            gemDir,
            overwrite
        )
        GemUtils.setupJars(
            gemConfiguration,
            gemDir,
            overwrite
        )
    }
//    /** Prepare dependencies with a custom overwrite behavior.
//     *
//     */
//    static void prepareDependencies(Project project, GemOverwriteAction overwrite) {
//
//        Configuration execConfiguration = project.configurations.findByName(configuration)
//
//        File gemDir = getGemWorkDir().absoluteFile
//
//        gemDir.mkdirs()
//
//        GemUtils.extractGems(
//            project,
//            execConfiguration,
//            execConfiguration,
//            gemDir,
//            overwrite
//        )
//        GemUtils.setupJars(
//            execConfiguration,
//            gemDir,
//            overwrite
//        )
//    }

    /** Prepare en environment which can be used to execute JRuby.
     *
     * @param presetEnvironment Preset envrionment to use.
     * @param inheritRubyEnv Whether to inherit the global Ruby environment.
     * @param gemWorkDir Working directory where GEMs are unpacked to.
     * @return Environment which can be used to execute JRuby within.
     *
     * @since 2.0
     */
    static Map<String, Object> prepareJRubyEnvironment(
        Map<String, Object> presetEnvironment,
        boolean inheritRubyEnv,
        File gemWorkDir
    ) {
        final Map<String, Object> preparedEnv = [:]

        preparedEnv.putAll(presetEnvironment.findAll { String key, Object value ->
            inheritRubyEnv || !(key in FILTERED_ENV_KEYS || key.matches(/rvm.*/))
        })

        preparedEnv.putAll([
            'JBUNDLE_SKIP': true,
            'JARS_SKIP'   : true,
            'PATH'        : prepareWorkingPath(gemWorkDir, System.getenv(pathVar())),
            'GEM_HOME'    : gemWorkDir.absolutePath,
            'GEM_PATH'    : gemWorkDir.absolutePath,
            'JARS_HOME'   : new File(gemWorkDir.absoluteFile, 'jars'),
            'JARS_LOCK'   : new File(gemWorkDir.absoluteFile, 'Jars.lock')
        ])

        return preparedEnv
    }

}
