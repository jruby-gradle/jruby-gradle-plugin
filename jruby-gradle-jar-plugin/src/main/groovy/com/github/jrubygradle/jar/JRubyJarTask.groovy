package com.github.jrubygradle.jar

import com.github.jrubygradle.GemUtils
import com.github.jrubygradle.jar.internal.JRubyDirInfo

import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.file.CopySpec
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.StopExecutionException

/**
 * @author Christian Meier
 */
class JRubyJar extends Jar {

    enum Type { RUNNABLE, LIBRARY }

    static final String DEFAULT_MAIN_CLASS = 'de.saumya.mojo.mains.JarMain'
    static final String EXTRACTING_MAIN_CLASS = 'de.saumya.mojo.mains.ExtractingMain'

    @Input
    String jrubyVersion = project.jruby.defaultVersion
    @Input
    String jrubyMainsVersion = '0.2.0'
    @Input
    String mainClass
    
    /** Adds a GEM installation directory
     */
    @InputDirectory
    void gemDir(def properties=[:],File f) {
        gemDir(properties,f.absolutePath)
    }

    /** Adds a GEM installation directory
     * @param Properties that affect how the GEM is packaged in the JAR. Currently only {@code fullGem} is
     * supported. If set the full GEM content will be packed, otherwise only a subset will be packed.
     * @param dir Source folder. Will be handled by {@code project.files(dir)}
    */
    void gemDir(def properties=[:],Object dir) {
        CopySpec spec = GemUtils.gemCopySpec(properties,project,dir)
        spec.exclude '.jrubydir'
        with spec
        // TODO change base plugin to store jars in
        // project.jruby.gemInstallDir + '/jars' since they belong together
        with GemUtils.jarCopySpec(project, {project.jruby.jarInstallDir})
    }

    /** Makes the JAR executable by setting a custom main class
     *
     * @param className Name of main class
     */
    @Incubating
    void mainClass(final String className) {
        this.mainClass = className
        if (this.scriptName == null) {
            this.scriptName = runnable()
        }
    }

    @Incubating
    void initScript(final Object scriptName) {
        this.scriptName = scriptName
    }

    /** Sets the defaults
     *
     * @param defs A list of defaults. Currently {@code gems} and {@code mainClass} are the only recognised values.
     * Unrecognised values are silently discarded
     */
    void defaults(final String... defs ) {
        defs.each { String it ->
            switch(it) {
                case 'gems':
                case 'mainClass':
                    "default${it.capitalize()}"()
            }
        }
    }

    /** Loads the default GEM installation directory and
     * JAR installation directory
     *
     */
    void defaultGems() {
        gemDir({project.jruby.gemInstallDir})
    }

    /** Makes the executable by adding a default main class
     *
     */
    @Incubating
    void defaultMainClass() {
        mainClass(DEFAULT_MAIN_CLASS)
    }

    /** Makes the executable by adding a default main class
     * which extracts the jar to temporary directory
     *
     */
    @Incubating
    void extractingMainClass() {
        mainClass(EXTRACTING_MAIN_CLASS)
    }

    void jrubyVersion(String version) {
      this.jrubyVersion = version
    }

    void jrubyMainsVersion(String version) {
      this.jrubyMainsVersion = version
    }

    @Deprecated
    void jruby(Closure cfg) {
        project.logger.info 'It is no longer necessary to use the jruby closure on a JRubyJar task.' 
        def cl = cfg.clone()
        cl.delegate = this
        cl.call()
    }

    @PackageScope
    void applyConfig() {
        if (scriptName == null) {
            scriptName = runnable()
        }
        if (scriptName == Type.LIBRARY) {
            if (mainClass != null) {
                throw new StopExecutionException('can not have mainClass for library')
            }
        }
        else {
            if (mainClass == null) {
                defaultMainClass()
            }
        }
        if (mainClass != null && scriptName != Type.LIBRARY) {
            with project.copySpec {
                from {
                    project.configurations.getByName( name ).collect {
                       project.zipTree( it )
                    }
                }
                include '**'
                exclude 'META-INF/MANIFEST.MF'
                // some pom.xml are readonly which creates problems
                // with zipTree on second run
                exclude 'META-INF/maven/**/pom.xml'
            }
            manifest = project.manifest {
                attributes 'Main-Class': mainClass
            }
        }
        if (scriptName != Type.RUNNABLE && scriptName != Type.LIBRARY) {
            File script = project.file(scriptName)
            with project.copySpec {
                from script.parent
                include script.name
                rename(script.name, 'jar-bootstrap.rb')
            }
        }
        
    }

    Type library() {
        Type.LIBRARY
    }

    Type runnable() {
        Type.RUNNABLE
    }

    JRubyJar() {
        appendix = 'jruby'
        
        File dir = project.file("${project.buildDir}/dirinfo/${name}")
        JRubyDirInfo dirInfo = new JRubyDirInfo(dir)

        from(dir) {
            include '**'
        }
        // TODO we should provide proper sourceSet instead !?
        from("${project.projectDir}/src/main/ruby")

        // TODO currently this picks ONLY the 'gems' configuration
        dependsOn 'jrubyPrepare'

        // TODO get rid of this and try to adjust the CopySpec for the gems
        // to exclude '.jrubydir'
        // there are other duplicates as well :(
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)

        project.gradle.taskGraph.addTaskExecutionListener(
            new TaskExecutionListener() {
                void afterExecute(Task task, TaskState state) {
                }

                void beforeExecute(Task task) {
                    if (task.name == this.name) {
                        task.getSource().visit {
                            dirInfo.add(it.relativePath)
                        }
                    }
                }
            }
        )
    }

    private Object scriptName
}
