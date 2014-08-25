package com.lookout.jruby

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
  *
  * @author R. Tyler Croy
  */
class JRubyJar extends Jar {

    static final String mainClass = 'com.lookout.jruby.JarMain'
    static final String JRUBYJAR_CONFIG = 'jrubyJar'

    JRubyJar() {
        super()
        description 'Create a JRuby-based .jar file'

        // Bring in any compiled classes from our project
        from "$project.buildDir/classes/main"

        // By adding the JarMain class as the main-class we can have a
        // runnable jar
        manifest {
            attributes 'Main-Class' : mainClass
        }
    }

    /** Update dependencies on the project to include those necessary for
     * building JRubyJar's
     */
    static void updateJRubyDependencies(Project project) {
        project.dependencies {
            jrubyJar group: 'org.jruby', name: 'jruby-complete', version: project.jruby.defaultVersion
        }
    }
}
