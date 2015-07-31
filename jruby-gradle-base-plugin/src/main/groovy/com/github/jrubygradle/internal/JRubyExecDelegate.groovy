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

    def methodMissing(String name, args) {
        if( name == 'args' || name == 'setArgs' ) {
            throw new UnsupportedOperationException("Use jrubyArgs/scriptArgs instead")
        }
        if( name == 'main' ) {
            throw new UnsupportedOperationException("Setting main class for jruby is not a valid operation")
        }

        if(args.size() == 1) {
            passthrough.add( [ "${name}" : args[0] ] )
        } else {
            passthrough.add( [ "${name}" : args ] )
        }
    }

    /** Gets the script to use.
     *
     * @return Get the script to use. Can be null.
     */
    File getScript() { _convertScript() }

    /** buildArgs creates a list of arguments to pass to the JVM
     */
    List<String> buildArgs() {
        JRubyExecUtils.buildArgs(_convertJrubyArgs(),script,_convertScriptArgs())
    }

    @PackageScope
    def keyAt(Integer index) {
        passthrough[index].keySet()[0]
    }

    @PackageScope
    def valuesAt(Integer index) {
        passthrough[index].values()[0]
    }

    private def passthrough = []

    static def jrubyexecDelegatingClosure = { Project project, Closure cl ->
        def proxy =  new JRubyExecDelegate()
        proxy.project = project
        Closure cl2 = cl.clone()
        cl2.delegate = proxy
        cl2.call()

        Configuration config = project.configurations.getByName(JRUBYEXEC_CONFIG)
        proxy.prepareDependencies(project)

        project.javaexec {
            classpath JRubyExecUtils.classpathFromConfiguration(config)
            proxy.passthrough.each { item ->
                def k = item.keySet()[0]
                def v = item.values()[0]
                "${k}" v
            }
            main 'org.jruby.Main'
            // just keep this even if it does not exists
            args '-I' + JRubyExec.jarDependenciesGemLibPath(proxy.getGemWorkDir())
            // load Jars.lock on startup
            args '-rjars/setup'
            proxy.buildArgs().each { item ->
                args item.toString()
            }

            setEnvironment proxy.getPreparedEnvironment(System.env)
        }
    }

    static void addToProject(Project project) {
        project.ext {
            jrubyexec = JRubyExecDelegate.jrubyexecDelegatingClosure.curry(project)
        }
    }
}
