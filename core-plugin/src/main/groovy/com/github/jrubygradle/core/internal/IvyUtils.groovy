package com.github.jrubygradle.core.internal

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder

/** Utilities for dealing with Ivy formats.
 *
 */
@CompileStatic
class IvyUtils {

    /** Converts a list of revisions to an HTML directory listing.
     *
     * @param revisions List of GEM revisions.
     * @return HTML-based directory listing which can be use to serve up
     *  something in the way that Gradle would expect it to be.
     */
    @CompileDynamic
    static String revisionsAsHtmlDirectoryListing(List<String> revisions) {
        StringWriter out = new StringWriter()
        new MarkupBuilder(out).html {
            head()
            body {
                revisions.each { rev ->
                    pre {
                        a(href: "${rev}/", rel: 'nofollow')
                    }
                }
            }
        }
        out.toString()
    }
}
