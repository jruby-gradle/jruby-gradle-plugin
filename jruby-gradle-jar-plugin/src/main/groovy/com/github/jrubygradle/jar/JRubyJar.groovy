package com.github.jrubygradle.jar

import com.github.jrubygradle.JRubyPrepare
import com.github.jrubygradle.jar.internal.JRubyDirInfo
import groovy.transform.PackageScope
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Input
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
    static final String DEFAULT_MAIN_CLASS = 'org.jruby.mains.JarMain'
    static final String EXTRACTING_MAIN_CLASS = 'org.jruby.mains.ExtractingMain'
    static final String DEFAULT_JRUBY_MAINS = '0.4.0'

    protected String jrubyVersion

    /**
     * Return the project default unless set
     *
     * The reason that this is defined as a getter instead of just setting
     * {@code jrubyVersion} at task construction-time is to ensure that if a user
     * modifies the jrubyVersion on the project after we have instantiated, that we still
     * respect this setting
     * */
    String getJrubyVersion() {
        if (jrubyVersion == null) {
            return project.jruby.defaultVersion
        }
        return jrubyVersion
    }

    @Input
    void jrubyVersion(String version) {
        logger.info("setting jrubyVersion to ${version} from ${jrubyVersion}")
        this.jrubyVersion = version
        addEmbeddedDependencies(project.configurations.maybeCreate(configuration))
    }

    @Input
    @Optional
    String jrubyMainsVersion = DEFAULT_JRUBY_MAINS

    void jrubyMainsVersion(String version) {
        logger.info("setting jrubyMainsVersion to ${version} from ${jrubyMainsVersion}")
        jrubyMainsVersion = version
        addEmbeddedDependencies(project.configurations.maybeCreate(configuration))
    }

    /** Return the directory that the dependencies for this project will be staged into */
    File getGemDir() {
        return prepareTask.outputDir
    }

    @Input
    String mainClass

    @Input
    @Optional
    String configuration = DEFAULT_JRUBYJAR_CONFIG

    void setConfiguration(String newConfiguration) {
        logger.info("using the ${newConfiguration} configuration for the ${name} task")
        configuration = newConfiguration
    }

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
            Configuration embeds = project.configurations.findByName(customConfigName)

            with project.copySpec {
                embeds.each { File embed ->
                    logger.info("unzipping ${embed} in the jar")
                    /* We nede to extract the class files from jruby-mains in order to properly run */
                    from { project.zipTree(embed) }
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
            if (!script.exists()) {
                throw new InvalidUserDataException("initScript ${script} does not exists");
            }
            with project.copySpec {
                from script.parent
                include script.name
                rename(script.name, 'jar-bootstrap.rb')
            }
        }
        updateStageDirectory()

    }

    Type library() {
        Type.LIBRARY
    }

    Type runnable() {
        Type.RUNNABLE
    }

    JRubyJar() {
        appendix = 'jruby'
        addEmbeddedDependencies(project.configurations.maybeCreate(DEFAULT_JRUBYJAR_CONFIG))
        /* Make sure our default configuration is present regardless of whether we use it or not */
        prepareTask = project.task("prepare${prepareNameForSuffix(name)}", type: JRubyPrepare)
        dependsOn prepareTask

        // TODO get rid of this and try to adjust the CopySpec for the gems
        // to exclude '.jrubydir'
        // there are other duplicates as well :(
        setDuplicatesStrategy(DuplicatesStrategy.EXCLUDE)
        customConfigName = "jrubyJarEmbeds-${hashCode()}"

        project.afterEvaluate {
            addJRubyDependency()
            applyConfig()
        }
    }

    /**
     * Adds our jruby-complete to a custom configuration only so it can be
     * safely unzipped later when we build the jar
     */
    void addJRubyDependency() {
        project.configurations.maybeCreate(customConfigName)
        logger.info("adding the dependency jruby-complete ${getJrubyVersion()} to jar")
        project.dependencies.add(customConfigName, "org.jruby:jruby-complete:${getJrubyVersion()}")
        logger.info("adding the dependency jruby-mains ${getJrubyMainsVersion()} to jar")
        project.dependencies.add(customConfigName, "org.jruby.mains:jruby-mains:${getJrubyMainsVersion()}")
    }

    /** Add the necessary JRuby dependencies to the specified {@code org.gradle.api.artifacts.Configuration} */
    void addEmbeddedDependencies(Configuration config) {
        /* To ensure that we can load our jars properly, we should always have
         * jar-dependencies in our resolution graph */
        project.dependencies.add(config.name, 'rubygems:jar-dependencies:0.1.15')
    }

    /** Update the staging directory and tasks responsible for setting it up */
    void updateStageDirectory() {
        File dir = project.file("${project.buildDir}/dirinfo/${configuration}")
        dirInfo = new JRubyDirInfo(dir)

        prepareTask.dependencies project.configurations.maybeCreate(configuration)
        prepareTask.outputDir dir

        logger.info("${this} including files in ${dir}")
        from(dir) {
            include '**'
        }

        project.gradle.taskGraph.addTaskExecutionListener(
            new TaskExecutionListener() {
                void afterExecute(Task task, TaskState state) {
                    /* no op */
                    return
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
    protected String customConfigName
}
