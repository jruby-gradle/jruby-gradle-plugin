package com.github.jrubygradle

import com.github.jrubygradle.core.JRubyAwareTask
import com.github.jrubygradle.internal.JRubyExecUtils
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.RelativePath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.ysb33r.grolifant.api.StringUtils

import static com.github.jrubygradle.internal.JRubyExecUtils.classpathFromConfiguration

/** Generate a LOAD_PATH Ruby file whichi is loadable by Ruby scripts when
 * performing local manual testing.
 *
 * @author Schalk W. Cronjé
 * @since 0.1.15
 */
@SuppressWarnings('UnnecessaryGetter')
@CompileStatic
class GenerateGradleRb extends DefaultTask implements JRubyAwareTask {

    GenerateGradleRb() {
        this.jruby = extensions.create(JRubyPluginExtension.NAME, JRubyPluginExtension, this)
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

    File getDestinationDir() {
        project.file(destinationDir)
    }

    @OutputFile
    File destinationFile() {
        new File(getDestinationDir(), getBaseName())
    }

    @Input
    String getBaseName() {
        StringUtils.stringize(baseName)
    }

    File getGemInstallDir() {
        project.file(this.gemInstallDir)
    }

    @TaskAction
    @CompileDynamic
    void generate() {
        Object source = getSourceFromResource()
        File destination = destinationFile().parentFile
        String path = classpathFromConfiguration(jruby.jrubyConfiguration).join(File.pathSeparator)
        String gemDir = getGemInstallDir().absolutePath
        String bootstrapName = getBaseName()
        logger.info("GenerateGradleRb - source: ${source}, destination: ${destination}, baseName: ${baseName}")
        project.copy {
            from(source) {
                /* In the case of this plugin existing in a zip (i.e. the
             * plugin jar) our `source` will be a ZipTree, so we only want
             * to pull in the template itself
             */
                include "**/${GenerateGradleRb.BOOTSTRAP_TEMPLATE}"
            }
            into destination
            fileMode = 0755
            includeEmptyDirs = false
            rename BOOTSTRAP_TEMPLATE, bootstrapName
            // Flatten the file into the destination directory so we don't copy
            // the file into: ${destination}/META-INF/gradle-plugins/gradle.rb
            eachFile { FileCopyDetails details ->
                details.relativePath = new RelativePath(true, [details.name] as String[])
            }

            filter ReplaceTokens, beginToken: '%%', endToken: '%%',
                tokens: [GEMFOLDER: gemDir, JRUBYEXEC_CLASSPATH: path]
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
}
