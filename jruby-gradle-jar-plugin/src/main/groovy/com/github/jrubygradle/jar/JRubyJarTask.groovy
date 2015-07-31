package com.github.jrubygradle.jar

import com.github.jrubygradle.GemUtils
import com.github.jrubygradle.JRubyPrepare
import com.github.jrubygradle.jar.internal.JRubyDirInfo
import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskState
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.StopExecutionException

/**
 * @author Christian Meier
 */
class JRubyJar extends Jar {
    enum Type { RUNNABLE, LIBRARY }

    static final String DEFAULT_JRUBYJAR_CONFIG = 'jrubyJar'
    static final String DEFAULT_MAIN_CLASS = 'de.saumya.mojo.mains.JarMain'
    static final String EXTRACTING_MAIN_CLASS = 'de.saumya.mojo.mains.ExtractingMain'

    protected String jrubyVersion

    /** Return the project default unless set */
    String getJrubyVersion() {
        if (jrubyVersion == null) {
            return project.jruby.defaultVersion
        }
        return jrubyVersion
    }

    @Input
    void jrubyVersion(String version) {
        this.jrubyVersion = version
    }

    @Input
    String jrubyMainsVersion = '0.3.0'

    void jrubyMainsVersion(String version) {
        this.jrubyMainsVersion = version
    }


    @Input
    String mainClass

    @Input
    @Optional
    String configuration


    /** Makes the JAR executable by setting a custom main class
     *
     * @param className Name of main class
     */
    void mainClass(final String className) {
        this.mainClass = className
        if (this.scriptName == null) {
            this.scriptName = runnable()
        }
    }

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
                case 'mainClass':
                    "default${it.capitalize()}"()
            }
        }
    }

    /** Makes the executable by adding a default main class
     */
    void defaultMainClass() {
        mainClass(DEFAULT_MAIN_CLASS)
    }

    /** Makes the executable by adding a default main class
     * which extracts the jar to temporary directory
     */
    void extractingMainClass() {
        mainClass(EXTRACTING_MAIN_CLASS)
    }

    @PackageScope
    void applyConfig() {
        updateDependencies()

        if (scriptName == null) {
            scriptName = runnable()
        }

        if (scriptName == Type.LIBRARY) {
            if (mainClass != null) {
                throw new StopExecutionException('can not have mainClass for library')
            }
        }
        else if (mainClass == null) {
            defaultMainClass()
        }

        if (mainClass != null && scriptName != Type.LIBRARY) {
            /* NOTE: this should go away or be reafactored, GemUtils.setupJars excludes jruby */
            Configuration c = project.configurations.findByName(configuration)
            File jruby = c.find { it.name.matches(/jruby-complete-(.*).jar/) }
            File jrubyMains = c.find { it.name.matches(/jruby-mains-(.*).jar/) }
            logger.info("unzipping ${jrubyMains} in the jar")

            with project.copySpec {
                /* We nede to extract the class files from jruby-mains in order to properly run */
                //from { project.zipTree(jrubyMains) }
                //from { project.zipTree(jruby) }
                /* TEMPORARY HACK: https://github.com/jruby-gradle/jruby-gradle-plugin/issues/165 */
                from { c.findAll { it.name.matches(/(.*).jar/) }.collect { project.zipTree(it) } }
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
            if (!script.exists()) {
                throw new InvalidUserDataException("initScript ${script} does not exists");
            }
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
        /* Make sure our default configuration is present regardless of whether we use it or not */
        project.configurations.maybeCreate(DEFAULT_JRUBYJAR_CONFIG)
        prepareTask = project.task("prepare${prepareNameForSuffix(name)}", type: JRubyPrepare)
        dependsOn prepareTask

        // TODO get rid of this and try to adjust the CopySpec for the gems
        // to exclude '.jrubydir'
        // there are other duplicates as well :(
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)

        project.afterEvaluate {
            applyConfig()
        }
    }

    void updateDependencies() {
        if (configuration == null) {
            configuration = DEFAULT_JRUBYJAR_CONFIG
        }
        Configuration taskConfiguration = project.configurations.maybeCreate(configuration)
        project.dependencies.add(configuration, "org.jruby:jruby-complete:${getJrubyVersion()}")
        project.dependencies.add(configuration, "de.saumya.mojo:jruby-mains:${getJrubyMainsVersion()}")

        File dir = project.file("${project.buildDir}/dirinfo/${configuration}")
        dirInfo = new JRubyDirInfo(dir)

        prepareTask.dependencies taskConfiguration
        prepareTask.outputDir dir

        logger.info("${this} including files in ${dir}")
        from(dir) {
            include '**'
        }

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

    /**
     * Prepare a name for suffixing to a task name, i.e. with a baseName of
     * "foo" if I need a task to prepare foo, this will return 'Foo' so I can
     * make a "prepareFoo" task and it cases properly
     *
     * This method has a special handling for the string 'jruby' where it will
     * case it properly like "JRuby" instead of "Jruby"
     */
    private String prepareNameForSuffix(String baseName) {
        return baseName.replaceAll("(?i)jruby", 'JRuby').capitalize()
    }

    protected Object scriptName
    protected JRubyDirInfo dirInfo
    protected JRubyPrepare prepareTask
}
