package com.mindvault.app.domain.analysis

object SynonymMap {

    private val synonymGroups = listOf(
        setOf("image", "photo", "picture", "photograph", "img"),
        setOf("task", "todo", "to-do", "item", "action"),
        setOf("idea", "concept", "thought"),
        setOf("bug", "issue", "problem", "error", "defect"),
        setOf("meeting", "call", "discussion", "standup"),
        setOf("doc", "document", "documentation", "docs"),
        setOf("code", "programming", "coding", "development", "dev"),
        setOf("test", "testing", "qa", "quality"),
        setOf("design", "ui", "ux", "interface", "layout"),
        setOf("deploy", "deployment", "release", "ship"),
        setOf("config", "configuration", "settings", "setup"),
        setOf("db", "database", "storage", "data"),
        setOf("api", "endpoint", "service", "rest"),
        setOf("auth", "authentication", "login", "signin"),
        setOf("note", "notes", "memo", "annotation"),
        setOf("link", "url", "reference", "ref"),
        setOf("file", "attachment", "asset"),
        // Portuguese
        setOf("tarefa", "trabalho", "atividade"),
        setOf("ideia", "conceito", "pensamento"),
        setOf("reunião", "encontro", "chamada"),
        setOf("problema", "erro", "defeito"),
        setOf("aula", "classe", "lição"),
        setOf("estudo", "estudar", "aprendizado"),
    )

    private val synonymLookup: Map<String, Set<String>> = buildMap {
        for (group in synonymGroups) {
            for (word in group) {
                put(word.lowercase(), group.map { it.lowercase() }.toSet() - word.lowercase())
            }
        }
    }

    fun getSynonyms(word: String): Set<String> = synonymLookup[word.lowercase()] ?: emptySet()

    fun expandQuery(query: String): List<String> {
        val words = query.lowercase().split(Regex("\\s+"))
        val expanded = mutableSetOf<String>()
        for (word in words) {
            expanded.add(word)
            expanded.addAll(getSynonyms(word))
        }
        return expanded.toList()
    }
}
