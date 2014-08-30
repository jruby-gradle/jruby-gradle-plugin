package com.github.jrubygradle.internal

import com.github.jrubygradle.GemUtils
import com.github.jrubygradle.JRubyExec
import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.internal.FileUtils
import org.gradle.util.CollectionUtils

/**
 * @author Schalk W. Cronjé
 */
class JRubyExecDelegate {

    static final String JRUBYEXEC_CONFIG = JRubyExec.JRUBYEXEC_CONFIG

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

    /** Sets the scriptname
     *
     * @param fName Path to script
     */
    void script(Object fName) {
        switch (fName) {
            case File:
                this.script=fName
                break
            case String:
                this.script=new File(fName)
                break
            default:
                this.script=new File(fName.toString())
        }
    }


    String getScript() { this.script }

    /** Returns a list of script arguments
     */
    List<String> scriptArgs() {CollectionUtils.stringize(this.scriptArgs)}

    /** Set arguments for script
     *
     * @param args
     */
    void scriptArgs(Object... args) {
        this.scriptArgs.addAll(args as List)
    }

    /** Returns a list of jruby arguments
     */
    List<String> jrubyArgs()  {CollectionUtils.stringize(this.jrubyArgs)}

    /** Set arguments for jruby
     *
     * @param args
     */
    void jrubyArgs(Object... args) {
        this.jrubyArgs.addAll(args as List)
    }

    /** buildArgs creates a list of arguments to pass to the JVM
     */
    List<String> buildArgs() {
        def cmdArgs = []
        cmdArgs.addAll(jrubyArgs)
        cmdArgs.add(script.absolutePath)
        cmdArgs.addAll(scriptArgs)
        cmdArgs as List<String>
    }

    @PackageScope
    def keyAt(Integer index) {
        passthrough[index].keySet()[0]
    }

    @PackageScope
    def valuesAt(Integer index) {
        passthrough[index].values()[0]
    }

    @PackageScope
    void validate() {
        if( this.script == null ) {
            throw new NullPointerException("'script' is not set")
        }
    }

    private def passthrough = []
    private File script
    private List<Object>  scriptArgs = []
    private List<Object>  jrubyArgs = []

    static File tmpGemDir(Project project) {
        String ext = FileUtils.toSafeFileName('project.'+JRUBYEXEC_CONFIG)
        new File( project.buildDir, "tmp/${ext}").absoluteFile
    }

    static def jrubyexecDelegatingClosure = { Project project, Closure cl ->
        def proxy =  new JRubyExecDelegate()
        Closure cl2 = cl.clone()
        cl2.delegate = proxy
        cl2.call()

        proxy.validate()
        File gemDir=new File(project.jruby.gemInstallDir)
        Configuration config = project.configurations.getByName(JRUBYEXEC_CONFIG)
        GemUtils.OverwriteAction overwrite = project.gradle.startParameter.refreshDependencies ?  GemUtils.OverwriteAction.OVERWRITE : GemUtils.OverwriteAction.SKIP
        project.mkdir gemDir
        GemUtils.extractGems(project,config,config,gemDir,overwrite)

        project.javaexec {
            classpath JRubyExecUtils.classpathFromConfiguration(config)
            proxy.passthrough.each { item ->
                def k = item.keySet()[0]
                def v = item.values()[0]
                "${k}" v
            }
            main 'org.jruby.Main'
            proxy.buildArgs().each { item ->
               args item.toString()
            }
            environment 'GEM_HOME' : gemDir.absolutePath
        }
    }

    static void addToProject(Project project) {
        project.ext {
            jrubyexec = JRubyExecDelegate.jrubyexecDelegatingClosure.curry(project)
        }
    }
}
