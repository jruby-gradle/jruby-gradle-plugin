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

import org.gradle.api.Plugin
import org.gradle.api.Project

import com.github.jrubygradle.JRubyPlugin

/**
 * Created by schalkc on 27/08/2014.
 */
class JRubyWarPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.apply plugin: 'war'
        project.apply plugin: 'com.github.jruby-gradle.base'
        project.configurations.create(JRubyWar.JRUBYWAR_CONFIG)
        project.configurations.maybeCreate('jrubyEmbeds')

        project.afterEvaluate {
            JRubyWar.updateJRubyDependencies(project)

            project.dependencies {
                jrubyEmbeds group: 'com.github.jruby-gradle', name: 'warbler-bootstrap', version: '0.2.0+'
            }
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
