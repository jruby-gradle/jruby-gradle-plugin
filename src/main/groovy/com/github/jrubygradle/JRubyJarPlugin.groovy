package com.github.jrubygradle

/**
 * Created by schalkc on 27/08/2014.
 */
class JRubyJarPlugin {
    void apply(Project project) {
        // MIGHT NEED: project.apply plugin: 'java', 'java-base'

        project.configurations.create(JRubyWar.JRUBYWAR_CONFIG)

        // TODO: Should probably check whether it exists before creating it
        project.configurations {
            jrubyEmbeds
        }

        project.dependencies {
            jrubyEmbeds group: 'com.lookout', name: 'warbler-bootstrap', version: '1.+'
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

        project.task('jrubyJar', type: JRubyJar) {
            group JRubyPlugin.TASK_GROUP_NAME
            dependsOn project.tasks.jrubyPrepare
            dependsOn project.tasks.classes
        }

    }


}
