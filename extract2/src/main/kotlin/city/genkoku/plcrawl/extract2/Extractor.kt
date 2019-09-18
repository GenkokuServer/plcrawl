package city.genkoku.plcrawl.extract2

import jp.nephy.jsonkt.*
import jp.nephy.jsonkt.delegation.*
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths

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
        val array = source.toJsonArray()

        array
            .filter {
                !it.jsonObject.contains("tag")
            }
            .forEach { print(it) }

        array.map { it.parse<FinalStatus>() }
            .asSequence()
            .flatMap { status ->
                status
                    .results
                    .asSequence()
                    .map {
                        val desc = it.description
                        val pom = it.poms.firstOrNull()
                        Page(
                            desc.name,
                            desc.version,
                            it.jar,
                            status.tag,
                            desc.website,
                            desc.loadbefore,
                            desc.depend,
                            desc.softdepend,
                            identify(pom),
                            determineBukkitVersion(pom),
                            scm(pom),
                            (desc.authors.asSequence().let { s ->
                                desc.author?.let { s + it } ?: s
                            }).distinct().toList(),
                            extractLicenses(pom)
                        )
                    }
            }
            .merge()
            .forEach {
                var file = fs.getPath("/", "${it.name}.md")
                if (Files.exists(file)) {
                    var i = 0
                    while (Files.exists(file)) {
                        file = fs.getPath("/", "${it.name}${i++}.md")
                    }
                }
                println("Writing to ${file.fileName}...")
                Files.write(file, it.render().split('\n'))
                Files.write(file.resolveSibling(file.fileName.toString() + ".tags"), it.tags)
            }
    }

}

data class FinalStatus(override val json: JsonObject) : JsonModel {
    val timestamp by string
    val tag by string
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
