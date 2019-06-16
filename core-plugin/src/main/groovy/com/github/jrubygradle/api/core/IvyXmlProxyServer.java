/**
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
package com.github.jrubygradle.api.core;

import java.net.URI;
import java.nio.file.Path;

/**
 * Proxy service which can translate RubyGems structures into Ivy structures
 * and vice-versa.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
public interface IvyXmlProxyServer extends Runnable {

    /** Local bind address of the proxy server.
     *
     * @return URI of service.
     */
    URI getBindAddress();

    /** Location of cached {@code ivy.xml} file.
     *
     * @param group Group associated with GEMs. This is the group that will be used inside Gradle
     *              as a Maven group or an Ivy organisation. As Rubygems does not have this concept
     *              it is purely for usage inside Gradle.
     * @param name Name of GEM.
     * @param revision Verison of GEM.
     * @return Location of cached file (even if file does not exist yet).
     */
    Path ivyFile(String group, String name, String revision);

    /** Set proxy service to refresh dependencies on a subsequent run.
     *
     * @param refresh {@code true} if service should refresh dependencies.
     */
    void setRefreshDependencies(boolean refresh);
}
