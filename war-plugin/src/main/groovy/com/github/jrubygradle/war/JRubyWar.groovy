/*
 * Copyright (c) 2014-2020, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.war

import com.github.jrubygradle.internal.core.Transform
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
            Transform.toList(project.configurations.jrubyEmbeds) {
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
