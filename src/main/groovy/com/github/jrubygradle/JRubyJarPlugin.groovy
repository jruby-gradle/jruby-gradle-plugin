package com.github.jrubygradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * @author Schalk W. Cronjé
 */
class JRubyJarPlugin implements Plugin<Project> {
    void apply(Project project) {

        project.apply plugin : 'com.github.jruby-gradle.base'

        project.configurations.maybeCreate('jrubyEmbeds')
        project.configurations.maybeCreate('testGems')
        project.configurations.maybeCreate('runtimeGems')

        project.dependencies {
            jrubyEmbeds group: 'com.lookout', name: 'warbler-bootstrap', version: '1.0.0'
        }

// TODO: This will depend on which plugin we pull in
//            // In order to update the testing cycle we need to tell unit tests where to
//            // find GEMs. We are assuming that if someone includes this plugin, that they
//            // will be writing tests that includes jruby and that they might need some
//            // GEMs as part of the tests.
//            project.tasks.test {
//                environment GEM_HOME : project.extensions.getByName('jruby').gemInstallDir
//                dependsOn 'jrubyPrepareGems'
//            }


       if(!Jar.metaClass.respondsTo(Jar.class,'jruby',Closure)) {
           Jar.metaClass.jruby = { Closure extraConfig ->
               JRubyJarConfigurator.configureArchive(delegate,extraConfig)
           }
       }

        project.afterEvaluate {
            JRubyJarConfigurator.afterEvaluateAction(project)
        }
    }


}
