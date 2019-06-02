package com.github.jrubygradle.core

import groovy.transform.CompileStatic

/** Overwrite actions when installing GEMs locally into the build area
 *
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