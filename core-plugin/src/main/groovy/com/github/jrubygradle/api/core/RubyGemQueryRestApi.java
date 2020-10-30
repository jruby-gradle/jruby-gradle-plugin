/**
 * Copyright (c) 2014-2020, R. Tyler Croy <rtyler@brokenco.de>,
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
package com.github.jrubygradle.api.core;

import com.github.jrubygradle.api.gems.GemInfo;

import java.util.List;

/** Interface for querying a service that confirorms to the RubyGem API.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0.
 *
 * @see {@link https://guides.rubygems.org/rubygems-org-api}
 */
public interface RubyGemQueryRestApi {

    /**
     * Return all published versions for a specific GEM
     *
     * @param gemName Name of GEM.
     * @return List of versions. Can be empty if the GEM does not have any versions. Never {@code null}.
     * @throws {@link ApiException} if a networking or parser error occurs.
     */
    List<String> allVersions(String gemName) throws ApiException;

    /**
     * Return all published versions for a specific GEM
     *
     * @param gemName Name of GEM.
     * @param includePrelease Whether pre-release versions should be included.
     * @return List of versions. Can be empty if the GEM does not have any versions. Never {@code null}.
     * @throws {@link ApiException} if a networking or parser error occurs.
     */
    List<String> allVersions(String gemName, boolean includePrelease) throws ApiException;

    /**
     * Return latest published version of GEM.
     *
     * @param gemName Name of GEM.
     * @return Version of GEM
     * @throws {@link ApiException} if GEM does not exist.
     */
    String latestVersion(String gemName) throws ApiException;

    /**
     * Return latest published version of GEM.
     *
     * @param gemName Name of GEM.
     * @param allowPrerelease Whether a prereleased version can be considered a latest version.
     * @return Version of GEM
     * @throws {@link ApiException} if GEM does not exist.
     */
    String latestVersion(String gemName, boolean allowPrerelease) throws ApiException;

    /** Returns the basic metadata for a GEM.
     *
     * @param gemName Name of GEM.
     * @param version Version of GEM.
     * @return Metadata for GEM
     * @throws {@link ApiException} if GEM + version does not exist.
     */
    GemInfo metadata(String gemName, String version) throws ApiException;
}
