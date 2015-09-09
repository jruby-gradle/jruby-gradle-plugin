package com.github.jrubygradle.internal

import java.util.regex.Pattern

/**
 * since with rubygems most (almost all) dependencies will be declared
 * via versions ranges an tools like bundler are very strict on how to
 * resolve those versions - i.e. the reolved version needs to obey each given
 * contraint. maven does the same but gradle and ivy do pick the latest and
 * newest version when there are more then on contraint for the same gem -
 * which can create problems when using bundler alongside gradle.
 *
 * when converting a gempsec into a maven pom.xml the translation of a
 * gem version range into a maven version range. typically '~> 1.0' from ruby
 * becomes [1.0, 1.99999] on the maven side. so most dependencies from
 * gem artifacts will use such version ranges.
 *
 * to help gradle to be closer to the rubygems world when resolving gem
 * artifacts, it needs to calculate intersection between version ranges
 * in maven manner.
 *
 * this class basically represents a maven version range with boundary
 * (exclusive vs. inclusive) and its lower and upper bounded version and
 * allows to intersect its range with another version range.
 *
 * it also translate fixed version '1.0' to [1.0, 1.0] or the gradle notation
 * 1.2+ to [1.2, 1.99999] or 1.+ to [1.0, 1.99999] following the gemspec-to-pom
 * pattern.
 *
 * @author Christian Meier
 */
class GemVersion {

    private static LOW_EX = '('
    private static LOW_IN = '['
    private static UP_EX = ')'
    private static UP_IN = ']'
    private static Pattern DOT_PLUS = Pattern.compile('\\.\\+')
    private static Pattern PLUS = Pattern.compile('\\+')
    private static Pattern DIGITS_PLUS = Pattern.compile('[0-9]+\\+')
    private static Pattern HEAD = Pattern.compile('^.*,\\s*')
    private static Pattern TAIL = Pattern.compile(',.*$')
    private static Pattern FIRST = Pattern.compile('^[\\[\\(]')
    private static Pattern LAST = Pattern.compile('[\\]\\)]$')
    private static Pattern ZEROS = Pattern.compile('(\\.0)+$')

    private String low
    private String high
    private prefix = LOW_IN
    private postfix = UP_IN

    private GemVersion(String pre, String low, String high, String post) {
        prefix = pre
        this.low = low
        this.high = high
        postfix = post
    }

    /**
     * converts the given string to a version range with inclusive or
     * exclusive boundaries.
     *
     * @param String version
     */
    GemVersion(String version) {
        if (version.contains( '+' ) ) {
            low = ZEROS.matcher(PLUS.matcher(DOT_PLUS.matcher(version).replaceFirst('.0')).replaceFirst('')).replaceFirst('')
            high = DIGITS_PLUS.matcher(DOT_PLUS.matcher(version).replaceFirst('.99999')).replaceFirst('99999')
        }
        else if (version.contains( LOW_IN ) || version.contains( LOW_EX ) ||
                 version.contains( UP_IN ) || version.contains( UP_EX ) ) {
            prefix = version.charAt( 0 ).toString()
            postfix = version.charAt( version.size() - 1 ).toString()
            low = ZEROS.matcher(FIRST.matcher(TAIL.matcher(version).replaceFirst('')).replaceFirst('')).replaceFirst('')
            high = LAST.matcher(HEAD.matcher(version).replaceFirst('')).replaceFirst('')
            if (high == '' ){
              high = '99999'
            }
        }
        else {
            low = version
            high = version
        }
    }

    /**
     * since GemVersion is version range with lower bound and upper bound
     * this method just calculates the intersection of this version range
     * with the given other version range. it also honors whether the boundary
     * itself is included or excluded by the respective ranges.
     *
     * @param String the other version range to be intersected with this version range
     * @return GemVersion the intersected version range
     */
    GemVersion intersect(String otherVersion) {
        def other = new GemVersion( otherVersion )
        def newPrefix
        def newLow
        switch( compare(low, other.low) ) {
        case -1:
            newLow = other.low
            newPrefix = other.prefix
            break
        case 0:
            newPrefix = prefix == LOW_EX || other.prefix == LOW_EX ? LOW_EX : LOW_IN
            newLow = low
            break
        case 1:
            newLow = low
            newPrefix = prefix
        }
        def newPostfix
        def newHigh
        switch( compare(high, other.high) ) {
        case 1:
            newHigh = other.high
            newPostfix = other.postfix
            break
        case 0:
            newPostfix = postfix == UP_EX || other.postfix == UP_EX ? UP_EX : UP_IN
            newHigh = high
            break
        case -1:
            newHigh = high
            newPostfix = postfix
        }
        new GemVersion( newPrefix, newLow, newHigh, newPostfix )
    }

    /**
     * compares to version strings. first it splits the version
     * into parts on their ".". if one version has more parts then
     * the other, then the number of parts is used for comparison.
     * otherwise we find a part which differs between the versions
     * and compare them. this last comparision converts the parts to
     * integers if both contains only digits. otherwise a lexical
     * string comparision is used.
     *
     * @param String aObject first version
     * @param String bObject second version
     * @return int -1 if aObject < bObject, 0 if both are equal and 1 if aObject > bObject
     */
    private int compare( String aObject, String bObject ) {
        def aDigits = aObject.split('[.]')
        def bDigits = bObject.split('[.]')
        def index
        for( int i = 0; i < aDigits.length && i < bDigits.length; i++ ) {
            if (aDigits[ i ] != bDigits[ i ] ) {
                index = i
                break
            }
        }
        if (index == null) {
            // one contains the other - so look at the length
            if ( aDigits.length < bDigits.length ) {
                return -1
            }
            else if ( aDigits.length == bDigits.length ) {
                return 0
            }
            else {
                return 1
            }
        }
        def aaObject
        def bbObject
        if (aDigits[ index ].isInteger() && bDigits[ index ].isInteger()) {
            // compare them as number
            aaObject = aDigits[ index ] as int
            bbObject = bDigits[ index ] as int
        }
        else {
            // compare them as string
            aaObject = aDigits[ index ]
            bbObject = bDigits[ index ]
        }
        if (aaObject < bbObject) {
            -1
        }
        else if (aaObject > bbObject) {
            1
        }
        else {
            0
        }
    }

    /**
     * examines the version range on conflict, i.e. lower bound bigger then
     * upper bound.
     * @return boolean true if lower bound bigger then upper bound
     */
    boolean conflict() {
        compare(low, high) == 1
    }

    /**
     * string of the underlying data as maven version range. for prereleased
     * versions with ranges like [1.pre, 1.pre] the to range will be replaced
     * by the single boundary of the range.
     *
     * @return String maven version range
     */
    String toString(){
        if (prefix == '[' && postfix == ']' && low == high && low =~ /[a-zA-Z]/ ) {
            low
        }
        else {
            "${prefix}${low},${high}${postfix}"
        }
    }
}
