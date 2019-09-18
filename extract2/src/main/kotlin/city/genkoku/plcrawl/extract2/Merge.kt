package city.genkoku.plcrawl.extract2

fun Sequence<Page>.merge(): Sequence<Pair<String, String>> {
    val group = this.groupBy(
        { it.name.trim().toLowerCase() },
        { it }
    )

    return group.asSequence()
        .map { entry -> Pair(entry.key, entry.value.joinToString("\n") { it.render() }) }
}
