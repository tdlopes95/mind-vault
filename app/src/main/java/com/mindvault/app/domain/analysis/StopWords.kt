package com.mindvault.app.domain.analysis

object StopWords {
    val english = setOf(
        "a", "an", "the", "is", "are", "was", "were", "be", "been", "being",
        "have", "has", "had", "do", "does", "did", "will", "would", "could",
        "should", "may", "might", "can", "shall", "must", "need",
        "i", "me", "my", "we", "our", "you", "your", "he", "she", "it",
        "they", "them", "their", "this", "that", "these", "those",
        "in", "on", "at", "to", "for", "of", "with", "by", "from", "up",
        "about", "into", "through", "during", "before", "after",
        "and", "but", "or", "nor", "not", "so", "yet", "both", "either",
        "if", "then", "else", "when", "where", "how", "what", "which", "who",
        "all", "each", "every", "any", "some", "no", "other", "such",
        "just", "also", "very", "too", "quite", "really", "only",
        "here", "there", "now", "then", "still", "already",
        "more", "most", "less", "least", "much", "many", "few",
        "new", "old", "good", "bad", "great", "little", "big",
        "like", "know", "think", "make", "go", "get", "see", "come",
        "want", "use", "find", "give", "tell", "work", "call", "try",
        "than", "well", "back", "even", "way", "over"
    )

    val portuguese = setOf(
        "o", "a", "os", "as", "um", "uma", "uns", "umas",
        "de", "do", "da", "dos", "das", "em", "no", "na", "nos", "nas",
        "por", "para", "com", "sem", "sob", "sobre", "entre",
        "e", "ou", "mas", "porém", "contudo", "todavia",
        "que", "se", "como", "quando", "onde", "qual", "quem",
        "eu", "tu", "ele", "ela", "nós", "vós", "eles", "elas",
        "me", "te", "se", "nos", "vos", "lhe", "lhes",
        "meu", "minha", "teu", "tua", "seu", "sua",
        "este", "esta", "esse", "essa", "aquele", "aquela",
        "isto", "isso", "aquilo",
        "ser", "estar", "ter", "haver", "fazer", "poder", "ir",
        "é", "são", "foi", "está", "tem", "há", "vai",
        "não", "sim", "mais", "menos", "muito", "pouco",
        "bem", "mal", "já", "ainda", "também", "só", "apenas"
    )

    val all = english + portuguese

    fun isStopWord(word: String): Boolean = word.lowercase() in all
}
