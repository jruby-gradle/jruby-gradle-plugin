package com.github.jrubygradle.api.gems;

import java.net.URI;
import java.util.List;

/** GEM metadata.
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
public interface GemInfo {

    /** GEM name.
     *
     * @return Name of GEM. Never {@code null}.
     */
    String getName();

    /** GEM version.
     *
     * @return Version of GEM.
     */
    String getVersion();

    /** GEM platform.
     *
     * @return Usage platform for GEM.
     */
    String getPlatform();

    /** Required version of Rubygems.
     *
     * @return Version specification.
     */
    String getRubyGemsVersion();

    /** Required version of Ruby
     *
     * @return Version specification.
     */
    String getRubyVersion();

    /** GEM authors.
     *
     * @return List of authors. Can be empty, but not {@code null}.
     */
    List<String> getAuthors();

    /** GEM short description.
     *
     * @return Summary text.
     */
    String getSummary();

    /** GEM long description.
     *
     * @return Informative text.
     */
    String getDescription();

    /** GEM hash.
     *
     * @return SHA
     */
    String getSha();

    /** Project home.
     *
     * @return URI of project
     */
    URI getProjectUri();

    /** Location to download GEM.
     *
     * @return URI to GEM.
     */
    URI getGemUri();

    /** Project website.
     *
     * @return URI to homepage.
     */
    URI getHomepageUri();

    /** Location of documentation.
     *
     * @return Documentation URI.
     */
    URI getDocumentationUri();

    /** Transitive runtime dependencies.
     *
     * @return List of dependencies. Can be empty, but never {@code null}.
     */
    List<GemDependency> getDependencies();

    /** Transitive development dependencies.
     *
     * @return List of dependencies. Can be empty, but never {@code null}.
     */
    List<GemDependency> getDevelopmentDependencies();

    /** Transitive JAR requirements.
     *
     * @return List of JAR requirements. Can be empty, but never {@code null}
     */
    List<JarDependency> getJarRequirements();

    /** Whether the GEM is still a prerelease version.
     *
     * @return {@code true} for prerelease
     */
    boolean isPrerelease();

}
