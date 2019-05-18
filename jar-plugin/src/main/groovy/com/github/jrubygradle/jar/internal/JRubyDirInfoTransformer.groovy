package com.github.jrubygradle.jar.internal

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import shadow.org.apache.tools.zip.ZipEntry
import org.codehaus.plexus.util.IOUtil
import org.gradle.api.file.FileTreeElement

import shadow.org.apache.tools.zip.ZipOutputStream
import com.github.jengelman.gradle.plugins.shadow.transformers.Transformer
import com.github.jengelman.gradle.plugins.shadow.transformers.TransformerContext

/**
 * JRubyDirInfoTransformer implements a {@link Transformer} interface.
 *
 * This is internal primarily because it rewrequires an interaction with the
 * transformer interface inside of the shadow plugin, which will hopefully go
 * away at some point in the future
 */
@SuppressWarnings('UnusedMethodParameter')
class JRubyDirInfoTransformer implements Transformer {
    protected File tmpDir
    protected JRubyDirInfo info

    JRubyDirInfoTransformer() {
        tmpDir = Files.createTempDirectory('jrubydirinfo').toFile()
        info = new JRubyDirInfo(tmpDir)
    }

    /**
     * Register the relative path of the {@code element} that will be jarred.
     *
     * Since this transformer is just performing book-keeping, it returns false
     * to avoid telling the machinery in shadow to transform the actual file
     * being visited and jarred up
     *
     * @return false
     */
    boolean canTransformResource(FileTreeElement element) {
        info.add(element.relativePath)
        return false
    }

    /** No-op since we don't transform the actual file */
    void transform(TransformerContext context) {
        return
    }

    /**
     * Confirm that we've done some work so our {@code modifyOutputStream} is called
     *
     * @return true
     */
    boolean hasTransformedResource() {
        return true
    }

    /**
     * Process the output stream and add our .jrubydir entries.
     *
     * This method will also clean up our tempdir to make sure we don't
     * clutter the user's machine with junk
     */
    void modifyOutputStream(ZipOutputStream os, boolean preserveFileTimestamps) {
        processDirectory(os, tmpDir)
        deleteTempDirectory(tmpDir)
    }

    /**
     * Process the directory given and ensure that all our .jrubydir files are added.
     *
     * @param stream {@link ZipOutputStream} for our archive being built
     * @param directory Directory which contains our .jrubydir files to be
     *  copied
     */
    protected void processDirectory(ZipOutputStream stream, File directory) {
        directory.listFiles().each { File file ->
            if (file.isDirectory()) {
                processDirectory(stream, file)
            }
            else {
                Path relative = Paths.get(tmpDir.absolutePath).relativize(Paths.get(file.absolutePath))
                stream.putNextEntry(new ZipEntry(relative.toString()))
                IOUtil.copy(new FileInputStream(file), stream)
            }
        }
    }

    /** Recursively delete the given {@link File} */
    protected void deleteTempDirectory(File directory) {
        directory.listFiles().each { File file ->
            if (file.isDirectory()) {
                deleteTempDirectory(file)
            }
            file.delete()
        }
        directory.delete()
    }
}

