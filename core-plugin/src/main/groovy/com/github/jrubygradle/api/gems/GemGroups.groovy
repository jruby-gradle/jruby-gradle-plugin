package com.github.jrubygradle.api.gems

import com.github.jrubygradle.api.core.RepositoryHandlerExtension
import groovy.transform.CompileStatic

import static com.github.jrubygradle.api.core.RepositoryHandlerExtension.DEFAULT_GROUP_NAME

/** Defines groups which contains GEMs
 *
 * @author Schalk W. Cronjé
 *
 * @since 2.0
 */
@CompileStatic
class GemGroups {

    public static final String NAME = 'gemGroups'

    /** Is this group/organisation a GEM group ?
     *
     * @param groupName Name of group/organisation.
     * @return {@code true} is group is a GEM group.
     */
    boolean isGemGroup(final String groupName) {
        groups.contains(groupName)
    }

    /** Add a new group for GEMs.
     *
     * @param groupName Name of group to add.
     */
    void addGemGroup(final String groupName) {
        groups.add(groupName)
    }

    private final Set<String> groups = [RepositoryHandlerExtension.DEFAULT_GROUP_NAME].toSet()
}
