package com.github.jrubygradle.jar

import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import com.github.jrubygradle.GemUtils

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
        jar().with GemUtils.gemCopySpec(properties,archive.project,dir)
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
        jar().with GemUtils.jarCopySpec(archive.project, dir)
    }

    /** Makes the JAR executable by setting a custom main class
     *
     * @param className Name of main class
     */
    @Incubating
    void mainClass(final String className) {
        jar().with {
            manifest {
                attributes 'Main-Class': className
            }
        }
        jar().with archive.project.copySpec {
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

    @Incubating
    void initScript(final String scriptName) {
        def script = archive.project.file(scriptName)
        jar().with archive.project.copySpec {
            from(script.parent)
            include script.name
            rename { 'jar-bootstrap.rb' }
        }
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

    private JRubyJarConfigurator(Jar a) {
        archive = a
    }

    private Jar jar() {
        return archive.project.tasks.getByName('jrubyJar')
    }

    private Jar archive
}
