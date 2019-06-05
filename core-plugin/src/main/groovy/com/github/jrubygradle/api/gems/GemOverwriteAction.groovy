package com.github.jrubygradle.api.gems

import groovy.transform.CompileStatic

/** Overwrite actions when installing GEMs locally into the build area
 *
 * @author Schalk W. Cronjé
 * @author R Tyler Croy
 * @author Christian Meier
 *
 * @since 2.0
 */
@CompileStatic
enum GemOverwriteAction {
    /** Fail if GEM exists.
     *
     */
    FAIL,

    /** Skip GEM installation if GEM exists.
     *
     */
    SKIP,

    /** Overwrite any existing installation.
     *
     */
    OVERWRITE
}