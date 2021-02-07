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
package com.github.jrubygradle

import com.github.jrubygradle.api.core.JRubyAwareTask
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.ysb33r.grolifant.api.core.ProjectOperations
import org.ysb33r.grolifant.api.v4.StringUtils

import static com.github.jrubygradle.internal.JRubyExecUtils.classpathFromConfiguration

/** Generate a LOAD_PATH Ruby file which is loadable by Ruby scripts when
 * performing local manual testing.
 *
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 *
 * @since 0.1.15
 */
@SuppressWarnings('UnnecessaryGetter')
@CompileStatic
class GenerateGradleRb extends DefaultTask implements JRubyAwareTask {

    GenerateGradleRb() {
        this.jruby = extensions.create(JRubyPluginExtension.NAME, JRubyPluginExtension, this)
        this.projectOperations = ProjectOperations.create(project)
    }

    void destinationDir(Object dest) {
        this.destinationDir = dest
    }

    void baseName(Object name) {
        this.baseName = name
    }

    void setBaseName(Object name) {
        this.baseName = name
    }

    void gemInstallDir(Object dir) {
        this.gemInstallDir = dir
    }

    @Internal
    File getDestinationDir() {
        projectOperations.file(destinationDir)
    }

    @OutputFile
    File destinationFile() {
        new File(getDestinationDir(), getBaseName())
    }

    @Input
    String getBaseName() {
        StringUtils.stringize(baseName)
    }

    @Internal
    File getGemInstallDir() {
        projectOperations.file(this.gemInstallDir)
    }

    @Input
    protected String getGemInstallDirPath() {
        getGemInstallDir().absolutePath
    }

    @TaskAction
    @CompileDynamic
    @SuppressWarnings('DuplicateStringLiteral')
    void generate() {
        Object source = getSourceFromResource()
        File destination = destinationFile().parentFile
        String path = classpathFromConfiguration(jruby.jrubyConfiguration).join(File.pathSeparator)
        String gemDir = getGemInstallDirPath()
        String bootstrapName = getBaseName()
        String bootstrapTemplate = BOOTSTRAP_TEMPLATE
        logger.info("GenerateGradleRb - source: ${source}, destination: ${destination}, baseName: ${baseName}")
        projectOperations.copy { CopySpec cs ->
            cs.with {
                // In the case of this plugin existing in a zip (i.e. the plugin jar) our `source` will be a ZipTree,
                // so we only want to pull in the template itself
                from(source).include "**/${bootstrapTemplate}"

                into destination
                fileMode = 0755
                includeEmptyDirs = false
                rename bootstrapTemplate, bootstrapName
                // Flatten the file into the destination directory so we don't copy
                // the file into: ${destination}/META-INF/gradle-plugins/gradle.rb
                eachFile { FileCopyDetails details ->
                    details.relativePath = new RelativePath(true, [details.name] as String[])
                }

                filter ReplaceTokens, beginToken: '%%', endToken: '%%',
                    tokens: [GEMFOLDER: gemDir, JRUBYEXEC_CLASSPATH: path]
            }
        }
    }

    private Object getSourceFromResource() {
        Object source
        Enumeration<URL> enumResources
        enumResources = this.class.classLoader.getResources(BOOTSTRAP_TEMPLATE_PATH)

        if (enumResources.hasMoreElements()) {
            URI uri = enumResources.nextElement().toURI()
            String location = uri.schemeSpecificPart.replace('!/' + BOOTSTRAP_TEMPLATE_PATH, '')

            if (uri.scheme.startsWith('jar')) {
                location = location.replace('jar:file:', '')
                source = project.zipTree(location)
            } else if (uri.scheme.startsWith('file')) {
                source = location.replace('file:', '')
            } else {
                throw new GradleException("Cannot extract ${uri}")
            }
        } else {
            throw new GradleException("Cannot find ${BOOTSTRAP_TEMPLATE_PATH} in classpath")
        }
        return source
    }

    private static final String BOOTSTRAP_TEMPLATE = 'rubystub.template'
    private static final String BOOTSTRAP_TEMPLATE_PATH = 'META-INF/gradle-plugins/' + BOOTSTRAP_TEMPLATE

    private Object baseName = 'gradle.rb'
    private Object destinationDir = project.projectDir
    private Object gemInstallDir
    private final JRubyPluginExtension jruby
    private final ProjectOperations projectOperations
}
