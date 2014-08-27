package com.github.jrubygradle

/**
 * Created by schalkc on 27/08/2014.
 */
class JRubyWarPlugin {
    void apply(Project project) {
        // MIGHT NEED: project.apply plugin: 'java', 'java-base' or 'war'

        project.configurations.create(JRubyWar.JRUBYWAR_CONFIG)

        // TODO: Should probably check whether it exists before creating it
        project.configurations {
            jrubyEmbeds
        }

            project.dependencies {
                jrubyEmbeds group: 'com.lookout', name: 'warbler-bootstrap', version: '1.+'
            }

        project.afterEvaluate {
            JRubyWar.updateJRubyDependencies(project)
        }

        // TODO: jarcache should rather be inside buildDir
        project.task('jrubyCacheJars', type: Copy) {
            group TASK_GROUP_NAME
            description 'Cache .jar-based dependencies into .jarcache/'

            from project.configurations.jrubyWar
            into ".jarcache"
            include '**/*.jar'
        }

        project.task('jrubyPrepare') {
            group TASK_GROUP_NAME
            description 'Pre-cache and prepare all dependencies (jars and gems)'
            dependsOn project.tasks.jrubyCacheJars, project.tasks.jrubyPrepareGems
        }

        // Only jRubyWar will depend on jrubyPrepare. Other JRubyWar tasks created by
        // build script authors will be under their own control
        // jrubyWar task will use jrubyWar as configuration
        project.task('jrubyWar', type: JRubyWar) {
            group JRubyPlugin.TASK_GROUP_NAME
            description 'Create a JRuby-based web archive'
            dependsOn project.tasks.jrubyPrepare
            classpath project.configurations.jrubyWar
        }

    }
}
