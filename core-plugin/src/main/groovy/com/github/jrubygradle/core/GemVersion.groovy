package com.github.jrubygradle.core

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import java.util.regex.Pattern

/**
 * With rubygems almost all dependencies will be declared
 * via versions ranges and tools like Bundler are very strict on how to
 * resolve those versions - i.e. the resolved version needs to obey each given
 * constraint. Maven does the same but Gradle and Ivy pick the latest and
 * newest version when there are more then one constraint for the same gem -
 * which can create problems when using Bundler alongside Gradle.
 *
 * When converting a GemSpec into a Maven pom.xml the translation of a
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
 *
 * @since 2.0 (Moved here from base plugin where it existed since 0.4.0)
 */
@CompileStatic
class GemVersion implements Comparable<GemVersion> {

    public static final String MAX_VERSION = '99999'

    private static final String LOW_EX = '('
    private static final String LOW_IN = '['
    private static final String UP_EX = ')'
    private static final String UP_IN = ']'
    private static final Pattern DOT_PLUS = Pattern.compile('\\.\\+')
    private static final Pattern PLUS = Pattern.compile('\\+')
    private static final Pattern DIGITS_PLUS = Pattern.compile('[0-9]+\\+')
    private static final Pattern HEAD = Pattern.compile('^.*,\\s*')
    private static final Pattern TAIL = Pattern.compile(',.*$')
    private static final Pattern FIRST = Pattern.compile('^[\\[\\(]')
    private static final Pattern LAST = Pattern.compile('[\\]\\)]$')
    private static final Pattern ZEROS = Pattern.compile('(\\.0)+$')

    private static final Pattern GREATER_EQUAL = ~/^>=\s*(.+)/
    private static final Pattern GREATER = ~/^>\s*(.+)/
    private static final Pattern EQUAL = ~/^=\s*(.+)/
    private static final Pattern LESS = ~/^<\s*(.+)/
    private static final Pattern LESS_EQUAL = ~/^<=\s*(.+)/
    private static final Pattern TWIDDLE_WAKKA = ~/^~>\s*(.+)/

    private static final String VERSION_SPLIT = '[.]'

    final String low
    final String high
    final String prefix = LOW_IN
    final String postfix = UP_IN

    /** Create a Gem version instance from a Gradle version requirement.
     *
     * @param singleRequirement Gradle version string.
     * @return GemVersion instance.
     *
     * @since 2.0
     */
    static GemVersion gemVersionFromGradleRequirement(String singleRequirement) {
        new GemVersion(singleRequirement)
    }

    /** Create a Gem version instance from a single GEM version requirement.
     *
     * @param singleRequirement Single GEM requirement string.
     * @return GemVersion instance.
     *
     * @since 2.0
     */
    @SuppressWarnings('DuplicateStringLiteral')
    static GemVersion gemVersionFromGemRequirement(String singleRequirement) {
        if (singleRequirement.matches(GREATER_EQUAL)) {
            new GemVersion(
                true,
                getVersionFromRequirement(singleRequirement, GREATER_EQUAL),
                "${MAX_VERSION.toString()}.0.0",
                true
            )
        } else if (singleRequirement.matches(GREATER)) {
            new GemVersion(
                false,
                getVersionFromRequirement(singleRequirement, GREATER),
                "${MAX_VERSION.toString()}.0.0",
                true
            )
        } else if (singleRequirement.matches(EQUAL)) {
            String exact = getVersionFromRequirement(singleRequirement, EQUAL)
            new GemVersion(
                true,
                exact,
                exact,
                true
            )
        } else if (singleRequirement.matches(LESS)) {
            new GemVersion(
                true,
                '0.0.0',
                getVersionFromRequirement(singleRequirement, LESS),
                false
            )
        } else if (singleRequirement.matches(LESS_EQUAL)) {
            new GemVersion(
                true,
                '0.0.0',
                getVersionFromRequirement(singleRequirement, LESS_EQUAL),
                true
            )
        } else if (singleRequirement.matches(TWIDDLE_WAKKA)) {
            String base = getVersionFromRequirement(singleRequirement, TWIDDLE_WAKKA)
            int adds = 3 - base.split(VERSION_SPLIT).size()
            if (adds < 0) {
                adds = 0
            }
            new GemVersion(
                true,
                "${base}${'.0' * adds} ",
                "${base}${('.' + MAX_VERSION) * adds}",
                true
            )
        } else {
            throw new GemVersionException("Do not not how to process ${singleRequirement} as a version string")
        }
    }

    /** Is the low version specification inclusive?
     *
     * @return {@code true} if inclusive.
     *
     * @since 2.0
     */
    boolean isLowInclusive() {
        prefix == LOW_IN
    }

    /** Is the high version specification inclusive?
     *
     * @return {@code true} if inclusive.
     *
     * @since 2.0
     */
    boolean isHighInclusive() {
        postfix == UP_IN
    }

    /** Is the high version unspecified?
     *
     * @return {@code true} if the high version is unspecified in the original GEM specification.
     *
     * @since 2.0
     */
    boolean isOpenHigh() {
        high == MAX_VERSION
    }

    /**
     * since GemVersion is version range with lower bound and upper bound
     * this method just calculates the intersection of this version range
     * with the given other version range. it also honors whether the boundary
     * itself is included or excluded by the respective ranges.
     *
     * @param The other version range to be intersected with this version range
     * @return GemVersion the intersected version range
     */
    GemVersion intersect(String otherVersion) {
        intersect(gemVersionFromGradleRequirement(otherVersion))
    }

    /**
     * since GemVersion is version range with lower bound and upper bound
     * this method just calculates the intersection of this version range
     * with the given other version range. it also honors whether the boundary
     * itself is included or excluded by the respective ranges.
     *
     * @param The other version range to be intersected with this version range
     * @return GemVersion the intersected version range
     *
     * @since 2.0
     */
    GemVersion intersect(GemVersion other) {
        String newPrefix
        String newLow
        switch (compare(low, other.low)) {
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

        String newPostfix
        String newHigh

        switch (compare(high, other.high)) {
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
        return new GemVersion(newPrefix, newLow, newHigh, newPostfix)
    }

    /** Creates a new GEM version requirement which that the lowest of two requirements and the highest
     * of those same requirement
     *
     * @param other Other GEM to combine with
     * @return New GEM version requirement.
     *
     * @since 2.0
     */
    GemVersion union(GemVersion other) {
        List<GemVersion> pair = [this, other]
        GemVersion min = pair.min()
        GemVersion max = pair.max()

        new GemVersion(min.prefix, min.low, max.high, max.postfix)
    }

    /** Allows for versions to be compared and sorted.
     *
     * @param other Other GEM version to compare to.
     * @return -1, 0 or 1.
     *
     * @since 2.0
     */
    @Override
    int compareTo(GemVersion other) {
        int loCompare = compare(low, other.low)
        if (loCompare) {
            return loCompare
        }

        if (prefix != other.prefix) {
            return prefix == LOW_IN ? -1 : 1
        }

        int hiCompare = compare(high, other.high)

        if (hiCompare) {
            return hiCompare
        }

        if (postfix != other.postfix) {
            return postfix == UP_IN ? 1 : -1
        }

        0
    }

    /**
     * examines the version range on conflict, i.e. lower bound bigger then
     * upper bound.

     * @return boolean true if lower bound bigger then upper bound
     */
    boolean conflict() {
        return (compare(low, high) == 1)
    }

    /**
     * string of the underlying data as maven version range. for prereleased
     * versions with ranges like [1.pre, 1.pre] the to range will be replaced
     * by the single boundary of the range.
     *
     * @return String maven version range
     */
    String toString() {
        if (prefix == LOW_IN && postfix == UP_IN && low == high && low =~ /[a-zA-Z]/) {
            return low
        }
        return "${prefix}${low},${high}${postfix}"
    }

    @CompileDynamic
    @SuppressWarnings('NoDef')
    private static String getVersionFromRequirement(String gemRevision, Pattern matchPattern) {
        def matcher = gemRevision =~ matchPattern
        matcher[0][1]
    }

    private GemVersion(Boolean lowInclusive, String low, String high, Boolean highInclusive) {
        this.prefix = lowInclusive ? LOW_IN : LOW_EX
        this.low = low
        this.high = high
        this.postfix = highInclusive ? UP_IN : UP_EX
    }

    private GemVersion(String pre, String low, String high, String post) {
        this.prefix = pre
        this.low = low
        this.high = high
        this.postfix = post
    }

    /**
     * converts the given string to a version range with inclusive or
     * exclusive boundaries.
     *
     * @param String gradleVersionPattern
     */
    private GemVersion(final String gradleVersionPattern) {
        if (gradleVersionPattern.contains('+')) {
            low = ZEROS.matcher(PLUS.matcher(DOT_PLUS.matcher(gradleVersionPattern).replaceFirst('.0')).replaceFirst('')).replaceFirst('')
            high = DIGITS_PLUS.matcher(DOT_PLUS.matcher(gradleVersionPattern).replaceFirst('.99999')).replaceFirst(MAX_VERSION)
        } else if (gradleVersionPattern.contains(LOW_IN) || gradleVersionPattern.contains(LOW_EX) ||
            gradleVersionPattern.contains(UP_IN) || gradleVersionPattern.contains(UP_EX)) {
            prefix = gradleVersionPattern.charAt(0).toString()
            postfix = gradleVersionPattern.charAt(gradleVersionPattern.size() - 1).toString()
            low = ZEROS.matcher(FIRST.matcher(TAIL.matcher(gradleVersionPattern).replaceFirst('')).replaceFirst('')).replaceFirst('')
            high = LAST.matcher(HEAD.matcher(gradleVersionPattern).replaceFirst('')).replaceFirst('')

            if (high == '') {
                high = MAX_VERSION
            }
        } else {
            low = gradleVersionPattern
            high = gradleVersionPattern
        }
    }

    /**
     * compares two version strings. first it splits the version
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
    private int compare(String aObject, String bObject) {
        String[] aDigits = aObject.split(VERSION_SPLIT)
        String[] bDigits = bObject.split(VERSION_SPLIT)
        int index = -1

        for (int i = 0; i < aDigits.length && i < bDigits.length; i++) {
            if (aDigits[i] != bDigits[i]) {
                index = i
                break
            }
        }

        if (index == -1) {
            // one contains the other - so look at the length
            if (aDigits.length < bDigits.length) {
                return -1
            }
            if (aDigits.length == bDigits.length) {
                return 0
            }
            return 1
        }

        if (aDigits[index].isInteger() && bDigits[index].isInteger()) {
            // compare them as number
            aDigits[index] as int <=> bDigits[index] as int
        } else {
            // compare them as string
            aDigits[index] <=> bDigits[index]
        }
    }
}
