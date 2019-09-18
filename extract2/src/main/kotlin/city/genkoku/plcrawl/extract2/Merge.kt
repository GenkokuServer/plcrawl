package city.genkoku.plcrawl.extract2

fun Sequence<Page>.merge(): Sequence<MergedPage> {
    val group = this.groupBy(
        { it.name.trim().toLowerCase() },
        { it }
    )

    return group.asSequence().map { entry -> MergedPage(entry.key, entry.value) }
}


data class MergedPage(
    val name: String,
    val pages: Iterable<Page>
) {

    val tags: Iterable<String> = pages.map { it.server }

    fun render(): String {
        return pages.joinToString("\n") { it.render() }
    }

}