package com.github.jrubygradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.CopySpec
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

        extractGems(project, jRubyClasspath, project.files(gem), destDir, overwrite)
    }

    static void extractGems(Project project,
                           File jRubyClasspath,
                           FileCollection gems,
                           File destDir,
                           GemUtils.OverwriteAction overwrite) {
        Set<File> gemsToProcess = []
        Set<File> deletes = []

        getGems(gems).files.each { File gem ->
            String gemName = gemFullNameFromFile(gem.name)
            File extractDir = new File(destDir, "gems/${gemName}")
            // We want to check for -java specific gem installations too, e.g.
            // thread_safe-0.3.4-java
            File extractDirForJava = new File(destDir, "gems/${gemName}-java")

            switch (overwrite) {
                case GemUtils.OverwriteAction.SKIP:
                    if (extractDir.exists() || extractDirForJava.exists()) {
                        return
                    }
                case GemUtils.OverwriteAction.OVERWRITE:
                    deletes.add(extractDir)
                    deletes.add(extractDirForJava)
                    break
                case GemUtils.OverwriteAction.FAIL:
                    if (extractDir.exists() || extractDirForJava.exists()) {
                        throw new DuplicateFileCopyingException("Gem ${gem.name} already exists")
                    }
            }

            gemsToProcess.add(gem)
        }

        if (gemsToProcess.size()) {
            deletes.each { project.delete it }
            destDir.mkdirs()

            project.logger.info "Installing " + (gemsToProcess.collect { File it -> it.name }).join(',')

            project.javaexec {
                // Setting these environment variables will ensure that
                // jbundler and/or jar-dependencies will not attempt to invoke
                // Maven on a gem's behalf to install a Java dependency that we
                // should already have taken care of, see #79
                setEnvironment 'JBUNDLE_SKIP' : 'true', 'JARS_SKIP' : 'true'
                main 'org.jruby.Main'
                classpath jRubyClasspath
                args '-S', 'gem', 'install'
                gemsToProcess.each { File gem ->
                    args gem
                }
                args '--ignore-dependencies',
                     "--install-dir=${destDir.absolutePath}",
                     '-N',
                     '--platform=java'

                // Workaround for bug
                if(jRubyClasspath.name.contains('1.7.14')) {
                    project.logger.debug "Gem installation: Working around bug in jruby 1.7.14"
                    environment HOME : project.gradle.gradleUserHomeDir.absolutePath
                }
            }
        }
    }

    /** Extract Gems from a given configuration.
     *
     * @param project Project instance
     * @param jRubyClasspath Where to find the jruby-complete jar
     * @param gemConfig Configuration containing GEMs
     * @param destDir Directory to extract to
     * @param action Allow overwrite of an existing gem folder
     */
    static void extractGems(
            Project project,
            Configuration jRubyConfig,
            Configuration gemConfig,
            File destDir,
            GemUtils.OverwriteAction action ) {

        Set<File> cp = jRubyConfig.files
        File jRubyClasspath = cp.find { it.name.startsWith('jruby-complete-') }
        assert jRubyClasspath != null
        extractGems(project,jRubyClasspath,project.files(gemConfig.files),destDir,action)
    }

    /** Take the given .gem filename (e.g. rake-10.3.2.gem) and just return the
     * gem "full name" (e.g. rake-10.3.2)
     */
    static String gemFullNameFromFile(String filename) {
        return filename.replaceAll(~".gem", "")
    }

    /** Adds a GEM CopySpec to an archive
     *
     * The following are supported as properties:
     * <ul>
     * <li>fullGem (boolean) - Copy all of the GEM content, not just a minimal subset</li>
     * <li>subfolder (Object) - Adds an additional subfolder into the GEM
     * </ul>
     *
     * @param Additional properties to control behaviour
     * @param dir The source of the GEM files
     * @return Returns a CopySpec which can be attached as a child to another object that implements a CopySpec
     * @since 0.1.2
     */
    static CopySpec gemCopySpec(def properties=[:],Project project,Object dir) {
        boolean fullGem=false
        if(properties.containsKey('fullGem')) {
            fullGem = properties['fullGem']
        }
        project.copySpec {
            from(dir) {
                include '**'
                if(!fullGem) {
                    exclude 'cache/**'
                    exclude 'gems/*/test/**'
                    exclude 'gems/*/tests/**'
                    exclude 'build_info'
                }
            }

            if(properties.containsKey('subfolder')) {
                into properties['subfolder']
            }
        }
    }
}
