package city.genkoku.plcrawl

import city.genkoku.plcrawl.bukkit.Description
import city.genkoku.plcrawl.bukkit.readPluginData
import city.genkoku.plcrawl.maven.readArchivedPOMs
import com.alibaba.fastjson.JSON
import org.apache.maven.model.Model
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import kotlin.streams.asSequence
import kotlin.streams.asStream


data class FinalStatus(
    val timestamp: String,
    val results: List<ExtractionResult>
)

data class ExtractionResult(
    val description: Description,
    val poms: List<Model>
)


fun main(args: Array<String>) {
    if (args.isEmpty()) {
        print("plcrawl <plugins directory>")
        return
    }
    val dir = Paths.get(args[0])

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
        .asStream()
        .parallel()
        .map { root ->
            readPluginData(root)?.let {
                ExtractionResult(it, readArchivedPOMs(root))
            }
        }
        .asSequence()
        .filterNotNull()
        .toList()
        .let { FinalStatus(ZonedDateTime.now().toString(), it) }
        .let {
            println(JSON.toJSONString(it))
        }
}

