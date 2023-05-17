/*
 * Copyright (c) 2014-2023, R. Tyler Croy <rtyler@brokenco.de>,
 *     Schalk Cronje <ysb33r@gmail.com>, Christian Meier, Lookout, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.jrubygradle.jar.internal

import com.github.jengelman.gradle.plugins.shadow.impl.RelocatorRemapper
import com.github.jengelman.gradle.plugins.shadow.internal.UnusedTracker

/*
 * This source code is derived from Apache 2.0 licensed software copyright John
 * Engelman (https://github.com/johnrengelman) and was originally ported from this
 * repository: https://github.com/johnrengelman/shadow
*/

import com.github.jengelman.gradle.plugins.shadow.internal.ZipCompressor
import com.github.jengelman.gradle.plugins.shadow.relocation.Relocator
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import groovy.util.logging.Slf4j
import org.apache.commons.io.FilenameUtils
import org.apache.commons.io.IOUtils
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.UncheckedIOException
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.DefaultFileTreeElement
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.tasks.util.PatternSet
import org.gradle.internal.UncheckedException
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.apache.tools.zip.UnixStat
import org.apache.tools.zip.Zip64RequiredException
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipFile
import org.apache.tools.zip.ZipOutputStream

import java.util.zip.ZipException

/**
 * JRubyJarCopyAction is an implementation of the {@link CopyAction} interface for mutating the JRubyJar archive.
 *
 * This class, in its current form is really just a big copy and paste of the
 * shadow plugin's <a
 * href="https://github.com/johnrengelman/shadow/blob/4.0.4/src/main/groovy/com/github/jengelman/gradle/plugins/shadow/tasks/ShadowCopyAction.groovy">ShadowCopyAction</a>
 * with one notable exception, it disables the behavior of unzipping nested
 * archives when creating the resulting archive.
 *
 * This class is only intended to be used with the {@link
 * JRubyDirInfoTransformer} until such a time when this can be refactored to
 * support the same behavior in a less hackish way.
 */
@Slf4j
@SuppressWarnings(['ParameterCount', 'CatchException', 'DuplicateStringLiteral',
    'CatchThrowable', 'VariableName', 'UnnecessaryGString', 'InvertedIfElse'])
class JRubyJarCopyAction implements CopyAction {
    static final long CONSTANT_TIME_FOR_ZIP_ENTRIES = (new GregorianCalendar(1980, 1, 1, 0, 0, 0)).timeInMillis

    private final Provider<File> zipFile
    private final ZipCompressor compressor
    private final DocumentationRegistry documentationRegistry
    private final List<Transformer> transformers
    private final List<Relocator> relocators
    private final PatternSet patternSet
    private final String encoding
    private final boolean preserveFileTimestamps
    private final boolean minimizeJar
    private final UnusedTracker unusedTracker

    JRubyJarCopyAction(Provider<File> zipFile, ZipCompressor compressor, DocumentationRegistry documentationRegistry,
                       String encoding, List<Transformer> transformers, List<Relocator> relocators,
                       PatternSet patternSet,
                       boolean preserveFileTimestamps, boolean minimizeJar, UnusedTracker unusedTracker) {

        this.zipFile = zipFile
        this.compressor = compressor
        this.documentationRegistry = documentationRegistry
        this.transformers = transformers
        this.relocators = relocators
        this.patternSet = patternSet
        this.encoding = encoding
        this.preserveFileTimestamps = preserveFileTimestamps
        this.minimizeJar = minimizeJar
        this.unusedTracker = unusedTracker
    }

    @Override
    WorkResult execute(CopyActionProcessingStream stream) {
        Set<String> unusedClasses
        if (minimizeJar) {
            stream.process(new BaseStreamAction() {
                @Override
                void visitFile(FileCopyDetails fileDetails) {
                    // All project sources are already present, we just need
                    // to deal with JAR dependencies.
                    if (isArchive(fileDetails)) {
                        unusedTracker.addDependency(fileDetails.file)
                    }
                }
            })
            unusedClasses = unusedTracker.findUnused()
        } else {
            unusedClasses = Collections.emptySet()
        }

        File zipFileResolved = zipFile.get()
        try {
            final ZipOutputStream zipOutStr = compressor.createArchiveOutputStream(zipFileResolved)
            withResource(zipOutStr, new Action<ZipOutputStream>() {
                void execute(ZipOutputStream outputStream) {
                    try {
                        stream.process(new StreamAction(outputStream, encoding, transformers, relocators, patternSet,
                            unusedClasses))
                        processTransformers(outputStream)
                    } catch (Exception e) {
                        log.error('ex', e)
                        //TODO this should not be rethrown
                        throw e
                    }
                }
            })
        } catch (UncheckedIOException e) {
            if (e.cause instanceof Zip64RequiredException) {
                throw new Zip64RequiredException(
                    String.format("%s\n\nTo build this archive, please enable the zip64 extension.\nSee: %s",
                        e.cause.message, documentationRegistry.getDslRefForProperty(Zip, "zip64"))
                )
            }
        } catch (Exception e) {
            throw new GradleException("Could not create ZIP '${zipFileResolved}'", e)
        }
        return WorkResults.didWork(true)
    }

    private void processTransformers(ZipOutputStream stream) {
        transformers.each { Transformer transformer ->
            if (transformer.hasTransformedResource()) {
                transformer.modifyOutputStream(stream, preserveFileTimestamps)
            }
        }
    }

    private long getArchiveTimeFor(long timestamp) {
        return preserveFileTimestamps ? timestamp : CONSTANT_TIME_FOR_ZIP_ENTRIES
    }

    private ZipEntry setArchiveTimes(ZipEntry zipEntry) {
        if (!preserveFileTimestamps) {
            zipEntry.setTime(CONSTANT_TIME_FOR_ZIP_ENTRIES)
        }
        return zipEntry
    }

    private static <T extends Closeable> void withResource(T resource, Action<? super T> action) {
        try {
            action.execute(resource)
        } catch (Throwable t) {
            try {
                resource.close()
            } catch (IOException e) {
                log.debug("Dropping ignored exception ${e}")
            }
            throw UncheckedException.throwAsUncheckedException(t)
        }

        try {
            resource.close()
        } catch (IOException e) {
            throw new UncheckedIOException(e)
        }
    }

    abstract class BaseStreamAction implements CopyActionProcessingStreamAction {
        protected boolean isArchive(FileCopyDetails fileDetails) {
            return fileDetails.relativePath.pathString.endsWith('.jar')
        }

        protected boolean isClass(FileCopyDetails fileDetails) {
            return FilenameUtils.getExtension(fileDetails.path) == 'class'
        }

        @Override
        void processFile(FileCopyDetailsInternal details) {
            if (details.directory) {
                visitDir(details)
            } else {
                visitFile(details)
            }
        }

        @SuppressWarnings(['UnusedMethodParameter', 'EmptyMethodInAbstractClass'])
        protected void visitDir(FileCopyDetails dirDetails) {
        }

        protected abstract void visitFile(FileCopyDetails fileDetails)
    }

    private class StreamAction extends BaseStreamAction {

        private final ZipOutputStream zipOutStr
        private final List<Transformer> transformers
        private final List<Relocator> relocators
        private final RelocatorRemapper remapper
        private final PatternSet patternSet
        private final Set<String> unused

        private final Set<String> visitedFiles = [] as Set

        StreamAction(ZipOutputStream zipOutStr, String encoding, List<Transformer> transformers,
                     List<Relocator> relocators, PatternSet patternSet, Set<String> unused) {
            this.zipOutStr = zipOutStr
            this.transformers = transformers
            this.relocators = relocators
            this.remapper = new RelocatorRemapper(relocators, null)
            this.patternSet = patternSet
            this.unused = unused
            if (encoding != null) {
                this.zipOutStr.setEncoding(encoding)
            }
        }

        private boolean recordVisit(RelativePath path) {
            return visitedFiles.add(path.pathString)
        }

        @Override
        void visitFile(FileCopyDetails fileDetails) {
            try {
                boolean isClass = isClass(fileDetails)
                if (!remapper.hasRelocators() || !isClass) {
                    if (!isTransformable(fileDetails)) {
                        String mappedPath = remapper.map(fileDetails.relativePath.pathString)
                        ZipEntry archiveEntry = new ZipEntry(mappedPath)
                        archiveEntry.setTime(getArchiveTimeFor(fileDetails.lastModified))
                        archiveEntry.unixMode = (UnixStat.FILE_FLAG | fileDetails.mode)
                        zipOutStr.putNextEntry(archiveEntry)
                        fileDetails.copyTo(zipOutStr)
                        zipOutStr.closeEntry()
                    } else {
                        transform(fileDetails)
                    }
                } else if (isClass && !isUnused(fileDetails.path)) {
                    remapClass(fileDetails)
                }
                recordVisit(fileDetails.relativePath)
            } catch (Exception e) {
                throw new GradleException(String.format("Could not add %s to ZIP '%s'.", fileDetails, zipFile.get()), e)
            }
        }

        private boolean isUnused(String classPath) {
            final String className = FilenameUtils.removeExtension(classPath)
                .replace('/' as char, '.' as char)
            final boolean result = unused.contains(className)
            if (result) {
                log.debug("Dropping unused class: $className")
            }
            return result
        }

        private void remapClass(RelativeArchivePath file, ZipFile archive) {
            if (file.classFile) {
                ZipEntry zipEntry = setArchiveTimes(new ZipEntry(remapper.mapPath(file) + '.class'))
                addParentDirectories(new RelativeArchivePath(zipEntry))
                InputStream is = archive.getInputStream(file.entry)
                try {
                    remapClass(is, file.pathString, file.entry.time)
                } finally {
                    is.close()
                }
            }
        }

        private void remapClass(FileCopyDetails fileCopyDetails) {
            if (FilenameUtils.getExtension(fileCopyDetails.name) == 'class') {
                remapClass(fileCopyDetails.file.newInputStream(), fileCopyDetails.path, fileCopyDetails.lastModified)
            }
        }

        private void remapClass(InputStream classInputStream, String path, long lastModified) {
            InputStream is = classInputStream
            ClassReader cr = new ClassReader(is)

            // We don't pass the ClassReader here. This forces the ClassWriter to rebuild the constant pool.
            // Copying the original constant pool should be avoided because it would keep references
            // to the original class names. This is not a problem at runtime (because these entries in the
            // constant pool are never used), but confuses some tools such as Felix' maven-bundle-plugin
            // that use the constant pool to determine the dependencies of a class.
            ClassWriter cw = new ClassWriter(0)

            ClassVisitor cv = new ClassRemapper(cw, remapper)

            try {
                cr.accept(cv, ClassReader.EXPAND_FRAMES)
            } catch (Throwable ise) {
                throw new GradleException("Error in ASM processing class " + path, ise)
            }

            byte[] renamedClass = cw.toByteArray()

            // Need to take the .class off for remapping evaluation
            String mappedName = remapper.mapPath(path)

            InputStream bis = new ByteArrayInputStream(renamedClass)
            try {
                // Now we put it back on so the class file is written out with the right extension.
                ZipEntry archiveEntry = new ZipEntry(mappedName + ".class")
                archiveEntry.setTime(getArchiveTimeFor(lastModified))
                zipOutStr.putNextEntry(archiveEntry)
                IOUtils.copyLarge(bis, zipOutStr)
                zipOutStr.closeEntry()
            } catch (ZipException e) {
                log.warn("We have a duplicate " + mappedName + " in source project")
            } finally {
                bis.close()
            }
        }

        @Override
        protected void visitDir(FileCopyDetails dirDetails) {
            try {
                // Trailing slash in name indicates that entry is a directory
                String path = dirDetails.relativePath.pathString + '/'
                ZipEntry archiveEntry = new ZipEntry(path)
                archiveEntry.setTime(getArchiveTimeFor(dirDetails.lastModified))
                archiveEntry.unixMode = (UnixStat.DIR_FLAG | dirDetails.mode)
                zipOutStr.putNextEntry(archiveEntry)
                zipOutStr.closeEntry()
                recordVisit(dirDetails.relativePath)
            } catch (Exception e) {
                throw new GradleException(String.format("Could not add %s to ZIP '%s'.", dirDetails, zipFile.get()), e)
            }
        }

        private void transform(ArchiveFileTreeElement element, ZipFile archive) {
            transformAndClose(element, archive.getInputStream(element.relativePath.entry))
        }

        private void transform(FileCopyDetails details) {
            transformAndClose(details, details.file.newInputStream())
        }

        private void transformAndClose(FileTreeElement element, InputStream is) {
            try {
                String mappedPath = remapper.map(element.relativePath.pathString)
                transformers.find { it.canTransformResource(element) }.transform(
                    TransformerContext.builder()
                        .path(mappedPath)
                        .is(is)
                        .relocators(relocators)
                        .build()
                )
            } finally {
                is.close()
            }
        }

        private boolean isTransformable(FileTreeElement element) {
            return transformers.any { it.canTransformResource(element) }
        }

    }

    class RelativeArchivePath extends RelativePath {

        ZipEntry entry

        RelativeArchivePath(ZipEntry entry) {
            super(!entry.directory, entry.name.split('/'))
            this.entry = entry
        }

        boolean isClassFile() {
            return lastName.endsWith('.class')
        }

        RelativeArchivePath getParent() {
            if (!segments || segments.length == 1) {
                return null
            }
            //Parent is always a directory so add / to the end of the path
            String path = segments[0..-2].join('/') + '/'
            return new RelativeArchivePath(setArchiveTimes(new ZipEntry(path)))
        }
    }

    class ArchiveFileTreeElement implements FileTreeElement {

        private final RelativeArchivePath archivePath

        ArchiveFileTreeElement(RelativeArchivePath archivePath) {
            this.archivePath = archivePath
        }

        boolean isClassFile() {
            return archivePath.classFile
        }

        @SuppressWarnings(['GetterMethodCouldBeProperty'])
        @Override
        File getFile() {
            return null
        }

        @Override
        boolean isDirectory() {
            return archivePath.entry.directory
        }

        @Override
        long getLastModified() {
            return archivePath.entry.lastModifiedDate.time
        }

        @Override
        long getSize() {
            return archivePath.entry.size
        }

        @Override
        InputStream open() {
            return null
        }

        @Override
        void copyTo(OutputStream outputStream) {

        }

        @Override
        boolean copyTo(File file) {
            return false
        }

        @Override
        String getName() {
            return archivePath.pathString
        }

        @Override
        String getPath() {
            return archivePath.lastName
        }

        @Override
        RelativeArchivePath getRelativePath() {
            return archivePath
        }

        @Override
        int getMode() {
            return archivePath.entry.unixMode
        }

        FileTreeElement asFileTreeElement() {
            return new DefaultFileTreeElement(null, new RelativePath(!isDirectory(), archivePath.segments), null, null)
        }
    }
}
