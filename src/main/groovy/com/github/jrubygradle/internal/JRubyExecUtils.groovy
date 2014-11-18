package com.github.jrubygradle.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection

import java.util.regex.Matcher

/**
 * @author Schalk W. Cronjé.
 */
@CompileStatic
class JRubyExecUtils {

    /** Extract a list of files from a configuration that is suitable for a jruby classpath
     *
     * @param cfg Configuration to use
     * @return
     */
    static def classpathFromConfiguration(Configuration cfg) {
        cfg.files.findAll { File f -> !f.name.toLowerCase().endsWith('.gem') }
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
    static String jrubyJarVersion(File jar) {
        Matcher matches = jar.name =~ /jruby-complete-(.+)\.jar/
        !matches ? null : matches[0][1]
    }

    /** Extract the jruby-complete-XXX.jar as a FileCollection
     *
     * @param cfg FileCollection
     * @return Returns the classpath as a File or null if the jar was not found
     */
    static FileCollection jrubyJar(FileCollection fc) {
        fc.filter { File f -> f.name.startsWith('jruby-complete-') }
    }

    static List<String> buildArgs( List<Object> jrubyArgs, File script, List<Object> scriptArgs ) {
        def cmdArgs = []

        boolean useBinPath = jrubyArgs.contains('-S')
        cmdArgs.addAll(jrubyArgs)

        if(script!=null && !useBinPath) {
            if(!script.exists()) {
                throw new InvalidUserDataException("${script} does not exist")
            }
            cmdArgs.add(script.absolutePath)
        } else if(script!=null && useBinPath ) {
            if(script.isAbsolute() && !script.exists()) {
                throw new InvalidUserDataException("${script} does not exist")
            }
            cmdArgs.add(script.toString())
        } else if(script==null && jrubyArgs.size() == 0 ) {
            throw new InvalidUserDataException('Cannot instantiate a JRubyExec instance without either `script` or `jrubyArgs` set')
        }
        cmdArgs.addAll(scriptArgs)
        cmdArgs as List<String>
    }
}
