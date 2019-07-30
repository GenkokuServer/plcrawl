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
    return Description(
        map["name"] as String,
        map["version"] as String,
        map["main"] as String,
        map["website"] as String?,
        map["author"] as String?,
        map["authors"] as List<String>?
    )
}

