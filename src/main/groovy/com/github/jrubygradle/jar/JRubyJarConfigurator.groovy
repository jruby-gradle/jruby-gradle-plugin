package com.github.jrubygradle.jar

import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import com.github.jrubygradle.GemUtils

/** Helper class to add extra methods to {@code Jar} tasks in order to add JRuby specifics.
 *
 * @author Schalk W. Cronjé
 */
class JRubyJarConfigurator {

    static final String DEFAULT_BOOTSTRAP_CLASS = 'com.github.jrubygradle.jar.bootstrap.JarMain'
    static final String SHADOW_JAR_TASK_CLASS = 'com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar'

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
            if(t.class.superclass.name == SHADOW_JAR_TASK_CLASS && t.name=='shadowJar') {
                t.configurations.add(project.configurations.getByName('jrubyJar'))
            }
        }
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
        archive.with GemUtils.gemCopySpec(properties,archive.project,dir)
    }

    /** Makes the JAR executable by setting a custom main class
     *
     * @param className Name of main class
     */
    @Incubating
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
    @Incubating
    void defaultMainClass() {
        mainClass(DEFAULT_BOOTSTRAP_CLASS)
    }

    boolean isShadowJar() {
        shadowJar
    }

    private JRubyJarConfigurator(Jar a) {
        archive = a
        if (a.class.name == SHADOW_JAR_TASK_CLASS || a.class.superclass.name == SHADOW_JAR_TASK_CLASS) {
            shadowJar = true
        }
    }

    private Jar archive
    private boolean shadowJar = false
}
