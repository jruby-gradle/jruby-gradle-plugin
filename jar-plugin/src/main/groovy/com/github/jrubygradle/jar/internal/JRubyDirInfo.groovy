/*
 * Copyright (c) 2014-2019, R. Tyler Croy <rtyler@brokenco.de>,
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

import org.gradle.api.file.RelativePath

/**
 * JRubyDirInfo is responsible for generation of .jrubydir files.
 *
 * The .jrubydir files help the JRuby runtime navigate using {@code Dir.glob}
 * type patterns from within the packed JRubyJar context
 */
class JRubyDirInfo {
    private static final String NEW_LINE = System.getProperty('line.separator')
    private static final File OMIT = new File('')
    private static final List<String> OMISSION_DIRS = ['META-INF', 'bin', 'jars']

    private final Map dirsCache = [:]

    private final File dirInfo

    JRubyDirInfo(File dir) {
        dirInfo = dir
    }

    File toFile(File path, String subdir) {
        if (path == null) {
            if (OMISSION_DIRS.contains(subdir)) {
                return OMIT
            }
            return new File(subdir)
        }

        if (path != OMIT) {
            return  new File(path, subdir)
        }
        return path
    }

    void process(File path) {
        boolean isRoot = path.parent == null
        File file = new File(dirInfo,
                             isRoot ? '.jrubydir' : "${path.parent}/.jrubydir")
        String name = path.name
        File dir = file.parentFile
        List dirs = dirsCache[dir]

        if (dirs == null) {
            dirs = dirsCache[dir] = []
        }
        if (!dirs.contains(name)) {
            dirs << name
            dir.mkdirs()
            if (file.exists()) {
                file.append(name + NEW_LINE)
            }
            else {
                StringBuilder buf = new StringBuilder()
                buf.append('.').append(NEW_LINE)
                if (!isRoot) {
                    buf.append('..').append(NEW_LINE)
                }
                buf.append(name).append(NEW_LINE)
                file.write(buf.toString())
            }
        }
    }

    void add( RelativePath relativePath ) {
        if (!['jar-bootstrap.rb', 'MANIFEST.MF', '.jrubydir'].contains(relativePath.lastName)) {
            File path = null
            relativePath.segments.each {
                path = toFile(path, it.toString())
                if (path != OMIT) {
                    process(path)
                }
            }
        }
    }
}
