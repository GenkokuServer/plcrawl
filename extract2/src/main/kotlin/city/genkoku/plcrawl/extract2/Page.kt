package city.genkoku.plcrawl.extract2

class Page(
    val name: String,
    val version: String,
    val jar: String,
    val website: String?,
    val loadbefore: List<String>?,
    val depend: List<String>?,
    val softdepend: List<String>?,
    val projectId: String?,
    val bukkitVersion: String?,
    val scm: String?,
    val authors: List<String>?,
    val licenses: List<String>?
) {
    fun render(): String {
        return sequenceOf(
            "## $name",
            "",
            "名前: $name",
            "バージョン: $version",
            "JAR".render(jar),
            "Web サイト".render(website),
            "このプラグインを前提とするプラグイン".render(loadbefore),
            "依存関係".render(depend),
            "連携可能なプラグイン".render(softdepend),
            "",
            "プロジェクト ID".render(projectId),
            "コンパイル時の Bukkit バージョン".render(bukkitVersion),
            "リポジトリ".render(scm),
            "著作者".render(authors),
            "ライセンス".render(licenses),
            ""
        )
            .filterNotNull()
            .joinToString("  \n")
    }

}
