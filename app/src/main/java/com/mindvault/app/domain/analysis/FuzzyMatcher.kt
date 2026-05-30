package com.mindvault.app.domain.analysis

object FuzzyMatcher {

    fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        return dp[m][n]
    }

    fun isFuzzyMatch(query: String, target: String): Boolean {
        val len = query.length
        val tolerance = when {
            len <= 4 -> 1
            len <= 7 -> 2
            else -> 3
        }
        return levenshteinDistance(query.lowercase(), target.lowercase()) <= tolerance
    }

    fun fuzzySearch(query: String, candidates: List<String>): List<Pair<String, Int>> {
        val queryLower = query.lowercase()
        val len = queryLower.length
        val tolerance = when {
            len <= 4 -> 1
            len <= 7 -> 2
            else -> 3
        }
        return candidates
            .map { it to levenshteinDistance(queryLower, it.lowercase()) }
            .filter { (_, dist) -> dist <= tolerance }
            .sortedBy { (_, dist) -> dist }
    }
}
