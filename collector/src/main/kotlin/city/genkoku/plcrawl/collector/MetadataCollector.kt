package city.genkoku.plcrawl.collector

import city.genkoku.plcrawl.collector.bukkit.Description
import city.genkoku.plcrawl.collector.bukkit.readPluginData
import city.genkoku.plcrawl.collector.maven.readArchivedPOMs
import com.alibaba.fastjson.JSON
import org.apache.maven.model.Model
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.ZonedDateTime
import kotlin.streams.asSequence


data class FinalStatus(
    val timestamp: String,
    val tag: String,
    val results: List<CollectionResult>
)

data class CollectionResult(
    val description: Description,
    val poms: List<Model>,
    val jar: String
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
        .flatMap { file ->
            runCatching {
                val fs = FileSystems.newFileSystem(file, null)
                System.err.println("Reading $file...")
                fs.rootDirectories.asSequence().map { root ->
                    readPluginData(root)?.let {
                        CollectionResult(
                            it,
                            readArchivedPOMs(root),
                            file.fileName.toString()
                        )
                    }
                }
            }.onFailure {
                System.err.println("Corrupted archive is detected: '$file'")
                it.printStackTrace()
            }.getOrElse { sequenceOf() }
        }
        .filterNotNull()
        .toList()
        .let { FinalStatus(ZonedDateTime.now().toString(), dir.fileName.toString(), it) }
        .let {
            println(JSON.toJSONString(it))
        }
}

