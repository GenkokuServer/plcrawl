package city.genkoku.plcrawl.maven

import java.nio.file.Files
import java.nio.file.Path
import javax.xml.bind.JAXBContext
import javax.xml.bind.UnmarshalException
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElementWrapper
import javax.xml.bind.annotation.XmlRootElement
import kotlin.streams.asSequence

fun readArchivedPOMs(root: Path): List<Project> {
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
        .filterNotNull()
        .toList()
}

fun readPOM(xmlFile: Path): Project? {
    val lines = Files.readAllLines(xmlFile)
    if (lines.isEmpty())
        return null
    return try {
        unmarshalPOM(lines.joinToString(System.lineSeparator()))
    } catch (ex: UnmarshalException) {
        System.err.println(ex.localizedMessage)
        null
    }
}

fun unmarshalPOM(xml: String): Project? {
    val context = JAXBContext.newInstance(ProjectBuilder::class.java)
    val unmarshaller = context.createUnmarshaller()
    return xml.reader().use {
        (unmarshaller.unmarshal(it) as ProjectBuilder?)?.build()
    }
}

interface POMBuilder<T> {

    fun build(): T

}

private class DependencyBuilder : POMBuilder<Dependency> {
    @get:XmlElement(name = "groupId")
    var groupId: String? = null

    @get:XmlElement(name = "artifactId")
    var artifactId: String? = null

    @get:XmlElement(name = "version")
    var version: String? = null

    @get:XmlElement(name = "scope", defaultValue = "compile")
    var scope: String? = "compile"

    @get:XmlElement(name = "classifier")
    var classifier: String? = null

    override fun toString(): String {
        return "$scope \"$groupId:$artifactId:$version:$classifier\""
    }

    override fun build(): Dependency {
        return Dependency(
            groupId ?: "",
            artifactId ?: "",
            version ?: "",
            scope ?: "compile",
            classifier ?: ""
        )
    }

}

private class ParentBuilder : POMBuilder<Parent> {

    @get:XmlElement
    var groupId: String? = null

    @get:XmlElement
    var artifactId: String? = null

    @get:XmlElement
    var version: String? = null

    @get:XmlElement
    var relativePath: String? = null

    override fun build(): Parent {
        return Parent(
            groupId!!,
            artifactId!!,
            version!!,
            relativePath
        )
    }

}

private class LicenseBuilder : POMBuilder<License> {

    @XmlElement
    var name: String? = null

    @XmlElement
    var url: String? = null

    @XmlElement
    var distribution: String? = null

    @XmlElement
    var comments: String? = null

    override fun build(): License {
        return License(
            name ?: "All rights reserved",
            url ?: "",
            distribution,
            comments
        )
    }

}

@XmlRootElement(name = "project")
private class ProjectBuilder : POMBuilder<Project> {

    @get:XmlElement
    var parent: ParentBuilder? = null

    @get:XmlElement
    var groupId: String? = null

    @get:XmlElement(required = true)
    var artifactId: String? = null

    @get:XmlElement
    var version: String? = null

    @get:XmlElement
    var properties: Map<String, String>? = null

    @get:XmlElementWrapper(name = "dependencies")
    @get:XmlElement(name = "dependency")
    var dependencies: List<DependencyBuilder>? = null

    override fun toString(): String {
        return "$groupId:$artifactId:$version[properties=$properties, dependencies=$dependencies]"
    }

    override fun build(): Project {
        return Project(
            groupId ?: "",
            artifactId ?: "",
            version ?: "",
            properties ?: mapOf(),
            dependencies?.map { it.build() } ?: listOf(),
            parent?.build()
        )
    }

}
