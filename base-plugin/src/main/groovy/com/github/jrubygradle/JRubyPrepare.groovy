package com.github.jrubygradle

import com.github.jrubygradle.api.core.AbstractJRubyPrepare
import com.github.jrubygradle.internal.JRubyExecUtils
import groovy.transform.CompileStatic

/**
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 */
@CompileStatic
class JRubyPrepare extends AbstractJRubyPrepare {

    JRubyPrepare() {
        super()
        this.jruby = extensions.create(JRubyPluginExtension.NAME, JRubyPluginExtension, this)
    }

    /** Location of {@code jruby-complete} JAR.
     *
     * @return Path on local filesystem
     */
    @Override
    protected File getJrubyJarLocation() {
        JRubyExecUtils.jrubyJar(this.jruby.jrubyConfiguration)
    }

    private final JRubyPluginExtension jruby
}

