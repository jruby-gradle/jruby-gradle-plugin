/*
 * Copyright (c) 2014-2019, R. Tyler Croy <rtyler@brokenco.de>,
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

import com.github.jrubygradle.api.core.JRubyAwareTask
import com.github.jrubygradle.api.core.JRubyExecSpec
import com.github.jrubygradle.internal.JRubyExecUtils
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.Optional
import org.gradle.process.JavaExecSpec
import org.gradle.util.GradleVersion

import java.util.concurrent.Callable

import static com.github.jrubygradle.internal.JRubyExecUtils.prepareJRubyEnvironment
import static com.github.jrubygradle.internal.JRubyExecUtils.resolveScript
import static org.ysb33r.grolifant.api.StringUtils.stringize

/** Runs a ruby script using JRuby
 *
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 *
 */
class JRubyExec extends JavaExec implements JRubyAwareTask, JRubyExecSpec {

    public static final String MAIN_CLASS = 'org.jruby.Main'
    private static final String USE_JVM_ARGS = 'Use jvmArgs / scriptArgs instead'

    /**
     * Allows for use script author to control the effect of the
     * system Ruby environment.
     */
    @Input
    boolean inheritRubyEnv = false

    JRubyExec() {
        super()
        super.setMain MAIN_CLASS
        this.jruby = extensions.create(JRubyPluginExtension.NAME, JRubyPluginExtension, this)

        inputs.property 'jrubyver', {
            jruby.jrubyVersion
        }

        inputs.property 'gemConfiguration', {
            jruby.gemConfiguration
        }

        if (GradleVersion.current() >= GradleVersion.version('4.10')) {
            dependsOn(project.provider({ JRubyPluginExtension jpe ->
                project.tasks.getByName(jpe.gemPrepareTaskName)
            }.curry(this.jruby)))
        } else {
            project.afterEvaluate({ Task t, JRubyPluginExtension jpe ->
                t.dependsOn(jpe.gemPrepareTaskName)
            }.curry(this, this.jruby))
        }
    }

    /** Script to execute.
     * @return The path to the script (or nul if not set)
     */
    @Optional
    @Input
    File getScript() {
        resolveScript(project, this.script)
    }

    /** Returns a list of script arguments
     */
    @Input
    List<String> getScriptArgs() {
        stringize(this.scriptArgs)
    }

    /** Returns a list of jruby arguments
     */
    @Input
    List<String> getJrubyArgs() {
        stringize(this.jrubyArgs)
    }

    /**
     * Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    @Override
    void script(Object scr) {
        this.script = scr
    }

    /**
     * Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    @Override
    void setScript(Object scr) {
        this.script = scr
    }

    /**
     * Clear existing arguments and assign a new set.
     *
     * @param args New set of script arguments.
     */
    @Override
    void setScriptArgs(Iterable<Object> args) {
        this.scriptArgs.clear()
        this.scriptArgs.addAll(args)
    }

    /**
     * Add arguments for script
     *
     * @param args Arguments to be aqdded to script arguments
     */
    @Override
    void scriptArgs(Object... args) {
        this.scriptArgs.addAll(args.toList())
    }

    /**
     * Clear existing JRuby-specific arguments and assign a new set.
     *
     * @param args New collection of JRUby-sepcific arguments.
     */
    @Override
    void setJrubyArgs(Iterable<Object> args) {
        this.jrubyArgs.clear()
        this.jrubyArgs.addAll(args)
    }

    /**
     * Add JRuby-specific arguments.
     *
     * @param args
     */
    @Override
    void jrubyArgs(Object... args) {
        this.jrubyArgs.addAll(args.toList())
    }

    /** Location of GEM working directory.
     *
     * @return Provider of GEM working directory.
     */
    Provider<File> getGemWorkDir() {
        Callable<File> resolveGemWorkDir = { JRubyPluginExtension jpe ->
            ((JRubyPrepare) project.tasks.getByName(jpe.gemPrepareTaskName)).outputDir
        }.curry(jruby) as Callable<File>

        project.provider(resolveGemWorkDir)
    }

    /** If it is required that a JRubyExec task needs to be executed with a different version of JRuby that the
     * globally configured one, it can be done by setting it here.
     *
     * @deprecated Use{@code jruby.getJrubyVersion( )} instead.
     *
     */
    @Deprecated
    String getJrubyVersion() {
        deprecated('Use jruby.getJrubyVersion() rather getJrubyVersion()')
        jruby.jrubyVersion
    }

    /** Setting the {@code jruby-complete} version allows for tasks to be run using different versions of JRuby.
     * This is useful for comparing the results of different version or running with a gem that is only
     * compatible with a specific version or when running a script with a different version that what will
     * be packaged.
     *
     * @param version String in the form '9.0.1.0'
     *
     * @deprecated Use{@code jruby.jrubyVersion} instead.
     *
     * @since 0.1.18
     */
    @Deprecated
    void jrubyVersion(final String ver) {
        deprecated('Use jruby.jrubyVersion rather jrubyVersion')
        jruby.jrubyVersion(ver)
    }

    /**
     *
     * @param newConfiguration
     *
     * @deprecated Use{@code jruby.setGemConfiguration( )} instead.
     */
    @Deprecated
    void configuration(Configuration newConfiguration) {
        deprecated('Use jruby.setGemConfiguration() rather than configuration()')
        jruby.gemConfiguration(newConfiguration)
    }

    /**
     *
     * @param newConfiguration
     *
     * @deprecated Use{@code jruby.setGemConfiguration( )} instead.
     */
    @Deprecated
    void configuration(String newConfiguration) {
        deprecated('Use jruby.setGemConfiguration(newConfiguration) rather than configuration(newConfiguration)')
        jruby.gemConfiguration(newConfiguration)
    }

    /** Setting the {@code jruby-complete} version allows for tasks to be run using different versions of JRuby.
     * This is useful for comparing the results of different version or running with a gem that is only
     * compatible with a specific version or when running a script with a different version that what will
     * be packaged.
     *
     * @param version String in the form '9.0.1.0'
     *
     * @deprecated Use{@code jruby.setJrubyVersion} rather {@code setJrubyVersion}.
     */
    @Deprecated
    void setJrubyVersion(final String version) {
        deprecated('Use jruby.setJrubyVersion rather setJrubyVersion')
        jruby.jrubyVersion(version)
    }

    @Override
    @SuppressWarnings('UnnecessaryGetter')
    void exec() {
        File gemDir = getGemWorkDir().get()
        setEnvironment prepareJRubyEnvironment(this.environment, this.inheritRubyEnv, gemDir)
        super.classpath jruby.jrubyConfiguration
        super.setArgs(getArgs())
        super.exec()
    }

    /** getArgs gets overridden in order to add JRuby options, script name and script arguments in the correct order.
     *
     * There are three modes of behaviour
     * <ul>
     *   <li> script set. no jrubyArgs, or jrubyArgs does not contain {@code -S}: normal way to execute script. A check
     *   whether the script exists will be performed.
     *   <li> script set. jrubyArgs contains {@code -S}: if script is not absolute, no check will be performed to see
     *   if the script exists and will be assumed that the script can be found using the default ruby path mechanism.
     *   <li> script not set, but jrubyArgs set: set up to execute jruby with no script. This should be a rarely used
     *   option.
     * </ul>
     *
     * @throw {@code org.gradle.api.InvalidUserDataException} if mode of behaviour cannot be determined.
     */
    @Override
    @Input
    @SuppressWarnings('UnnecessaryGetter')
    List<String> getArgs() {
        JRubyExecUtils.buildArgs([], jrubyArgs, getScript(), scriptArgs)
    }

    @Override
    JavaExec setMain(final String mainClassName) {
        if (mainClassName == MAIN_CLASS) {
            super.setMain(mainClassName)
        } else {
            throw notAllowed("Setting main class for JRuby to ${mainClassName} is not a valid operation")
        }
    }

    @Override
    JavaExec setArgs(Iterable<?> applicationArgs) {
        throw notAllowed(USE_JVM_ARGS)
    }

    @Override
    JavaExec args(Object... args) {
        throw notAllowed(USE_JVM_ARGS)
    }

    @Override
    JavaExecSpec args(Iterable<?> args) {
        throw notAllowed(USE_JVM_ARGS)
    }

    private static UnsupportedOperationException notAllowed(final String msg) {
        return new UnsupportedOperationException(msg)
    }

    private void deprecated(String message) {
        logger.warn "Deprecated method in task ${name}: ${message}"
    }

    private final JRubyPluginExtension jruby
    private Object script
    private final List<Object> scriptArgs = []
    private final List<Object> jrubyArgs = []

}
