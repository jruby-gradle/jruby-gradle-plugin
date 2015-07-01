package com.github.jrubygradle

import com.github.jrubygradle.internal.JRubyExecUtils
import com.github.jrubygradle.internal.GemVersionResolver
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

        Map jrubyVersion = JRubyExecUtils.jrubyJarVersionTriple(jRubyClasspath)

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
                // there are a few extra args which look like defaults
                // but we need to make sure any config in $HOME/.gemrc
                // is overwritten
                args '--ignore-dependencies',
                     "--install-dir=${destDir.absolutePath}",
                     '--no-user-install',
                     '--wrappers',
                     '--no-document',
                     '--local'

                // Workaround for bug
                if(JRubyExecUtils.jrubyJarVersion(jRubyClasspath) == '1.7.14') {
                    project.logger.debug "Gem installation: Working around bug in jruby 1.7.14"
                    environment HOME : project.gradle.gradleUserHomeDir.absolutePath
                }

                // Workaround for FFI bug that is seen on some Windows environments
                if(System.getProperty('os.name').toLowerCase().startsWith('windows')) {
                  environment 'TMP' : System.env.TMP, 'TEMP' : System.env.TEMP
                }

                systemProperties 'file.encoding' : 'utf-8'
            }

            if(jrubyVersion.major == 1 && jrubyVersion.minor <= 7 && jrubyVersion.patchlevel < 19) {
                project.logger.warn "Not generating JRuby directory info information as current JRuby version < 1.7.19"
            } else {
                project.javaexec {
                    main 'org.jruby.Main'
                    classpath jRubyClasspath
                    args '-r', 'jruby/commands', '-e', "JRuby::Commands.generate_dir_info( '${destDir.absolutePath}' )"

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

    static void writeJarsLock(File jarsLock, List<String> coordinates,
                              GemUtils.OverwriteAction overwrite) {
        switch(overwrite) {
            case OverwriteAction.FAIL:
                if (jarsLock.exists()) {
                    throw new DuplicateFileCopyingException("${jarsLock.name} already exists")
                }
            case OverwriteAction.SKIP:
                if (jarsLock.exists()) {
                    break
                }
            case OverwriteAction.OVERWRITE:
                jarsLock.parentFile.mkdirs()
                jarsLock.withWriter { writer ->
                    coordinates.each { writer.println it }
                }
        }
    }

    static void rewriteJarDependencies(File jarsDir, List<File> dependencies,
                                       Map<String, String> renameMap,
                                       GemUtils.OverwriteAction overwrite) {
        dependencies.each { File dependency ->
            if (dependency.name.toLowerCase().endsWith('.jar') && !dependency.name.startsWith('jruby-complete-')) {
                File destination = new File (jarsDir, renameMap[dependency.name])
                switch(overwrite) {
                    case OverwriteAction.FAIL:
                        if (destination.exists()) {
                            throw new DuplicateFileCopyingException("Jar ${destination.name} already exists")
                        }
                    case OverwriteAction.SKIP:
                        if (destination.exists()) {
                            break
                        }
                    case OverwriteAction.OVERWRITE:
                        destination.delete()
                        destination.parentFile.mkdirs()
                        dependency.withInputStream { destination << it }
                }
            }
        }
    }

    static void setupJars(Configuration config,
                          File destDir,
                          GemUtils.OverwriteAction overwrite) {
        def artifacts = config.resolvedConfiguration.resolvedArtifacts
        Map<String,String> fileRenameMap = [:]
        List<String> coordinates = []
        List<File> files = []
        artifacts.each { dep ->

            def group = dep.moduleVersion.id.group
            def groupAsPath = group.replace('.' as char, File.separatorChar)
            def version = dep.moduleVersion.id.version
            // TODO classifier
            String newFileName = "${groupAsPath}/${dep.name}/${version}/${dep.name}-${version}.${dep.type}"

            // we do not want jruby-complete.jar or gems
            if (group != 'rubygems' && dep.type != 'gem' && dep.name != 'jruby-complete') {
                // TODO classifier and system-scope
                coordinates << "${group}:${dep.name}:${version}:runtime:"
            }
            fileRenameMap[dep.file.name] = newFileName
            // TODO omit system-scoped files
            files << dep.file
        }

        // create Jars.lock file used by jar-dependencies
        writeJarsLock(new File(destDir, 'Jars.lock'), coordinates, overwrite)

        rewriteJarDependencies(new File(destDir, 'jars'), files, fileRenameMap, overwrite)
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
                // TODO have some standard which is bin/*, gems/**
                // specifications/*
                if(!fullGem) {
                    exclude 'cache/**'
                    exclude 'gems/*/test/**'
                    exclude 'gems/*/tests/**'
                    exclude 'gems/*/spec/**'
                    exclude 'gems/*/specs/**'
                    exclude 'build_info'
                }
            }

            if(properties.containsKey('subfolder')) {
                into properties['subfolder']
            }
        }
    }

    static CopySpec jarCopySpec(Project project,Object dir) {
	    project.copySpec {
            from(dir) {
                include '**'
            }
        }
    }
}
