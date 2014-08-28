package com.github.jrubygradle.jar

import com.github.jrubygradle.internal.WarblerBootstrap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

/**
 * @author Schalk W. Cronjé
 */
class JRubyJarPlugin implements Plugin<Project> {
    void apply(Project project) {

        project.apply plugin : 'com.github.jruby-gradle.base'
        project.configurations.maybeCreate('jrubyEmbeds')

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

       if(!Jar.metaClass.respondsTo(Jar.class,'jruby',Closure)) {
           Jar.metaClass.jruby = { Closure extraConfig ->
               JRubyJarConfigurator.configureArchive(delegate,extraConfig)
           }
       }

        project.afterEvaluate {
            WarblerBootstrap.addDependency(project)
            JRubyJarConfigurator.afterEvaluateAction(project)
        }
    }


}
