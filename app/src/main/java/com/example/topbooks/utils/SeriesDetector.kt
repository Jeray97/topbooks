package com.example.topbooks.utils

object SeriesDetector {

    data class SeriesInfo(
        val name: String,
        val index: Int
    )

    fun detect(title: String): SeriesInfo? {

        val cleanTitle = title.lowercase()

        val patterns = listOf(

            // Harry Potter (Book 4)
            Regex("(.*)\\s*\\(book\\s*(\\d+)\\)", RegexOption.IGNORE_CASE),

            // Mistborn #1
            Regex("(.*)\\s*#(\\d+)"),

            // Dune 2
            Regex("(.*)\\s(\\d+)$"),

            // Vol 1
            Regex("(.*)\\s(vol\\.?|volume)\\s*(\\d+)", RegexOption.IGNORE_CASE)
        )

        for (regex in patterns) {

            val match = regex.find(cleanTitle)

            if (match != null) {

                val name = match.groupValues[1].trim()
                val index = match.groupValues.last().toIntOrNull() ?: 0

                return SeriesInfo(
                    name.replaceFirstChar { it.uppercase() },
                    index
                )
            }
        }

        return null
    }
}