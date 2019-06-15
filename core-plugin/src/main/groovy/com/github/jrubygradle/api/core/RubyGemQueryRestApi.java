package com.github.jrubygradle.api.core;

import com.github.jrubygradle.api.gems.GemInfo;

import java.util.List;

/** Interface for querying a service that confirorms to the RubyGem API.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0.
 *
 * @see https://guides.rubygems.org/rubygems-org-api
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
     * Return latest published version of GEM.
     *
     * @param gemName Name of GEM.
     * @return Version of GEM
     * @throws {@link ApiException} if GEM does not exist.
     */
    String latestVersion(String gemName) throws ApiException;

    /** Returns the basic metadata for a GEM.
     *
     * @param gemName Name of GEM.
     * @param version Version of GEM.
     * @return Metadata for GEM
     * @throws {@link ApiException} if GEM + version does not exist.
     */
    GemInfo metadata(String gemName, String version) throws ApiException;
}
