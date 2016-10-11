package com.github.jrubygradle.war

import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.bundling.War

/**
  *
  * @author R. Tyler Croy
  */
class JRubyWar extends War {

    static final String JRUBYWAR_MAINCLASS = 'com.github.jrubygradle.warbler.WarMain'
    static final String JRUBYWAR_CONFIG = 'jrubyWar'

    /** Setting the main class allows for a runnable War.
     * By default the value is {@code JRUBYWAR_MAINCLASS}
     */
    @Input
    String mainClass = JRUBYWAR_MAINCLASS

    @Override
    void copy() {
        // Bring our vendored gems into the created war file
        webInf {
            from project.jruby.gemInstallDir
            into 'gems'
        }

        // Bring in any compiled classes from our project
        from "$project.buildDir/classes/main"

        // note that zipTree call is wrapped in closure so that configuration
        // is only resolved at execution time. This will take the embeds
        // from within the `jrubyEmbeds` configuration and dump them into the war
        from {
            project.configurations.jrubyEmbeds.collect {
                project.zipTree(it)
            }
        }

        manifest {
            attributes 'Main-Class' : mainClass
        }

        /* If we haven't been given a web.xml, let's pull the default one from
         * warbler-bootstrap
         */
        if (webXml == null) {
            project.configurations.compile.each {
                if (it.name =~ 'warbler-bootstrap') {
                    from project.zipTree(it).matching {
                        include 'WEB-INF/web.xml'
                    }
                }
            }
        }

        super.copy()
    }

    /** Update dependencies on the project to include those necessary for
     * building JRubyWar's
     */
    static void updateJRubyDependencies(Project project) {
        project.dependencies {
            jrubyWar "org.jruby:jruby-complete:${project.jruby.defaultVersion}"
            jrubyWar('org.jruby.rack:jruby-rack:[1.1.0,2.0)') {
                exclude module: 'jruby-complete'
            }
        }
    }
}
