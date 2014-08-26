package com.github.jrubygradle.internal

import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection

/**
 * @author Schalk W. Cronjé.
 */
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

    /** Extract the jruby-complete-XXX.jar as a FileCollection
     *
     * @param cfg FileCollection
     * @return Returns the classpath as a File or null if the jar was not found
     */
    static FileCollection jrubyJar(FileCollection fc) {
        fc.filter { File f -> it.name.startsWith('jruby-complete-') }
    }

}
