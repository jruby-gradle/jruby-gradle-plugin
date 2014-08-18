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
    static Boolean extractGem(Project p, File gem) {
        def gemname = gemFullNameFromFile(gem.getName())
        File extract_dir = new File("./vendor/gems/$gemname")

        if (extract_dir.exists()) {
            return
        }

        p.exec {
            executable "gem"
            args 'install', gem, '--install-dir=./vendor', '--no-ri', '--no-rdoc'
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
    static void extractGem(Project project, def jRubyClasspath, File gem,File destDir,boolean overwrite) {
        def gemname = gemFullNameFromFile(gem.name)
        File extract_dir = new File(destDir,gemname)

        if (extract_dir.exists()) {
            if(overwrite) {
                project.delete extract_dir
            } else {
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

    /** Take the given .gem filename (e.g. rake-10.3.2.gem) and just return the
     * gem "full name" (e.g. rake-10.3.2)
     */
    static String gemFullNameFromFile(String filename) {
        return filename.replaceAll(~".gem", "")
    }
}
