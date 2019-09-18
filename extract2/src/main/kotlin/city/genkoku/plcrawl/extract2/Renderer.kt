package city.genkoku.plcrawl.extract2

fun Any?.renderNullable(header: String): String? {
    return this?.let {
        val str = it.toString()
        "$header: ${
        if (str.contains("://"))
            str
        else
            "`$str`"
        }"

    }
}

fun List<*>?.renderList(header: String): String? {
    return this?.let {
        if (it.isEmpty()) return null
        return (sequenceOf("$header:")
                + it.asSequence().map { e -> "  - $e" })
            .joinToString("\n") + "\n"
    }
}

fun String.render(element: Any?): String? {
    return when (element) {
        is List<*>? -> element.renderList(this)
        else -> element.renderNullable(this)
    }
}
