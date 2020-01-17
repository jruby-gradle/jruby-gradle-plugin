/*
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
