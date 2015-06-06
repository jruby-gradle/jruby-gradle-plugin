package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 */
class JRubyPrepareGems  extends DefaultTask {

    /** Target directory for GEMs. Extracted GEMs should end up in {@code outputDir + "/gems"}
     */
    @OutputDirectory
    File outputDir


    @InputFiles
    FileCollection getGems() {
        GemUtils.getGems(project.files(this.gems))
    }

    /** Sets the output directory
     *
     * @param f Output directory
     */
    void outputDir(Object f) {
        this.outputDir = project.file(f)
    }

    /** Adds gems to be prepared
     *
     * @param f A file, directory, configuration or list of gems
     */
    void gems(Object f) {
        if (this.gems == null) {
            this.gems = []
        }
        this.gems.add(f)
    }

    @TaskAction
    void copy() {
        File jrubyJar = JRubyExecUtils.jrubyJar(project.configurations.getByName(JRubyExec.JRUBYEXEC_CONFIG))
        GemUtils.extractGems(project, jrubyJar, getGems(), outputDir, GemUtils.OverwriteAction.SKIP)
    }

    private List<Object> gems
}

