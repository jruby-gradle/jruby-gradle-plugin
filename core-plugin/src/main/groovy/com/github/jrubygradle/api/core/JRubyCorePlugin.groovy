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
package com.github.jrubygradle.api.core

import com.github.jrubygradle.api.gems.GemResolverStrategy
import com.github.jrubygradle.internal.gems.GemVersionResolver
import groovy.transform.CompileStatic
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.ExtensionAware
import org.ysb33r.grolifant.api.core.ProjectOperations

/** Provides only a repository handler extensiosn for looking up rubygem
 * metadata.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class JRubyCorePlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        ProjectOperations.maybeCreateExtension(project)
        GemResolverStrategy gemGroups = project.extensions.create(GemResolverStrategy.NAME, GemResolverStrategy)

        ((ExtensionAware) project.repositories).extensions.create(
            RepositoryHandlerExtension.NAME,
            RepositoryHandlerExtension,
            project
        )

        project.configurations.all { Configuration cfg ->
            GemVersionResolver.addGemResolver(cfg, gemGroups, new GemVersionResolver(gemGroups, project.logger, cfg))
        }
    }
}
