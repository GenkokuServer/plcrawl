package city.genkoku.plcrawl.bukkit


import kotlinx.serialization.Serializable
import org.yaml.snakeyaml.Yaml
import java.nio.file.Files
import java.nio.file.Path

@Serializable
data class Description(
    val name: String,
    val version: String,
    val main: String,
    val website: String? = null,
    val author: String? = null,
    val authors: List<String>? = null
)

fun readPluginData(root: Path): Description? {
    val descriptionFile = root.resolve("plugin.yml");
    if (Files.notExists(descriptionFile))
        return null

    val map = Yaml().loadAs(Files.newInputStream(descriptionFile), Map::class.java)

    val name = map["name"]
    val version = map["version"]
    val main = map["main"]
    val website = map["website"]
    val author = map["author"]
    val authors = map["authors"]

    author?.let {
        if (it !is String) {
            System.err.println(
                "Warning: '$name' has invalid plugin.yml (property 'author' is ${it.javaClass})"
            )
        }
    }

    return Description(
        name as String,
        version.toString(),
        main as String,
        website as String?,
        author?.toString(),
        authors as List<String>?
    )
}

