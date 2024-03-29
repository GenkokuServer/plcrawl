package city.genkoku.plcrawl.collector.maven

import org.apache.maven.model.Model
import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.codehaus.plexus.util.xml.pull.XmlPullParserException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

fun readArchivedPOMs(root: Path): List<Model> {
    val foundXMLs = mutableListOf<Path>(
        // pom.xml in root
        root.resolve("pom.xml"),
        // pom.xml in src/ directory
        root.resolve("src").resolve("pom.xml")
    )

    // from /META-INF/maven/${groupId}/${artifactId}/pom.xml
    val metaDir = root.resolve("META-INF")
    if (Files.exists(metaDir)) {
        val mavenDir = metaDir.resolve("maven")
        if (Files.exists(mavenDir)) {
            foundXMLs.addAll(
                Files.list(mavenDir).asSequence()
                    .filter { Files.isDirectory(it) }
                    .flatMap { groupDir ->
                        Files.list(groupDir).iterator().asSequence()
                    }
                    .map { it.resolve("pom.xml") }
            )
        }
    }

    return foundXMLs
        .asSequence()
        .filter { Files.isRegularFile(it) }
        .map(::readPOM)
        .filterAll()
        .toList()
}

fun Sequence<Model?>.filterAll(): Sequence<Model> {
    return this
        .filterNotNull()
        .filter(::filterPOM)
        .map(::inheritPOM)
}

val KNOWN_BUKKIT_DEPENDENCIES = arrayOf(
    "org.bukkit:bukkit:jar",
    "org.bukkit:craftbukkit:jar",
    "org.spigotmc:spigot-api:jar",
    "org.spigotmc:spigot:jar",
    "com.destroystokyo.paper:paper-api:jar"
)

fun filterPOM(pom: Model): Boolean {
    return pom.dependencies
        .asSequence()
        .filter { KNOWN_BUKKIT_DEPENDENCIES.contains(it.managementKey) }
        .count() > 0
}

fun inheritPOM(pom: Model): Model {
    pom.groupId = pom.groupId ?: pom.parent.groupId
    pom.version = pom.version ?: pom.parent.version
    return pom
}

fun readPOM(xmlFile: Path): Model? {
    val lines = Files.readAllLines(xmlFile)
    if (lines.isEmpty())
        return null
    return try {
        unmarshalPOM(lines.joinToString(System.lineSeparator()))
    } catch (ex: XmlPullParserException) {
        System.err.println(ex.localizedMessage)
        null
    }
}

fun unmarshalPOM(xml: String): Model? {
    val properties = xml.reader().use {
        MavenXpp3Reader().read(it).properties
    }

    var xmlMutable = xml
    for (key in properties.keys) {
        val value = (key as String).resolve(properties as Map<String, String>);
        System.err.println("Processing property $key: $value")

        value?.also {
            xmlMutable = xmlMutable.replace(
                "\${${key as String}}",
                it
            )
        }
    }

    return xmlMutable.reader().use {
        MavenXpp3Reader().read(it)
    }
}
