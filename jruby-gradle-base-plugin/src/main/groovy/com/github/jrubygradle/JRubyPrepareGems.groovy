package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/**
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 */
@Deprecated
class JRubyPrepareGems  extends JRubyPrepare {

  JRubyPrepareGems () {
        super()
        project.logger.info "The 'JRubyPrepareGems' task type is deprecated and will be removed in a future version. Please use 'JRubyPrepare' tasl type instead."
    }

}

