package com.lookout.jruby.internal

import org.gradle.api.artifacts.Configuration

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
        cfg.files.findAll { File f -> !f.name.endsWith('.gem') }
    }
}
