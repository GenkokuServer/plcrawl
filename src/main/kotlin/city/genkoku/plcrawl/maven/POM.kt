package city.genkoku.plcrawl.maven

import kotlinx.serialization.Serializable


@Serializable
data class Dependency(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val scope: String = "compile",
    val classifier: String? = null
)

@Serializable
data class Parent(
    val groupId: String,
    val artifactId: String,
    val version: String,
    val relativePath: String? = null
)

@Serializable
data class License(
    val name: String,
    val url: String,
    val distribution: String? = null,
    val comments: String? = null
)

@Serializable
data class Project(
    val groupId: String = "",
    val artifactId: String,
    val version: String = "",
    val properties: Map<String, String> = mapOf(),
    val dependencies: List<Dependency> = listOf(),
    val parent: Parent? = null
)