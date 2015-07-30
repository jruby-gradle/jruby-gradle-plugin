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

    /** Extract a list of files from a configuration that is suitable for a jruby classpath
     *
     * @param cfg Configuration to use
     * @return
     */
    static def classpathFromConfiguration(Configuration cfg) {
        cfg.files.findAll { File f -> f.name.startsWith('jruby-complete-') }
    }

    /** Extract the jruby-complete-XXX.jar classpath
     *
     * @param cfg Configuration
     * @return Returns the classpath as a File or null if the jar was not found
     */
    static File jrubyJar(Configuration cfg) {
        cfg.files.find { it.name.startsWith('jruby-complete-') }
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
        Matcher matches = jar.name =~ /jruby-complete-(.+)\.jar/
        !matches ? null : matches[0][1]
    }

    /** Extracts the JRuby version number as a triplet from a jruby-complete-XXX.jar filename
     *
     * @param jar JRuby Jar
     * @return Version string map [major,minor,patchlevel] or null
     *
     * @since 0.1.16
     */
    @CompileDynamic
    static Map jrubyJarVersionTriple(final File jar) {
        String version = jrubyJarVersion(jar)
        if(!version) {return null}

        Matcher matches = version =~ /(\d{1,2})\.(\d{1,3})\.(\d{1,3}).*/

        (!matches.matches() || matches[0].size() != 4) ? null : [
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
        fc.filter { File f -> f.name.startsWith('jruby-complete-') }
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
    static List<String> buildArgs(List<Object> extra, List<Object> jrubyArgs, File script, List<Object> scriptArgs) {
        def cmdArgs = extra
        // load Jars.lock on startup
        cmdArgs.add('-rjars/setup')
        boolean hasInlineScript = jrubyArgs.contains('-e')
        boolean useBinPath = jrubyArgs.contains('-S')

        /* Fefault to adding the -S option if we don't have an expression to evaluate
         * <https://github.com/jruby-gradle/jruby-gradle-plugin/issues/152>
         */
        if (!hasInlineScript && script && !jrubyArgs.contains('-S')) {
            jrubyArgs.add('-S')
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
                throw new InvalidUserDataException("No `script` property defined and no inline script provided")
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
        if (!(c.dependencies.find { it.name == 'jruby-complete'})) {
            project.dependencies.add(configuration, "org.jruby:jruby-complete:${version}")
        }

        if (version.startsWith("1.7.1")) {
            project.dependencies.add(configuration,
                    "rubygems:jar-dependencies:${JAR_DEPENDENCIES_VERSION}")
        }
    }

}
