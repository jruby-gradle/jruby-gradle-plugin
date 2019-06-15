package com.github.jrubygradle.api.core;

import org.gradle.process.JavaExecSpec;

import java.io.File;
import java.util.List;

/**
 * Formalises a way of executing JRuby from Gradle via an execution specification
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
public interface JRubyExecSpec extends JavaExecSpec {
    /**
     * Allows for use script author to control the effect of the
     * system Ruby environment.
     *
     * @param inherit {@code true} if Ruby environment should be inherited.
     */
    void setInheritRubyEnv(boolean inherit);

    /**
     * Check is Ruby environment should be inherited.
     *
     * @return {@code true} ir system Ruby environment should be inherited.
     */
    boolean getInheritRubyEnv();

    /**
     * Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    void script(Object scr);

    /**
     * Set script to execute.
     *
     * @param scr Path to script. Can be any object that is convertible to File.
     */
    void setScript(Object scr);

    /**
     * Ruby script to execute.
     *
     * @return Script to execute. Can be {@code null}.
     */
    File getScript();

    /**
     * Clear existing arguments and assign a new set.
     *
     * @param args New set of script arguments.
     */
    void setScriptArgs(Iterable<Object> args);

    /**
     * Add arguments for script
     *
     * @param args Arguments to be aqdded to script arguments
     */
    void scriptArgs(Object... args);

    /**
     * Script-specific arguments.
     *
     * @return List of arguments. Can be empty, but never {@code null}.
     */
    List<String> getScriptArgs();

    /**
     * Clear existing JRuby-specific arguments and assign a new set.
     *
     * @param args New collection of JRUby-sepcific arguments.
     */
    void setJrubyArgs(Iterable<Object> args);

    /**
     * Add JRuby-specific arguments.
     *
     * @param args
     */
    void jrubyArgs(Object... args);

    /**
     * JRuby-specific arguments.
     *
     * @return List of arguments. Can be empty, but never {@code null}.
     */
    List<String> getJrubyArgs();
}
