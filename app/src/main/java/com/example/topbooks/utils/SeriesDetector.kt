package com.example.topbooks.utils

object SeriesDetector {

    data class SeriesInfo(
        val name: String,
        val index: Int
    )

    private val ROMAN_MAP = mapOf(
        "i" to 1, "ii" to 2, "iii" to 3, "iv" to 4, "v" to 5,
        "vi" to 6, "vii" to 7, "viii" to 8, "ix" to 9, "x" to 10,
        "xi" to 11, "xii" to 12, "xiii" to 13, "xiv" to 14, "xv" to 15
    )

    private val SERIES_KEYWORDS = listOf(
        "saga", "serie", "series", "trilogía", "trilogia", "trilogy",
        "duología", "duologia", "duology", "tetralogía", "tetralogia", "tetralogy",
        "crónicas", "cronicas", "chronicles", "ciclo", "cycle",
        "colección", "coleccion", "collection"
    )

    private val YEAR_REGEX = Regex("\\b(19|20)\\d{2}\\b")

    fun detect(title: String): SeriesInfo? {
        val cleanTitle = title.trim()
        val lowerTitle = cleanTitle.lowercase()

        if (YEAR_REGEX.containsMatchIn(cleanTitle)) {
            val withoutYears = YEAR_REGEX.replace(cleanTitle, "").replace("()", "").trim()
            val result = matchPatterns(withoutYears)
            if (result != null) return result
        }

        val result = matchPatterns(cleanTitle)
        if (result != null) return result

        return detectByKeywords(lowerTitle)
    }

    private fun matchPatterns(title: String): SeriesInfo? {
        val lowerTitle = title.lowercase()

        val patterns = listOf(
            Regex("\\(([^()]+?)\\s*[,;]\\s*(?:book|libro|vol\\.?|volume|#)\\s*(\\d+)\\)", RegexOption.IGNORE_CASE),
            Regex("\\((?:book|libro|vol\\.?|volume)\\s*(\\d+)\\s+(?:of\\s+)?([^()]+)\\)", RegexOption.IGNORE_CASE),
            Regex("(?:book|libro)\\s+(\\d+)\\s+(?:of(?:\\s+the)?|de(?:\\s+la)?)\\s+(.+?)(?:\\s*(?:series|saga|trilogy|trilogía|trilogia)|[\\s,;:!?\\-–—]|$)", RegexOption.IGNORE_CASE),
            Regex("(?:book|libro)\\s+(\\d+)\\s*[-–—:]\\s*(.+?)(?:\\s*(?:series|saga)|[\\s,;:!?\\-–—]|$)", RegexOption.IGNORE_CASE),
            Regex("(.+?)\\s*[-–—]\\s*(?:book|libro)\\s+(\\d+)", RegexOption.IGNORE_CASE),
            Regex("(.+?)\\s*[-–—]\\s*(?:vol\\.?|volume)\\s+(\\d+)", RegexOption.IGNORE_CASE),
            Regex("(.+?)\\s*[-–—]\\s*#(\\d+)", RegexOption.IGNORE_CASE),
            Regex("\\((.+?)\\s+(\\d+)\\)"),
            Regex("(.+?)\\s+(\\d+)\\s+(?:of\\s+\\d+|de\\s+\\d+)"),
            Regex("\\((.+?)\\s+(i{1,3}|iv|vi{0,3}|ix|xi{0,3}|xiv|xv)\\)", RegexOption.IGNORE_CASE),
            Regex("(.+?)\\s+(i{1,3}|iv|vi{0,3}|ix|xi{0,3}|xiv|xv)\\s*$", RegexOption.IGNORE_CASE),
            Regex("(.+?)\\s+(?:vol\\.?|volume)\\s+(\\d+)", RegexOption.IGNORE_CASE),
            Regex("(.+?)\\s*#(\\d+)\\s*$"),
            Regex("(.+?)\\s+(\\d+)\\s*$")
        )

        for (regex in patterns) {
            val match = regex.find(lowerTitle) ?: continue

            val groups = match.groupValues
            var name: String
            var index: Int

            val g1 = groups.getOrNull(1)?.trim() ?: ""
            val g2 = groups.getOrNull(2)?.trim() ?: ""

            val g1AsRoman = ROMAN_MAP[g1.lowercase()]
            val g2AsRoman = ROMAN_MAP[g2.lowercase()]
            val g1AsInt = g1.toIntOrNull()
            val g2AsInt = g2.toIntOrNull()

            when {
                g2AsInt != null -> {
                    name = g1
                    index = g2AsInt
                }
                g1AsInt != null && g2.isNotBlank() -> {
                    name = g2
                    index = g1AsInt
                }
                g2AsRoman != null -> {
                    name = g1
                    index = g2AsRoman
                }
                g1AsRoman != null && g2.isNotBlank() -> {
                    name = g2
                    index = g1AsRoman
                }
                else -> continue
            }

            name = cleanSeriesName(name)

            if (name.length < 2 || name.matches(Regex("^\\d+$"))) continue
            if (YEAR_REGEX.matches(name)) continue

            return SeriesInfo(
                name = name.replaceFirstChar { it.uppercase() },
                index = index
            )
        }

        return null
    }

    private fun detectByKeywords(lowerTitle: String): SeriesInfo? {
        for (keyword in SERIES_KEYWORDS) {
            val regex = Regex("(?:$keyword)\\s+(?:de\\s+|of\\s+(?:the\\s+)?)?(.+?)(?:\\s*[,;#(\\-–—]|\\s+\\d+\\s*$|$)", RegexOption.IGNORE_CASE)
            val match = regex.find(lowerTitle) ?: continue

            var name = match.groupValues[1].trim()
            name = cleanSeriesName(name)

            if (name.length < 2) continue

            val indexMatch = Regex("(?:book|libro|vol\\.?|#)\\s*(\\d+)", RegexOption.IGNORE_CASE).find(lowerTitle)
            val index = indexMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            return SeriesInfo(
                name = name.replaceFirstChar { it.uppercase() },
                index = index
            )
        }
        return null
    }

    private fun cleanSeriesName(name: String): String {
        var cleaned = name
        cleaned = cleaned.replace(Regex("^[-–—:,;\\s]+"), "")
        cleaned = cleaned.replace(Regex("[-–—:,;\\s]+$"), "")
        cleaned = cleaned.replace(Regex("\\(.*?\\)"), "")
        cleaned = cleaned.replace(Regex("^\\s*(?:the|la|el|los|las)\\s+", RegexOption.IGNORE_CASE), "")
        cleaned = cleaned.trim()
        return cleaned
    }
}
