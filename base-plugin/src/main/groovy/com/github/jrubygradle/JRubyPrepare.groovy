/*
 * Copyright (c) 2014-2023, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle

import com.github.jrubygradle.api.core.AbstractJRubyPrepare
import com.github.jrubygradle.internal.JRubyExecUtils
import groovy.transform.CompileStatic
import org.gradle.api.provider.Provider

import java.util.concurrent.Callable

/** Task for preparing a project-local installation of GEMs & JARs.
 *
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 */
@CompileStatic
class JRubyPrepare extends AbstractJRubyPrepare {

    JRubyPrepare() {
        super()
        this.jruby = extensions.create(JRubyPluginExtension.NAME, JRubyPluginExtension, this)
        this.jrubyJarLocation = project.provider({ JRubyPluginExtension jrubyExt ->
            JRubyExecUtils.jrubyJar(jrubyExt.jrubyConfiguration)
        }.curry(this.jruby) as Callable<File>)
    }

    /** Location of {@code jruby-complete} JAR.
     *
     * @return Path on local filesystem
     */
    @Override
    protected Provider<File> getJrubyJarLocation() {
        this.jrubyJarLocation
    }

    /** Version of JRuby to be used.
     *
     * This method should not resolve any files to obtain the version.
     *
     * @return Intended version of JRuby.
     */
    @Override
    protected String getProposedJRubyVersion() {
        jruby.jrubyVersion
    }

    private final JRubyPluginExtension jruby
    private final Provider<File> jrubyJarLocation
}

