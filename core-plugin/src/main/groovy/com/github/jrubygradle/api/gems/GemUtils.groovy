package com.github.jrubygradle.api.gems

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicateFileCopyingException
import org.gradle.api.file.FileCollection
import org.gradle.process.JavaExecSpec
import org.ysb33r.grolifant.api.OperatingSystem

import static com.github.jrubygradle.api.gems.GemOverwriteAction.SKIP
import static com.github.jrubygradle.api.gems.GemOverwriteAction.FAIL
import static com.github.jrubygradle.api.gems.GemOverwriteAction.OVERWRITE

/** A collection of utilities to manipulate GEMs.
 *
 * @author R Tyler Croy
 * @author Schalk W. Cronjé
 */
@CompileStatic
class GemUtils {
    public static final String JRUBY_MAINCLASS = 'org.jruby.Main'
    public static final String JRUBY_ARCHIVE_NAME = 'jruby-complete'

    /** Given a FileCollection return a filtered FileCollection only containing GEMs
     *
     * @param fc Original FileCollection
     * @return Filtered FileCollection
     */
    static FileCollection getGems(FileCollection fc) {
        fc.filter { File f ->
            f.name.toLowerCase().endsWith(GEM_EXTENSION)
        }
    }

    /** Extracts a gem to a folder
     *
     * @param project Project instance.
     * @param jRubyClasspath The path to the {@code jruby-complete} jar.
     * @param gem Gem file to extract.
     * @param destDir Directory to extract to.
     * @param overwrite Allow overwrite of an existing gem folder.
     */
    static void extractGem(
        Project project,
        File jRubyClasspath,
        File gem,
        File destDir,
        GemOverwriteAction overwrite
    ) {

        extractGems(project, jRubyClasspath, project.files(gem), destDir, overwrite)
    }

    /** Extracts and install a collection of GEMs.
     *
     * @param project Project instance.
     * @param jRubyClasspath The path to the {@code jruby-complete} jar.
     * @param gems GEMs to install.
     * @param destDir Directory to extract to.
     * @param overwrite Allow overwrite of an existing gem folder.
     */
    static void extractGems(
        Project project,
        File jRubyClasspath,
        FileCollection gems,
        File destDir,
        GemOverwriteAction overwrite
    ) {
        Set<File> gemsToProcess = []
        Set<File> deletes = []

        getGems(gems).files.each { File gem ->
            String gemName = gemFullNameFromFile(gem.name)
            File extractDir = new File(destDir, "gems/${gemName}")
            // We want to check for -java specific gem installations too, e.g.
            // thread_safe-0.3.4-java
            File extractDirForJava = new File(destDir, "gems/${gemName}-java")

            switch (overwrite) {
                case SKIP:
                    if (extractDir.exists() || extractDirForJava.exists()) {
                        return
                    }
                case OVERWRITE:
                    deletes.add(extractDir)
                    deletes.add(extractDirForJava)
                    break
                case FAIL:
                    if (extractDir.exists() || extractDirForJava.exists()) {
                        throw new DuplicateFileCopyingException("Gem ${gem.name} already exists")
                    }
            }

            gemsToProcess.add(gem)
        }

        if (gemsToProcess.size()) {
            deletes.each { project.delete it }
            destDir.mkdirs()

            project.logger.info("Installing ${gemsToProcess*.name.join(',')}")

            project.javaexec { JavaExecSpec spec ->
                spec.with {
                    // Setting these environment variables will ensure that
                    // jbundler and/or jar-dependencies will not attempt to invoke
                    // Maven on a gem's behalf to install a Java dependency that we
                    // should already have taken care of, see #79
                    environment JBUNDLE_SKIP: true,
                        JARS_SKIP: true,
                        GEM_HOME: destDir.absolutePath,
                        GEM_PATH: destDir.absolutePath
                    main = JRUBY_MAINCLASS
                    classpath jRubyClasspath
                    args '-S', GEM, 'install'

                    /*
                 * NOTE: gemsToProcess is assumed to typically be sourced from
                 * a FileCollection generated elsewhere in the code. The
                 * FileCollection a flattened version of the dependency tree.
                 *
                 * In order to handle Rubygems which depend on their
                 * dependencies at _installation time_, we need to reverse the
                 * order to make sure that the .gem files for the
                 * transitive/nested dependencies are installed first
                 *
                 * See:
                 * https://gikhub.com/jruby-gradle/jruby-gradle-plugin/issues/341
                 */
                    gemsToProcess.collect { it }.reverse().each { File gem ->
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

                    // Workaround for FFI bug that is seen on some Windows environments
                    if (OperatingSystem.current().windows) {
                        environment 'TMP': System.getenv('TMP'), 'TEMP': System.getenv('TEMP')
                    }

                    systemProperties 'file.encoding': 'utf-8'
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
        GemOverwriteAction action) {

        Set<File> cp = jRubyConfig.files
        File jRubyClasspath = cp.find { it.name.startsWith(JRUBY_ARCHIVE_NAME) }
        if (jRubyClasspath == null) {
            throw new GemInstallException(
                "Cannot find ${JRUBY_ARCHIVE_NAME}. Classpath contains ${cp.join(':')}"
            )
        }
        extractGems(project, jRubyClasspath, project.files(gemConfig.files), destDir, action)
    }

    /** Write a JARs lock file if the content has changed.
     *
     * @param jarsLock Loc file to write to.
     * @param coordinates Coordinates.
     */
    static void writeJarsLock(File jarsLock, List<String> coordinates) {
        String content

        if (jarsLock.exists()) {
            content = jarsLock.text
        } else {
            jarsLock.parentFile.mkdirs()
            content = ''
        }
        StringWriter newContent = new StringWriter()
        coordinates.each { newContent.println it }
        if (content != newContent.toString()) {
            jarsLock.text = newContent
        }
        newContent.close()
    }

    /** Rewrite the JAR dependencies
     *
     * @param jarsDir
     * @param dependencies
     * @param renameMap
     * @param overwrite
     */
    static void rewriteJarDependencies(
        File jarsDir,
        List<File> dependencies,
        Map<String, String> renameMap,
        GemOverwriteAction overwrite
    ) {
        dependencies.each { File dependency ->
            if (dependency.name.toLowerCase().endsWith('.jar') && !dependency.name.startsWith(JRUBY_ARCHIVE_NAME)) {
                File destination = new File(jarsDir, renameMap[dependency.name])
                switch (overwrite) {
                    case FAIL:
                        if (destination.exists()) {
                            throw new DuplicateFileCopyingException("Jar ${destination.name} already exists")
                        }
                    case GemOverwriteAction.SKIP:
                        if (destination.exists()) {
                            break
                        }
                    case OVERWRITE:
                        destination.delete()
                        destination.parentFile.mkdirs()
                        dependency.withInputStream { destination << it }
                }
            }
        }
    }

    /**
     *
     * @param config
     * @param destDir
     * @param overwrite
     */
    static void setupJars(
        Configuration config,
        File destDir,
        GemOverwriteAction overwrite
    ) {
        Set<ResolvedArtifact> artifacts = config.resolvedConfiguration.resolvedArtifacts
        Map<String, String> fileRenameMap = [:]
        List<String> coordinates = []
        List<File> files = []
        artifacts.each { ResolvedArtifact dependency ->
            String group = dependency.moduleVersion.id.group
            String groupAsPath = group.replace('.' as char, File.separatorChar)
            String version = dependency.moduleVersion.id.version
            // TODO classifier
            String newFileName = "${groupAsPath}/${dependency.name}/${version}/${dependency.name}-${version}.${dependency.type}"

            // we do not want jruby-complete.jar or gems
            if (dependency.type != GEM && dependency.name != JRUBY_ARCHIVE_NAME) {
                // TODO classifier and system-scope
                coordinates.add("${group}:${dependency.name}:${version}:runtime:".toString())
            }
            fileRenameMap[dependency.file.name] = newFileName
            // TODO omit system-scoped files
            files << dependency.file
        }

        // create Jars.lock file used by jar-dependencies
        writeJarsLock(new File(destDir, 'Jars.lock'), coordinates)

        rewriteJarDependencies(new File(destDir, 'jars'),
            files,
            fileRenameMap,
            overwrite)
    }

    /** Take the given .gem filename (e.g. rake-10.3.2.gem) and just return the
     * gem "full name" (e.g. rake-10.3.2)
     *
     * @param filename GEM filename.
     * @return GEM name + version.
     */
    static String gemFullNameFromFile(String filename) {
        return filename.replaceAll(~GEM_EXTENSION, '')
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
    @CompileDynamic
    static CopySpec gemCopySpec(Map properties = [:], Project project, Object dir) {
        boolean fullGem = properties['fullGem']
        String subFolder = properties['subfolder']

        project.copySpec(new Action<CopySpec>() {
            void execute(CopySpec spec) {
                spec.with {
                    from(dir) {
                        include EVERYTHING
                        // TODO have some standard which is bin/*, gems/**
                        // specifications/*
                        if (!fullGem) {
                            exclude 'cache/**'
                            exclude 'gems/*/test/**'
                            exclude 'gems/*/tests/**'
                            exclude 'gems/*/spec/**'
                            exclude 'gems/*/specs/**'
                            exclude 'build_info'
                        }
                    }
                    if (subFolder) {
                        into subFolder
                    }
                }
            }
        })
    }

    static CopySpec jarCopySpec(Project project, Object dir) {
        project.copySpec { CopySpec spec ->
            spec.with {
                from(dir) { include EVERYTHING }
            }
        }
    }

    private static final String GEM = 'gem'
    private static final String GEM_EXTENSION = '.gem'
    private static final String EVERYTHING = '**'
}
