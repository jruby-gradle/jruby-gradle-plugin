package com.github.jrubygradle.jar

import com.github.jrubygradle.GemUtils
import com.github.jrubygradle.jar.internal.JRubyDirInfo

import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.StopExecutionException

/** Helper class to add extra methods to {@code Jar} tasks in order to add JRuby specifics.
 *
 * @author Schalk W. Cronjé
 * @author Christian Meier
 */
class JRubyJarConfigurator {

    static final String DEFAULT_MAIN_CLASS = 'de.saumya.mojo.mains.JarMain'
    static final String DEFAULT_EXTRACTING_MAIN_CLASS = 'de.saumya.mojo.mains.ExtractingMain'

    // This is used by JRubyJarPlugin to configure Jar classes
    @PackageScope
    static void configureArchive(Jar archive,Closure c) {
        JRubyJarConfigurator configurator = new JRubyJarConfigurator(archive)
        Closure configure = c.clone()
        configure.delegate = configurator
        configure()
    }

    /** Adds a GEM installation directory
     */
    void gemDir(def properties=[:],File f) {
        gemDir(properties,f.absolutePath)
    }

    /** Adds a GEM installation directory
     * @param Properties that affect how the GEM is packaged in the JAR. Currently only {@code fullGem} is
     * supported. If set the full GEM content will be packed, otherwise only a subset will be packed.
     * @param dir Source folder. Will be handled by {@code project.files(dir)}
    */
    void gemDir(def properties=[:],Object dir) {
        hasGemsJars = true
        def spec = GemUtils.gemCopySpec(properties,archive.project,dir)
        spec.exclude '.jrubydir'
        archive.with spec
    }

    /** Adds a GEM installation directory
     */
    void jarDir(File f) {
        jarDir(f.absolutePath)
    }

    /** Adds a JAR installation directory
     * @param dir Source folder. Will be handled by {@code project.files(dir)}
    */
    void jarDir(Object dir) {
         hasGemsJars = true
         archive.with GemUtils.jarCopySpec(archive.project, dir)
    }

    /** Makes the JAR executable by setting a custom main class
     *
     * @param className Name of main class
     */
    @Incubating
    void mainClass(final String className) {
        if (this.mainClassName != null) {
            throw new StopExecutionException('mainClass can be set only once')
        }
        this.mainClassName = className
    }

    @Incubating
    void initScript(final Object scriptName) {
      if (this.scriptName != null) {
        throw new StopExecutionException('initScript can be set only once')
      }
      if (scriptName != 'runnable' && scriptName != 'library') {
        File script = archive.project.file(scriptName)
        archive.with archive.project.copySpec {
          from script.parent
          include script.name
          rename(script.name, 'jar-bootstrap.rb')
        }
      }
      if (scriptName != 'library') {
        archive.with archive.project.copySpec {
          from {
            archive.project.configurations.jrubyJar.collect {
              archive.project.zipTree( it )
            }
          }
          include '**'
          exclude 'META-INF/MANIFEST.MF'
          // some pom.xml are readonly which creates problems
          // with zipTree on second run
          exclude 'META-INF/maven/**/pom.xml'
        }
      }
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
                case 'extractingMainClass':
                    "default${it.capitalize()}"()
            }
        }
    }

    /** Loads the default GEM installation directory and
     * JAR installation directory
     *
     */
    void defaultGems() {
        gemDir({archive.project.jruby.gemInstallDir})
        // gems depend on jars so we need to add meaningful default
        jarDir({archive.project.jruby.jarInstallDir})
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
    void defaultExtractingMainClass() {
        mainClass(DEFAULT_EXTRACTING_MAIN_CLASS)
    }

    void applyConfig() {
        if (scriptName == null) {
            throw new StopExecutionException('there is no initScript configured')
        }
        if (! hasGemsJars ) {
            defaultGems()
        }
        if (scriptName == 'library') {
            if (mainClassName != null) {
                throw new StopExecutionException('can not have mainClass for library')
            }
        }
        else {
            if (mainClassName == null) {
                mainClassName = DEFAULT_MAIN_CLASS
            }
            archive.with {
                manifest {
                    attributes 'Main-Class': mainClassName
                }
            }
        }
    }

    String library() {
        'library'
    }

    String runnable() {
        'runnable'
    }

    private JRubyJarConfigurator(final Jar archive) {
        this.archive = archive
        if (!(archive instanceof JRubyJar)) {
            initScript library()
        }
        File dir = archive.project.file("${archive.project.buildDir}/dirinfo/${archive.name}")
        JRubyDirInfo dirInfo = new JRubyDirInfo(dir)

        archive.from(dir) {
            include '**'
        }
        // TODO we should provide proper sourceSet instead !?
        archive.from("${archive.project.projectDir}/src/main/ruby")

        // TODO currently this picks ONLY the 'gems' configuration
        archive.dependsOn 'jrubyPrepare'

        // TODO get rid of this and try to adjust the CopySpec for the gems
        // to exclude '.jrubydir'
        // there are other duplicates as well :(
        archive.setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)

        archive.project.gradle.taskGraph.addTaskExecutionListener(
            new TaskExecutionListener() {
                void afterExecute(Task task, TaskState state) {
                }

                void beforeExecute(Task task) {
                    if (task == archive) {
                        task.getSource().visit {
                            dirInfo.add(it.relativePath)
                        }
                        applyConfig()
                    }
                }
            }
        )
    }

    private boolean hasGemsJars = false
    private Object scriptName
    private String mainClassName
    private Jar archive
}
