package com.github.jrubygradle.api.gems;

/** JAR dependency as specified by a GEM spec.
 *
 */
public interface JarDependency extends GemDependency {
    /** Group/organisation that the JAR belongs to
     *
     * @return Can be {@code null} if no organisation.
     */
    String getGroup();
}
