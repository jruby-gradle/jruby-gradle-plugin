package com.github.jrubygradle.jar

/*
 * These two internal imports from the Shadow plugin are unavoidable because of
 * the expected internals of ShadowCopyAction
 */
import com.github.jengelman.gradle.plugins.shadow.internal.DefaultZipCompressor
import com.github.jengelman.gradle.plugins.shadow.internal.ZipCompressor

import com.github.jrubygradle.JRubyPrepare
import com.github.jrubygradle.jar.internal.JRubyDirInfoTransformer
import com.github.jrubygradle.jar.internal.JRubyJarCopyAction
import groovy.transform.PackageScope
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.InvalidUserDataException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.ZipEntryCompression

/**
 * JRubyJar creates a Java Archive with Ruby code packed inside of it.
 *
 * The most common use-case is when packing a self-contained executable jar
 * which would contain your application code, the JRuby runtime and a launcher
 * library to set up the runtime when the jar is executed.
 *
 * @author Christian Meier
 */
@SuppressWarnings('UnnecessaryGetter')
class JRubyJar extends Jar {
    enum Type {
        RUNNABLE, LIBRARY
    }

    static final String DEFAULT_JRUBYJAR_CONFIG = 'jrubyJar'
    static final String DEFAULT_MAIN_CLASS = 'org.jruby.mains.JarMain'
    static final String EXTRACTING_MAIN_CLASS = 'org.jruby.mains.ExtractingMain'
    static final String DEFAULT_JRUBY_MAINS = '0.6.1'

    /**
     * @return Directory that the dependencies for this project will be staged into
     */
    File getGemDir() {
        return prepareTask.outputDir
    }

    /**
     * Return the project default unless set
     *
     * The reason that this is defined as a getter instead of just setting
     * {@code jrubyVersion} at task construction-time is to ensure that if a user
     * modifies the jrubyVersion on the project after we have instantiated, that we still
     * respect this setting
     * */
    String getJrubyVersion() {
        if (embeddedJRubyVersion == null) {
            return project.jruby.defaultVersion
        }
        return embeddedJRubyVersion
    }

    /**
     * Set a custom version of JRuby to embed within the JRubyJar.
     *
     * @param version String representing a valid JRuby version
     */
    @Input
    void jrubyVersion(String version) {
        logger.info("setting jrubyVersion to ${version} from ${embeddedJRubyVersion}")
        embeddedJRubyVersion = version
    }

    /**
     * Retrieve the version of <a
     * href="3https://github.com/jruby/jruby-mains">jruby-mains</a> configured
     * for this JRubyJar
     *
     * @return String representation of the version defaulted
     */
    @Input
    @Optional
    String getJrubyMainsVersion() {
        return embeddedJRubyMainsVersion
    }

    /**
     * Set the version of <a
     * href="3https://github.com/jruby/jruby-mains">jruby-mains</a>
     * to embed into the JRubyJar
     *
     * @param version a valid version of the jruby-mains library
     */
    void jrubyMainsVersion(String version) {
        logger.info("setting jrubyMainsVersion to ${version} from ${embeddedJRubyMainsVersion}")
        embeddedJRubyMainsVersion = version
    }

    /**
     * @return configured 'Main-Class' attribute for the JRubyJar
     */
    @Input
    @Optional
    String getMainClass() {
        return jarMainClass
    }

    /** Makes the JAR executable by setting a custom main class
     *
     * @param className Name of main class
     */
    void mainClass(final String className) {
        jarMainClass = className
        if (this.scriptName == null) {
            this.scriptName = runnable()
        }
    }

    /**
     * @return String representing the name of the {@code Configuration} which
     *  will be used by this task
     */
    @Input
    @Optional
    String getConfiguration() {
        return jarConfiguration
    }

    /**
     * Set the configuration for this task to use for embedding dependencies
     * within the JRubyJar
     *
     * @param newConfiguration String name of an existing configuration
     */
    void setConfiguration(String newConfiguration) {
        logger.info("using the ${newConfiguration} configuration for the ${name} task")
        jarConfiguration = newConfiguration
    }

    /**
     * @param newConfiguration {@code Configuration} object to use for
     *  embedding dependencies
     */
    void setConfiguration(Configuration newConfiguration) {
        setConfiguration(newConfiguration.name)
    }

    void initScript(final Object scriptName) {
        this.scriptName = scriptName
    }

    /**
     * Sets the defaults.
     *
     * Unrecognised values are silently discarded
     *
     * @param defs A list of defaults. Currently {@code gems} and {@code mainClass} are the only recognised values.
     * @deprecated This method is no longer very useful, just use {@link defaultMainClass} instead
     */
    @Deprecated
    void defaults(final String... defs) {
        defs.each { String it ->
            switch (it) {
                case 'mainClass':
                    return "default${it.capitalize()}"()
                default:
                    logger.error("${this} { defaults '${it}' } is a no-op")
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
        } else if (mainClass == null) {
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

            manifest.attributes 'Main-Class': mainClass
        }

        if (scriptName != Type.RUNNABLE && scriptName != Type.LIBRARY) {
            File script = project.file(scriptName)
            if (!script.exists()) {
                throw new InvalidUserDataException("initScript ${script} does not exists")
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

    /** Update the staging directory and tasks responsible for setting it up */
    void updateStageDirectory() {
        File dir = project.file("${project.buildDir}/dirinfo/${configuration}")

        prepareTask.dependencies project.configurations.maybeCreate(configuration)
        prepareTask.outputDir dir

        logger.info("${this} including files in ${dir}")
        from(dir) {
            include 'specifications/**', 'gems/**', 'jars/**', 'bin/**', 'Jars.lock'
        }
    }

    /**
     * Provide a custom {@link CopyAction} to insert .jrubydir files into the archive.
     *
     * This is currently relying on lots of shadow plugin internals, be very
     * careful modifying this function :)
     *
     * @return instance of a CopyAction to perform the copy into the archive
     */
    @Override
    protected CopyAction createCopyAction() {
        return new JRubyJarCopyAction(getArchivePath(),
                getInternalCompressor(),
                null, /* DocumentationRegistry */
                'utf-8', /* encoding */
                [new JRubyDirInfoTransformer()], /* transformers */
                [], /* relocators */
                mainSpec.buildRootResolver().getPatternSet(), /* patternSet */
                false, /* preserveFileTimestamps */
                false, /* minimizeJar */
                null /* unusedTracker */
                )

    }

    @Internal
    protected ZipCompressor getInternalCompressor() {
        switch (entryCompression) {
            case ZipEntryCompression.DEFLATED:
                return new DefaultZipCompressor(this.zip64, ZipOutputStream.DEFLATED)
            case ZipEntryCompression.STORED:
                return new DefaultZipCompressor(this.zip64, ZipOutputStream.STORED)
            default:
                throw new IllegalArgumentException(String.format('Unknown Compression type %s', entryCompression))
        }
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
        return baseName.replaceAll('(?i)jruby', 'JRuby').capitalize()
    }

    protected Object scriptName
    protected JRubyPrepare prepareTask
    protected String customConfigName
    protected String embeddedJRubyVersion
    protected String embeddedJRubyMainsVersion = DEFAULT_JRUBY_MAINS
    protected String jarConfiguration = DEFAULT_JRUBYJAR_CONFIG
    protected String jarMainClass
}
