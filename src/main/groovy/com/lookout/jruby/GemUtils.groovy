package com.lookout.jruby

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicateFileCopyingException
import org.gradle.api.file.FileCollection

/** A collection of utilities to manipulate GEMs
 *
 * @author R Tyler Croy
 * @author Schalk W. Cronjé
 */
class GemUtils {

    enum OverwriteAction { FAIL, SKIP, OVERWRITE }

    /** Given a FileCollection return a filtered FileCollection only containing GEMs
     *
     * @param fc Original FileCollection
     * @return Filtered FileCollection
     */
    static FileCollection getGems(FileCollection fc) {
        fc.filter { File f ->
            f.name.toLowerCase().endsWith('.gem')
        }
    }

    /** Extract a gem to the default {@code gemInstallDir} dreictory using the default jruby version
     *
     * @param project Current project
     * @param gem GEM to extract
     */
    static void extractGem(Project project, File gem) {
        extractGem(project,gem,new File(project.jruby.gemInstallDir),GemUtils.OverwriteAction.SKIP)
    }

    /** Extracts a gem to a folder using the default JRuby version
     *
     * @param project Project instance
     * @param gem Gem file to extract
     * @param destDir Directory to extract to
     * @param overwrite Allow overwrite of an existing gem folder
     */
    static void extractGem(Project project,
                           File gem,
                           File destDir,
                           GemUtils.OverwriteAction overwrite) {
        extractGem(project,project.configurations.jrubyExec,gem,destDir,overwrite)
    }

    /** Extracts a gem to a folder
     *
     * @param project Project instance
     * @param jRubyConfig Where to find the jruby-complete jar (FileCollection, File or Configuration)
     * @param gem Gem file to extract
     * @param destDir Directory to extract to
     * @param overwrite Allow overwrite of an existing gem folder
     */
    static void extractGem(Project project,
                           Configuration jRubyConfig,
                            File gem,
                            File destDir,
                            GemUtils.OverwriteAction overwrite) {
        Set<File> cp = jRubyConfig.files
        File jRubyClasspath = cp.find { it.name.startsWith('jruby-complete-') }
        extractGem(project,jRubyClasspath,gem,destDir,overwrite)
    }

    /** Extracts a gem to a folder
     *
     * @param project Project instance
     * @param jRubyClasspath Where to find the jruby-complete jar
     * @param gem Gem file to extract
     * @param destDir Directory to extract to
     * @param overwrite Allow overwrite of an existing gem folder
     */
    static void extractGem(Project project,
                            File jRubyClasspath,
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

        destDir.mkdirs()

        project.javaexec {
            setEnvironment [:]
            main 'org.jruby.Main'
            classpath jRubyClasspath
            args '-S', 'gem', 'install', gem, '--ignore-dependencies', "--install-dir=${destDir.absolutePath}", '-N'
        }
    }

    /** Extract Gems from a given configuration.
     *
     * @param project Current project
     * @param jRubyClasspath
     * @param gemConfig
     * @param destDir
     * @param action
     */
    static void extractGems(Project project,Configuration jRubyClasspath, Configuration gemConfig,File destDir,GemUtils.OverwriteAction action ) {
        getGems(project.files(gemConfig.files)).each { File f ->
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
