package com.github.jrubygradle.internal

import com.github.jrubygradle.JRubyExec
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * @author Schalk W. Cronjé
 */
class JRubyExecDelegate implements JRubyExecTraits   {
    static final String JRUBYEXEC_CONFIG = JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG

    Project project

    Object methodMissing(String name, args) {
        if (name == 'args' || name == 'setArgs') {
            throw new UnsupportedOperationException('Use jrubyArgs/scriptArgs instead')
        }
        if (name == 'main') {
            throw new UnsupportedOperationException('Setting main class for jruby is not a valid operation')
        }

        if (args.size() == 1) {
            passthrough.add([(name.toString()) : args[0]])
        }
        else {
            passthrough.add([(name.toString()) : args])
        }
    }

    /** Gets the script to use.
     *
     * @return Get the script to use. Can be null.
     */
    File getScript() {
        return _convertScript()
    }

    /** buildArgs creates a list of arguments to pass to the JVM
     */
    List<String> buildArgs() {
        return JRubyExecUtils.buildArgs(_convertJrubyArgs(), script, _convertScriptArgs())
    }

    @PackageScope
    Object keyAt(Integer index) {
        return passthrough[index].keySet()[0]
    }

    @PackageScope
    Object valuesAt(Integer index) {
        return passthrough[index].values()[0]
    }

    private final List passthrough = []

    static Object jrubyexecDelegatingClosure = { Project project, Closure cl ->
        JRubyExecDelegate proxy =  new JRubyExecDelegate()
        proxy.project = project
        Closure cl2 = cl.clone()
        cl2.delegate = proxy
        cl2.call()

        Configuration config = project.configurations.getByName(JRUBYEXEC_CONFIG)
        proxy.prepareDependencies(project)

        project.javaexec {
            classpath JRubyExecUtils.classpathFromConfiguration(config)
            proxy.passthrough.each { item ->
                Object k = item.keySet()[0]
                Object v = item.values()[0]
                invokeMethod("${k}", v)
            }
            main 'org.jruby.Main'
            // just keep this even if it does not exists
            args '-I' + JRubyExec.jarDependenciesGemLibPath(proxy.gemWorkDir)
            // load Jars.lock on startup
            args '-rjars/setup'
            proxy.buildArgs().each { item ->
                args item.toString()
            }

            // Start with System.env then add from environment,
            // which will add the user settings and
            // overwrite any overlapping entries
            final env = [:]
            env << System.env
            env << environment

            setEnvironment proxy.getPreparedEnvironment(env)
        }
    }

    static void addToProject(Project project) {
        project.ext {
            jrubyexec = JRubyExecDelegate.jrubyexecDelegatingClosure.curry(project)
        }
    }
}
