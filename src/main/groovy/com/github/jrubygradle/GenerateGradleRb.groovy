package com.github.jrubygradle

import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/** Generate a LOAD_PATH Ruby file whichi is loadable by Ruby scripts when performing
 * local manual testing.
 *
 * @author Schalk W. Cronjé
 * @since 0.1.14
 */
class GenerateGradleRb extends DefaultTask {

    private Object baseName = 'gradle.rb'
    private Object jarCacheDir
    private Object destinationDir = project.projectDir

    void destinationDir(Object dest) {
        this.destinationDir = dest
    }

    void baseName(Object name) {
        this.baseName=name
    }

    void jarCacheDir(Object dir) {
        this.jarCacheDir = dir
    }

    File getDestinationDir() {
        project.file(destinationDir)
    }

    @OutputFile
    File destinationFile() {
        new File(destinationDir,baseName)
    }

    @OutputFile
    File getJarCache() {
        new File(this.jarCacheDir ? project.file(this.jarCacheDir) : project.gradle.startParameter.projectCacheDir,'.jarCache')
    }

    @Input
    String getBaseName() {
        // not quite correct - does not properly allow for lazy evaluation
        baseName.toString()
    }


    @TaskAction
    void generate() {
        project.copy {
            from sourceFromResource
            into destinationFile.parentFile
            rename 'rubystub.template',getBaseName()
            filter { String line ->
                line.replaceAll('%%GEMFOLDER%%',project.file(project.configurations.jruby.gemInstallDir).absolutePath).
                replaceAll('%%JARCACHE%%',getJarCache.absolutePath)
            }
        }
    }

    private Object getSourceFromResource() {
        Object source
        Enumeration<URL> enumResources
        enumResources = this.class.classLoader.getResources( BOOTSTRAP_TEMPLATE_PATH)
        if(!enumResources.hasMoreElements()) {
            throw new GradleException ("Cannot find ${BOOTSTRAP_TEMPLATE_PATH} in classpath")
        } else {
            URI uri = enumResources.nextElement().toURI()
            String location = uri.getSchemeSpecificPart().replace('!/'+BOOTSTRAP_TEMPLATE_PATH,'')
            if(uri.scheme.startsWith('jar')) {
                location=location.replace('jar:file:','')
                source= project.zipTree(location)
            } else if(uri.scheme.startsWith('file')) {
                source= location.replace('file:','')
            } else {
                throw new GradleException("Cannot extract ${uri}")
            }
        }
        return source
    }

    private static final String BOOTSTRAP_TEMPLATE_PATH = 'META-INF/gradle-plugins/rubystub.template'
}
