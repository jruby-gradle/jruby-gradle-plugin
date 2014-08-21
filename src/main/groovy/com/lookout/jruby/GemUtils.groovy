package com.lookout.jruby

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicateFileCopyingException

/** A collection of utilities to manipulate GEMs
 *
 * @author R Tyler Croy
 * @author Schalk W. Cronjé
 */
class GemUtils {
    enum OverwriteAction { FAIL, SKIP, OVERWRITE }

    static Boolean extractGem(Project p, File gem) {
        String gemName = gemFullNameFromFile(gem.getName())
        // Wherever our gems will be installed, we need to make sure the
        // directory exists since `p.exec` will silently fail
        new File(p.gemInstallDir).mkdirs()
        File extractDir = new File("${p.gemInstallDir}/gems/${gemName}")

        if (extractDir.exists()) {
            return
        }

        p.exec {
            executable "gem"
            args 'install', gem, "--install-dir=${p.gemInstallDir}", '--no-ri', '--no-rdoc'
        }
    }

    /** Extracts a gem to a folder
     *
     * @param project Project instance
     * @param jRubyClasspath Where to find the jruby-complete jar (FileCollection, File or Configuration)
     * @param gem Gem file to extract
     * @param destDir Directory to extract to
     * @param overwrite Allow overwrite of an existing gem folder
     * @return
     */
    static void extractGem(Project project,
                            def jRubyClasspath,
                            File gem,
                            File destDir,
                            GemUtils.OverwriteAction overwrite) {
        String gemName = gemFullNameFromFile(gem.name)
        File extractDir = new File(destDir, gemName)

        if (extractDir.exists()) {

            switch (overwrite) {
                case GemUtils.OverwriteAction.SKIP:
                    return
                case GemUtils.OverwriteAction.OVERWRITE:
                    project.delete extractDir
                    break
                case GemUtils.OverwriteAction.FAIL:
                    throw new DuplicateFileCopyingException("Gem ${gem.name} already exists")
            }
        }

        if( jRubyClasspath instanceof Configuration) {
            Set<File> cp = jRubyClasspath.files
            jRubyClasspath = cp.find { it.name.startsWith('jruby-complete-') }
        }

        project.javaexec {
            setEnvironment [:]
            main 'org.jruby.Main'
            classpath jRubyClasspath
            args '-S', 'gem', 'install', gem, "--install-dir=${destDir.absolutePath}", '-N'
        }
    }

    static void extractGems(Project project,def jRubyClasspath, Configuration gemConfig,File destDir,GemUtils.OverwriteAction action ) {
        gemConfig.files.findAll { File f ->
            f.name.endsWith('.gem')
        }.each { File f ->
            GemUtils.extractGem(project,jRubyClasspath,f,destDir,action)
        }

    }

    /** Take the given .gem filename (e.g. rake-10.3.2.gem) and just return the
     * gem "full name" (e.g. rake-10.3.2)
     */
    static String gemFullNameFromFile(String filename) {
        return filename.replaceAll(~".gem", "")
    }
}
