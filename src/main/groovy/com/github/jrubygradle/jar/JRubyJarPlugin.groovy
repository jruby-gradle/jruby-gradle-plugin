package com.github.jrubygradle.jar

import com.github.jrubygradle.JRubyPlugin
import groovy.transform.PackageScope
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

/**
 * @author Schalk W. Cronjé
 */
class JRubyJarPlugin implements Plugin<Project> {

    void apply(Project project) {

        project.apply plugin : 'com.github.jruby-gradle.base'
        project.apply plugin : 'java-base'
        project.configurations.maybeCreate('jrubyEmbeds')
        project.configurations.maybeCreate('jrubyJar')

        updateTestTask(project)
        addDependentTasks(project)
        addJrubyExtensionToJar(project)
        addAfterEvaluateHooks(project)
    }

    @PackageScope
    void addDependentTasks(Project project) {
        try {
            Task t = project.tasks.getByName('jar')
            if( t instanceof Jar) {
                t.dependsOn 'jrubyPrepare'
            }
        } catch(UnknownTaskException) {
            project.tasks.whenTaskAdded { Task t ->
                if (t.name == 'jar' && t instanceof Jar) {
                    project.task('jrubyJar', type: Jar) {
                        dependsOn 'jrubyPrepare'
                        destinationDir = t.destinationDir
                        baseName = t.baseName
                        extension = t.extension
                        version = t.version
                        appendix = 'all'
                        from project.file("${project.buildDir}/dirinfo")
                        from project.zipTree("${t.destinationDir}/${t.archiveName}")
                    }
                }
            }
        }
    }

    @PackageScope
    void addJrubyExtensionToJar(Project project) {
        if(!Jar.metaClass.respondsTo(Jar.class,'jruby',Closure)) {
            Jar.metaClass.jruby = { Closure extraConfig ->
                JRubyJarConfigurator.configureArchive(delegate,extraConfig)
            }
        }
    }

    @PackageScope
    void addAfterEvaluateHooks(Project project) {
        project.afterEvaluate {
            project.dependencies {
                jrubyJar group: 'org.jruby', name: 'jruby-complete', version: project.jruby.defaultVersion
                // TODO remove hardcoded version to config
                jrubyJar group: 'de.saumya.mojo', name: 'jruby-mains', version: '0.2.0'
            }
            project.tasks.withType(Jar) { task ->
                if (task.name == 'jar') {
                    finalizedBy( 'jrubyJar' )

                    def newLine = System.getProperty("line.separator")
                    def dirsCache = [:]
                    def dirInfo = project.file("${project.buildDir}/dirinfo")
                    dirInfo.deleteDir()
                    eachFile { FileCopyDetails details ->
                        if (details.relativePath.lastName != '.jrubydir') {
                            def path = null
                            details.relativePath.segments.each {
                                if (path == null) {
                                    path = new File(it)
                                }
                                else {
                                    path = new File(path, it)
                                }
                                def file = new File(dirInfo,
                                                    path.parent == null ? '.jrubydir' :
                                                    new File(path.parent, '.jrubydir').path)
                                def name = path.name
                                def dir = file.parentFile
                                def dirs = dirsCache[dir]
                                if (dirs == null) {
                                    dirs = dirsCache[dir] = []
                                }
                                if (!dirs.contains(name)) {
                                    dirs << name
                                    dir.mkdirs()
                                    if (file.exists()) {
                                        file.append(name + newLine)
                                    }
                                    else {
                                        file.write(name + newLine)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @PackageScope
    void updateTestTask(Project project) {
        // In order to update the testing cycle we need to tell unit tests where to
        // find GEMs. We are assuming that if someone includes this plugin, that they
        // will be writing tests that includes jruby and that they might need some
        // GEMs as part of the tests.
        def testConfiguration = { Task t ->
            environment GEM_HOME : project.extensions.getByName('jruby').gemInstallDir
            environment JARS_HOME : project.extensions.getByName('jruby').jarInstallDir
            dependsOn 'jrubyPrepare'
        }

        try {
            Task t = project.tasks.getByName('test')
            if( t instanceof Test) {
                project.configure(t,testConfiguration)
            }
        } catch(UnknownTaskException) {
            project.tasks.whenTaskAdded { Task t ->
                if(t.name == 'test' && t instanceof Test) {
                    project.configure(t,testConfiguration)
                }
            }
        }
    }
}
