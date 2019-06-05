package com.github.jrubygradle.api.gems;

/** Description of a transitive GEM dependency.
 *
 * @since 2.0
 */
public interface GemDependency {

    /** Name of transitive GEM dependency.
     *
     * @return GEM name
     */
    String getName();

    /** Version requirements that is requested upon this transitive dependency.
     *
      * @return Version requirements in GEM format.
     */
    String getRequirements();
}
