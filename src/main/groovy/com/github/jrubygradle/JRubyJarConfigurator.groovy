package com.github.jrubygradle

import groovy.transform.PackageScope
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/** Helper class to add extra methods to {@code Jar} tasks in order to add JRuby specifics.
 *
 * @author Schalk W. Cronjé
 */
class JRubyJarConfigurator {

    static final String DEFAULT_MAIN_CLASS = 'com.lookout.jruby.JarMain'

    // This is used by JRubyJarPlugin to configure Jar classes
    @PackageScope
    static void configureArchive(Jar archive,Closure c) {
        JRubyJarConfigurator configurator = new JRubyJarConfigurator(archive)
        Closure configure = c.clone()
        configure.delegate = configurator
        configure()
    }

    @PackageScope
    static void afterEvaluateAction( Project project ) {
        project.tasks.withType(Jar) { t ->
            if(t.manifest.attributes.containsKey('Main-Class')) {
                if(t.manifest.attributes.'Main-Class' == JRubyJarConfigurator.DEFAULT_MAIN_CLASS) {
                    t.with {
                        from({project.configurations.jrubyEmbeds.collect {project.zipTree(it)}}) {
                            include '**'
                            exclude '**/WarMain.class'
                        }
                    }
                }
            }
        }
    }

    /** Adds a GEM installation directory
     */
    void gemDir(File f) {
        gemDir(f.absolutePath)
    }

    /** Adds a GEM installation directory
     * @param dir Source folder. Will be handled by {@code project.files(dir)}
    */
    void gemDir(Object dir) {
        archive.with {
            from(dir) {
                include '**'
                exclude 'cache/**'
                exclude 'gems/*/test/**'
            }
        }
    }

    /** Makes the JAR executable by setting a custom main class
     *
     * @param className Name of main class
     */
    void mainClass(final String className) {
        archive.with {
            manifest {
                attributes 'Main-Class': className
            }
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
                    "default${it.capitalize()}"()
            }
        }
    }

    /** Loads the default GEM installation directory
     *
     */
    void defaultGems() {
        gemDir({archive.project.jruby.gemInstallDir})
    }

    /** Makes the executable by adding a default main class
     *
     */
    void defaultMainClass() {
        mainClass(DEFAULT_MAIN_CLASS)
    }


    private JRubyJarConfigurator(Jar a) {
        archive = a
    }

    private Jar archive
}
