package city.genkoku.plcrawl

import city.genkoku.plcrawl.bukkit.Description
import city.genkoku.plcrawl.bukkit.readPluginData
import city.genkoku.plcrawl.maven.Project
import city.genkoku.plcrawl.maven.readArchivedPOMs
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import kotlin.streams.asSequence


@Serializable
data class FinalStatus(
    val timestamp: String,
    val results: List<ExtractionResult>
)

@Serializable
data class ExtractionResult(
    val description: Description,
    val foundPoms: List<Project>
)


@UnstableDefault
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        print("plcrawl <plugins directory>")
        return
    }
    val dir = Paths.get(args[0])
    val json = Json(JsonConfiguration.Default)

    Files.list(dir).asSequence()
        .filter { it.last().toString().endsWith(".jar") }
        .filter { Files.size(it) > 0 }
        .map { file ->
            runCatching {
                FileSystems.newFileSystem(file, null)
            }.recover {
                System.err.println("Corrupted archive is detected: '$file'")
                it.printStackTrace()
                null
            }.getOrThrow()
        }
        .filterNotNull()
        .flatMap { it.rootDirectories.asSequence() }
        .map { root ->
            readPluginData(root)?.let {
                ExtractionResult(it, readArchivedPOMs(root))
            }
        }
        .filterNotNull()
        .toList()
        .let { FinalStatus(ZonedDateTime.now().toString(), it) }
        .let {
            println(json.stringify(FinalStatus.serializer(), it))
        }
}

