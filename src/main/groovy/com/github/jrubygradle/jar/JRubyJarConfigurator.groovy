package com.github.jrubygradle.jar

import groovy.transform.PackageScope
import org.gradle.api.Incubating
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.bundling.Jar
import com.github.jrubygradle.GemUtils

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
            if (t.manifest.attributes.containsKey('Main-Class')) {
                if (t.manifest.attributes.'Main-Class' == JRubyJarConfigurator.DEFAULT_MAIN_CLASS) {
                    t.with {
                        from({ project.configurations.jrubyEmbeds.collect { project.zipTree(it) } }) {
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
        maybeAddExtraManifest()
        archive.with {
            manifest {
                attributes 'Main-Class': className
            }
            metaInf {
                from { this.getDependencies() }
                into 'lib'
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
        mainClass(DEFAULT_MAIN_CLASS)
    }

    /** Adds a configuration to the list of dependencies that will be packed when creating an executable jar
     * This method is ignored if {@code mainClass} is not set.
     *
     * @param name Name of configuration
     */
    @Incubating
    void configuration(String name) {
        this.configuration(archive.project.configurations.getByName(name))
    }

    /** Adds a configuration to the list of dependencies that will be packed when creating an executable jar
     * This method is ignored if {@code mainClass} is not set.
     *
     * @param name Configuration
     */
    @Incubating
    void configuration(Configuration config) {
        if(this.configurations==null) {
            this.configurations = []
        }
        this.configurations.add(config)
    }

    private JRubyJarConfigurator(Jar a) {
        archive = a
    }

    private Set<File> getDependencies() {
        Set<File> tmp = []
        if(configurations == null) {
            archive.project.configurations.each { Configuration cfg ->
                if( ['jrubyJar','compile','runtime'].contains(cfg.name) ) {
                    tmp.addAll(cfg.files)
                }
            }
        } else {
            configurations.each {
                tmp.addAll(it.files)
            }
        }
        return tmp
    }

    private void maybeAddExtraManifest() {
        if(extraManifest == null) {
            extraManifest = new File(archive.project.buildDir,extraManifestName)
            String taskName = "${archive.name}ExtraManifest"
            Task task = archive.project.tasks.create(taskName)
            task << {
                String libs = task.inputs.files.collect { File f -> "lib/${f.name}" }.join(' ')
                extraManifest.parentFile.mkdirs()
                extraManifest.text = "Class-Path: ${libs}\n"
            }
            task.outputs.file(extraManifest)
            task.inputs.files({this.getDependencies()})
            archive.dependsOn task
            archive.manifest.from({task.outputs.files.singleFile})
        }
    }

    private String getExtraManifestName() {
        "tmp/${archive.name}-extraManifest.mf"
    }

    private Jar archive
    private List<Configuration> configurations
    private File extraManifest
}
