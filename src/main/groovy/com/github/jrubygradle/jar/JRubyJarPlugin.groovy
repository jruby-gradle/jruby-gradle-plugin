package com.github.jrubygradle.jar

import com.github.jrubygradle.JRubyPlugin
import groovy.transform.PackageScope
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.Test

/**
 * @author Schalk W. Cronjé
 */
class JRubyJarPlugin implements Plugin<Project> {

    static final String BOOTSTRAP_TASK_NAME = 'jrubyJavaBootstrap'

    void apply(Project project) {

        project.apply plugin : 'com.github.jruby-gradle.base'
        project.apply plugin : 'java-base'
        project.configurations.maybeCreate('compile')
        project.configurations.maybeCreate('jrubyEmbeds')
        project.configurations.maybeCreate('jrubyJar')

        updateTestTask(project)
        addCodeGenerationTask(project)
        addDependentTasks(project)
        addJrubyExtensionToJar(project)
        addAfterEvaluateHooks(project)
    }

    @PackageScope
    void addCodeGenerationTask(Project project) {

        Task stubTask = project.tasks.create( name: BOOTSTRAP_TASK_NAME, type : Copy )
        stubTask.extensions.create(
                'jruby',
                BootstrapClassExtension,
                stubTask
        )

        project.repositories {
            jcenter()
        }

        project.dependencies {
            compile group: 'org.jruby', name: 'jruby-complete', version: project.jruby.defaultVersion
        }

        project.configure(stubTask) {
            group JRubyPlugin.TASK_GROUP_NAME
            description 'Generates a JRuby Java bootstrap class'

            from({extensions.jruby.getSource()})
            into new File(project.buildDir,'generated/java')


            filter { String line ->
                line.replaceAll('%%LAUNCH_SCRIPT%%',extensions.jruby.initScript)
            }

            rename '(.+)\\.java\\.template','$1.java'

        }

        project.sourceSets.matching { it.name == "main" } .all {
            it.java.srcDir new File(project.buildDir,'generated/java')
        }
    }
    @PackageScope
    void addDependentTasks(Project project) {
        ['jar','shadowJar'].each { taskName ->
            try {
                Task t = project.tasks.getByName(taskName)
                if( t instanceof Jar) {
                    t.dependsOn 'jrubyPrepareGems'
                }
            } catch(UnknownTaskException) {
                project.tasks.whenTaskAdded { Task t ->
                    if (t.name == taskName && t instanceof Jar) {
                        t.dependsOn 'jrubyPrepareGems'
                    }
                }
            }
        }

        try {
            Task t = project.tasks.getByName('compileJava')
            if( t instanceof JavaCompile) {
                t.dependsOn BOOTSTRAP_TASK_NAME
            }
        } catch(UnknownTaskException) {
            project.tasks.whenTaskAdded { Task t ->
                if (t.name == 'compileJava' && t instanceof JavaCompile) {
                    t.dependsOn BOOTSTRAP_TASK_NAME
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
            }
            JRubyJarConfigurator.afterEvaluateAction(project)
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
            dependsOn 'jrubyPrepareGems'
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
