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
package com.github.jrubygradle.internal

import com.github.jrubygradle.JRubyPluginExtension
import com.github.jrubygradle.api.core.JRubyExecSpec
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.process.ExecResult
import org.gradle.process.JavaExecSpec
import org.ysb33r.grolifant.api.ClosureUtils

import static com.github.jrubygradle.JRubyExec.MAIN_CLASS
import static com.github.jrubygradle.internal.JRubyExecUtils.buildArgs
import static com.github.jrubygradle.internal.JRubyExecUtils.prepareJRubyEnvironment
import static com.github.jrubygradle.internal.JRubyExecUtils.resolveScript
import static org.ysb33r.grolifant.api.StringUtils.stringize

/** Delegate for running JRuby using {@code project.jrubyexec}.
 *
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 *
 */
class JRubyExecDelegate {

    static void addToProject(final Project project, final String name) {
        project.extensions.extraProperties.set(name, { JRubyExecDelegate delegator, def cfg ->
            switch (cfg) {
                case Closure:
                    delegator.call((Closure) cfg)
                    break
                case Action:
                    delegator.call((Action) cfg)
                    break
                default:
                    throw new UnsupportedOperationException(
                        "Invalid type passed to ${name}. Use closure or Action<JRubyExecSpec>."
                    )
            }
        }.curry(new JRubyExecDelegate(project)))
    }

    ExecResult call(@DelegatesTo(JRubyExecSpec) Closure cfg) {
        project.javaexec { JavaExecSpec javaExecSpec ->
            ExecSpec execSpec = new ExecSpec(project, javaExecSpec)
            ClosureUtils.configureItem(execSpec, cfg)
            finaliseJavaExecConfiguration(execSpec, javaExecSpec)
        }
    }

    ExecResult call(Action<JRubyExecSpec> cfg) {
        project.javaexec { JavaExecSpec javaExecSpec ->
            ExecSpec execSpec = new ExecSpec(project, javaExecSpec)
            cfg.execute(spec)
            finaliseJavaExecConfiguration(execSpec, javaExecSpec)
        }
    }

    void finaliseJavaExecConfiguration(ExecSpec execSpec, JavaExecSpec javaExecSpec) {
        JRubyPluginExtension jruby = project.extensions.getByType(JRubyPluginExtension)
        javaExecSpec.with {
            main = MAIN_CLASS
            classpath = jruby.jrubyConfiguration
            args = buildArgs([], execSpec.jrubyArgs, execSpec.script, execSpec.scriptArgs)
            environment = prepareJRubyEnvironment(
                environment,
                execSpec.inheritRubyEnv,
                project.tasks.getByName(jruby.gemPrepareTaskName).outputDir
            )
        }
    }

    private JRubyExecDelegate(Project project) {
        this.project = project
    }

    private final Project project

    private static class ExecSpec implements JRubyExecSpec {
        ExecSpec(Project project, JavaExecSpec spec) {
            this.project = project
            this.javaExecSpec = spec
        }

        boolean inheritRubyEnv

        @Override
        JavaExecSpec args(Iterable<?> a) {
            notSupported(USE_ARGS_ALTERNATIVES)
        }

        @Override
        JavaExecSpec args(Object... a) {
            notSupported(USE_ARGS_ALTERNATIVES)
        }

        @Override
        JavaExecSpec setArgs(Iterable<?> a) {
            notSupported(USE_ARGS_ALTERNATIVES)
        }

        @SuppressWarnings('UnusedMethodParameter')
        JavaExecSpec setArgs(Object... a) {
            notSupported(USE_ARGS_ALTERNATIVES)
        }

        @Override
        JavaExecSpec setMain(String m) {
            notSupported('Main class name cannot be changed')
        }

        @Override
        JavaExecSpec setClasspath(FileCollection cp) {
            notSupported('Classpath cannot be changed. Use jruby.gemConfiguration/jruby.jrubyVersion instead.')
        }

        @Override
        void script(Object scr) {
            this.script = resolveScript(project, scr)
        }

        @Override
        void setScript(Object scr) {
            this.script = resolveScript(project, scr)
        }

        @Override
        File getScript() {
            this.script
        }

        @Override
        void setScriptArgs(Iterable<Object> args) {
            this.scriptArgs.clear()
            this.scriptArgs.addAll(stringize(args))
        }

        @Override
        void scriptArgs(Object... args) {
            this.scriptArgs.addAll(stringize(args.toList()))
        }

        @Override
        List<String> getScriptArgs() {
            this.scriptArgs
        }

        @Override
        void setJrubyArgs(Iterable<Object> args) {
            this.jrubyArgs.clear()
            this.jrubyArgs.addAll(stringize(args))
        }

        @Override
        void jrubyArgs(Object... args) {
            this.jrubyArgs.addAll(stringize(args.toList()))
        }

        @Override
        List<String> getJrubyArgs() {
            this.jrubyArgs
        }

        private void notSupported(final String msg) {
            throw new UnsupportedOperationException(msg)
        }

        private static final String USE_ARGS_ALTERNATIVES = 'Use jrubyArgs/scriptArgs instead of `args`'

        private File script
        private final List<String> scriptArgs = []
        private final List<String> jrubyArgs = []
        private final @Delegate
        JavaExecSpec javaExecSpec
        private final Project project
    }
}
