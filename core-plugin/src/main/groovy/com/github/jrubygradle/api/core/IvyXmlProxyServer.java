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
