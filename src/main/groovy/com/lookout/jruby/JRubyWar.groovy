package com.lookout.jruby

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.bundling.War

/**
  *
  * @author R. Tyler Croy
  */
class JRubyWar extends War {

    static final String JRUBYWAR_MAINCLASS = 'com.lookout.jruby.WarMain'
    static final String JRUBYWAR_CONFIG = 'jrubyWar'

    /** Setting the main class allows for a runnable War.
     * By default the value is {@code JRUBYWAR_MAINCLASS}
     */
    @Input
    String mainClass = JRUBYWAR_MAINCLASS

    JRubyWar() {
        super()

        // Bring in any compiled classes from our project
        from "$project.buildDir/classes/main"

        // Bring our vendored gems into the created war file
        webInf {
            from project.jruby.gemInstallDir
            into 'gems'
        }

        // note that zipTree call is wrapped in closure so that configuration
        // is only resolved at execution time. This will take the embeds
        // from within the `jrubyEmbeds` configuration and dump them into the war
        from {
            project.configurations.jrubyEmbeds.collect {
                project.zipTree(it)
            }
        }
    }

    @Override
    void copy() {
        manifest {
            attributes 'Main-Class' : mainClass
        }
        super.copy()
    }

    /** Update dependencies on the project to include those necessary for
     * building JRubyWar's
     */
    static void updateJRubyDependencies(Project project) {
        project.dependencies {
            jrubyWar group: 'org.jruby', name: 'jruby-complete', version: project.jruby.defaultVersion
            jrubyWar (group: 'org.jruby.rack', name: 'jruby-rack', version: '1.1.+') {
                exclude module : 'jruby-complete'
            }
        }
    }
}
