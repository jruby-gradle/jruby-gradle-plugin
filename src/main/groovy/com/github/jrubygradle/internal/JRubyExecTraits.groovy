package com.github.jrubygradle.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.util.CollectionUtils

/** Provides common traits for JRuby script execution across the {@code JRubyExec}
 * task and {@project.jrubyexec} extension.
 *
 * @author Schalk W. Cronjé
 * @since 0.1.18
 */
@CompileStatic
trait JRubyExecTraits {

    /** Allow JRubyExec to inherit a Ruby env from the shell (e.g. RVM)
     *
     * @since 0.1.10 (Moved from {@code JRubyExec})
     */
    boolean inheritRubyEnv = false

    /** Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    void script(def scr) {
        setScript(scr)
    }

    /** Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    void setScript(def scr) {
        this.script = scr
    }

    /** Set alternative GEM unpack directory to use
     *
     * @param wd New working directory (convertible to file)
     */
    void setGemWorkDir( Object wd ) {
        this.gemWorkDir = wd
    }

    /** Set alternative GEM unpack directory to use
     *
     * @param wd New working directory (convertible to file)
     */
    void gemWorkDir( Object wd ) {
        this.gemWorkDir = wd
    }

    /** Add arguments for script
     *
     * @param args Arguments to be aqdded to script arguments
     */
    void scriptArgs(Object... args) {
        this.scriptArgs.addAll(args as List)
    }

    /** Add arguments for script in a closure style
     *
     * @param args Arguments to be aqdded to script arguments
     */
    void scriptArgs(Closure c) {
        this.scriptArgs << c
    }

    /** Add arguments for jruby
     *
     * @param args Additional arguments to be passed to JRuby itself.
     */
    void jrubyArgs(Object... args) {
        this.jrubyArgs.addAll(args as List)
    }


    File _convertScript() {
        switch (this.script) {
            case null:
                return null
            case File:
                return this.script as File
            case String:
                return new File(this.script as String)
            default:
                return new File(this.script.toString())
        }
    }


    // Internal functions intended to be used by plugin itself.

    File _convertGemWorkDir(Project project) {
        this.gemWorkDir ? project.file(this.gemWorkDir) : null
    }

    @CompileDynamic
    List<String> _convertScriptArgs() {
        CollectionUtils.stringize(
            this.scriptArgs.collect { arg ->
                /* In order to support closures in scriptArgs for lazy
                 * evaluation, we need to evaluate the closure if it is present
                 */
                if (arg instanceof Closure) {
                    (arg as Closure).call()
                }
                else {
                    arg
                }
            } .flatten()
        )
    }

    @CompileDynamic
    List<String> _convertJrubyArgs() {
        CollectionUtils.stringize(this.jrubyArgs)
    }

    private Object script
    private Object gemWorkDir
    private List<Object> scriptArgs = []
    private List<Object> jrubyArgs = []
}

