package city.genkoku.plcrawl.extract2

import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths


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

data class ExtractedData(
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
            "# $name",
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

fun extractLicenses(pom: JsonElement?): List<String>? {
    return pom?.let {
        it["licenses"].jsonArray.map { license ->
            val name = license["name"].string
            license["url"].stringOrNull
                ?.let { url -> "[$name]($url)" } ?: name
        }

    }
}

fun identify(pom: JsonElement?): String? {
    return pom?.let {
        try {
            return "${it["groupId"]}:${it["artifactId"]}:${it["version"]}"
        } catch (ex: JsonNullPointerException) {
            println(ex.json)
            throw ex
        }
    }
}

fun scm(pom: JsonElement?): String? {
    return pom?.let {
        it.jsonObject.getObjectOrNull("scm")?.let {
            it["url"]?.stringOrNull
        }
    }
}

val KNOWN_BUKKIT_DEPENDENCIES = arrayOf(
    "org.bukkit:bukkit:jar",
    "org.bukkit:craftbukkit:jar",
    "org.spigotmc:spigot-api:jar",
    "org.spigotmc:spigot:jar",
    "com.destroystokyo.paper:paper-api:jar"
)

fun determineBukkitVersion(pom: JsonElement?): String? {
    return pom?.let {
        it["dependencies"].jsonArray
            .asSequence()
            .filter {
                KNOWN_BUKKIT_DEPENDENCIES.contains(it["managementKey"].string)
            }
            .firstOrNull()
            ?.let {
                identify(it)
            }
    }
}

fun main() {
    val results = Paths.get("results.zip");
    if (Files.exists(results)) {
        System.err.println("results.zip is already exists!")
        return
    }
    val uri = URI.create("jar:" + results.toUri())
    FileSystems.newFileSystem(uri, mapOf(Pair("create", "true"))).use { fs ->
        val source = generateSequence { readLine() }.joinToString("\n")
        source.parse<FinalStatus>()
            .results
            .map {
                val desc = it.description
                val pom = it.poms.firstOrNull()
                ExtractedData(
                    desc.name,
                    desc.version,
                    it.jar,
                    desc.website,
                    desc.loadbefore,
                    desc.depend,
                    desc.softdepend,
                    identify(pom),
                    determineBukkitVersion(pom),
                    scm(pom),
                    (desc.authors.asSequence().let { s -> desc.author?.let { s + it } ?: s }).distinct().toList(),
                    extractLicenses(pom)
                )
            }
            .map {
                Pair(it.jar, it.render())
            }
            .forEach {
                var file = fs.getPath("/", "${it.first}.md")
                var i = 0
                while (Files.exists(file)) {
                    file = fs.getPath("/", "${it.first}${i++}.md")
                }
                println("Writing to ${file.fileName}...")
                Files.write(file, it.second.split('\n'));
            }
    }

}

data class FinalStatus(override val json: JsonObject) : JsonModel {
    val timestamp by string
    val results by modelList<CollectionResult>()
}

data class CollectionResult(override val json: JsonObject) : JsonModel {
    val description by model<Description>()
    val poms by jsonArray
    val jar by string
}

data class Description(override val json: JsonObject) : JsonModel {
    val author by nullableString
    val authors by stringList
    val loadbefore by stringList
    val softdepend by stringList
    val depend by stringList
    val main by string
    val name by string
    val version by string
    val website by nullableString
}
