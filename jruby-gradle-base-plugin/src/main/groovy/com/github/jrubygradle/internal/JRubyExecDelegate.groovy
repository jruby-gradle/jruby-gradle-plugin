package com.github.jrubygradle.internal

import com.github.jrubygradle.GemUtils
import com.github.jrubygradle.JRubyExec
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * @author Schalk W. Cronjé
 */
class JRubyExecDelegate implements JRubyExecTraits   {

    static final String JRUBYEXEC_CONFIG = JRubyExecUtils.DEFAULT_JRUBYEXEC_CONFIG

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

    /** Directory to use for unpacking GEMs.
     * This is optional. If not set, then an internal generated folder will be used. In general the latter behaviour
     * is preferred as it allows for isolating different {@code JRubyExec} tasks. However, this functionality is made
     * available for script authors for would like to control this behaviour and potentially share GEMs between
     * various {@code JRubyExec} tasks.
     *
     * @since 0.1.9
     */
    File getGemWorkDir() {
        _convertGemWorkDir(project)
    }

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
        Closure cl2 = cl.clone()
        cl2.delegate = proxy
        cl2.call()

        File gemDir= proxy._convertGemWorkDir(project) ?: project.file(project.jruby.gemInstallDir)

        Configuration config = project.configurations.getByName(JRUBYEXEC_CONFIG)
        GemUtils.OverwriteAction overwrite = project.gradle.startParameter.refreshDependencies ?  GemUtils.OverwriteAction.OVERWRITE : GemUtils.OverwriteAction.SKIP
        project.mkdir gemDir
        GemUtils.extractGems(project,config,config,gemDir,overwrite)
        GemUtils.setupJars(config,gemDir,overwrite)
        String pathVar = JRubyExecUtils.pathVar()

        project.javaexec {
            classpath JRubyExecUtils.classpathFromConfiguration(config)
            proxy.passthrough.each { item ->
                def k = item.keySet()[0]
                def v = item.values()[0]
                "${k}" v
            }
            main 'org.jruby.Main'
            // just keep this even if it does not exists
            args '-I' + JRubyExec.jarDependenciesGemLibPath(gemDir)
            // load Jars.lock on startup
            args '-rjars/setup'
            proxy.buildArgs().each { item ->
                args item.toString()
            }

            setEnvironment JRubyExecUtils.preparedEnvironment(getEnvironment(),proxy.inheritRubyEnv)
            environment 'PATH' : JRubyExecUtils.prepareWorkingPath(gemDir,System.env."${pathVar}")
            environment 'GEM_HOME' : gemDir.absolutePath
            environment 'GEM_PATH' : gemDir.absolutePath
            environment 'JARS_HOME' : new File(gemDir.absolutePath, 'jars')
            environment 'JARS_LOCK' : new File(gemDir.absolutePath, 'Jars.lock')
        }
    }

    static void addToProject(Project project) {
        project.ext {
            jrubyexec = JRubyExecDelegate.jrubyexecDelegatingClosure.curry(project)
        }
    }
}
