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
package com.github.jrubygradle.testhelper

import java.util.regex.Pattern

/**
 * @author Schalk W. Cronjé.
 */
class VersionFinder {
    @SuppressWarnings(['NoDef'])
    static String find(final File repoDir, final String artifact, final String extension) {
        Pattern pat = ~/^${artifact}-(.+)\.${extension}/
        def files = repoDir.list([ accept: { File dir, String name ->
            name ==~ pat
        } ] as FilenameFilter)

        if (files.size()) {
            def matcher = files[0] =~ pat
            matcher[0][1]
        } else {
            null
        }
    }

    static String findDependency(final File repoDir, final String organisation, final String artifact, final String extension) {
        "${organisation}:${artifact}:${find(repoDir, artifact, extension)}@${extension}"
    }
}
