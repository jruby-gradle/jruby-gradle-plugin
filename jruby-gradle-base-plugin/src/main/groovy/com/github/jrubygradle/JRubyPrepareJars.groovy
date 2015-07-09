package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import groovy.transform.PackageScope
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Incubating
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * Prepare embedded jars or all jars used by JRuby by telling the location
 * of those jars via JARS_HOME environment. The directory uses a maven
 * repository layout as per jar-dependencies.
 *
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 */
@Deprecated
class JRubyPrepareJars  extends DefaultTask {

    JRubyPrepareJars () {
        super()
        project.logger.info "The 'JRubyPrepareJars' task type is deprecated and will be removed in a future version. Please use 'JRubyPrepare' tasl type instead."
    }

    /** Target directory for JARs {@code outputDir + "/jars"}
     */
    @OutputDirectory
    File getOutputDir() { project.file(this.outputDir) }

    /** Sets the output directory
     *
     * @param f Output directory
     */
    void outputDir(Object file) { this.outputDir = file }

    @PackageScope
    Object outputDir

    @TaskAction
    void copy() {
        project.logger.info 'Obsolete tasks - does nothing anymore.' 
    }
}
