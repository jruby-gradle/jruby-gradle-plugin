package com.github.jrubygradle.internal

import groovy.transform.CompileDynamic
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection

import java.util.regex.Matcher

/**
 * @author Schalk W. Cronjé.
 */
class JRubyExecUtils {
    static final String JAR_DEPENDENCIES_VERSION = '0.1.15'
    static final String DEFAULT_JRUBYEXEC_CONFIG = 'jrubyExec'

    private static final String JRUBY_COMPLETE = 'jruby-complete'
    private static final String BINPATH_FLAG = '-S'

    /** Extract a list of files from a configuration that is suitable for a jruby classpath
     *
     * @param cfg Configuration to use
     * @return
     */
    static Set classpathFromConfiguration(Configuration cfg) {
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
     * @since 0.1.16
     * @deprecated This method is no longer used and will be removed in a later
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
            major : matches[0][1].toInteger(),
            minor : matches[0][2].toInteger(),
            patchlevel : matches[0][3].toInteger()
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

        /* Fefault to adding the -S option if we don't have an expression to evaluate
         * <https://github.com/jruby-gradle/jruby-gradle-plugin/issues/152>
         */
        if (!hasInlineScript && script && !jrubyArgs.contains(BINPATH_FLAG)) {
            jrubyArgs.add(BINPATH_FLAG)
            useBinPath = true
        }

        cmdArgs.addAll(jrubyArgs)

        if (useBinPath && (script instanceof File)) {
            if (script.isAbsolute() && (!script.exists())) {
                throw new InvalidUserDataException("${script} does not exist")
            }
            cmdArgs.add(script.toString())
        }
        else if (script == null) {
            if (useBinPath && (jrubyArgs.size() <= 1)) {
                throw new InvalidUserDataException('No `script` property defined and no inline script provided')
            }

            if (jrubyArgs.isEmpty()) {
                throw new InvalidUserDataException('Cannot build JRuby execution arguments with either `script` or `jrubyArgs` set')
            }
        }

        cmdArgs.addAll(scriptArgs as List<String>)
        return cmdArgs
    }

    /** Get the name of the system search path environmental variable
     *
     * @return Name of variable
     * @since 0.1.11
     */
    static String pathVar() {
        org.gradle.internal.os.OperatingSystem.current().pathVar
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

        if (version.startsWith('1.7.1')) {
            project.dependencies.add(configuration,
                    "rubygems:jar-dependencies:${JAR_DEPENDENCIES_VERSION}")
        }
    }

}
