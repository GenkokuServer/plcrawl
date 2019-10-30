package city.genkoku.plcrawl.finalize

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import org.eclipse.egit.github.core.Issue
import org.eclipse.egit.github.core.Label
import org.eclipse.egit.github.core.client.GitHubClient
import org.eclipse.egit.github.core.service.IssueService
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.*
import kotlin.streams.asSequence
import kotlin.streams.toList

const val ENDPOINT = "https://api.github.com"
val GSON = Gson()

data class AccessToken(
    val token: String
)

data class Configuration(
    val installationId: String,
    val issuer: String,
    val user: String,
    val repository: String,
    val color: String,
    val specialName: String,
    val keyPath: Path,
    val permittedRepositories: Collection<String>
) {
    constructor(properties: Properties) : this(
        properties["finalizer.installation_id"].toString(),
        properties["finalizer.issuer"].toString(),
        properties["finalizer.user"].toString(),
        properties["finalizer.repository"].toString(),
        properties["finalizer.issue.label.color"].toString(),
        properties["finalizer.issue.label.special_name"].toString(),
        Paths.get(properties["finalizer.authentication.key"].toString()),
        properties["finalizer.authentication.permitted_repositories"].toString().split(",")
    )
}

fun main() {
    val resultsArchive = Paths.get("results.zip");
    if (Files.notExists(resultsArchive)) {
        System.err.println("results.zip is not found")
        return
    }

    val configuration = Properties()
        .apply {
            ClassLoader.getSystemResourceAsStream("security.properties").use {
                this.load(InputStreamReader(it, Charsets.UTF_8))
            }
        }
        .let { Configuration(it) }

    val client = authenticate(configuration)
    val issues = IssueService(client)

    val uri = URI.create("jar:" + resultsArchive.toUri())
    FileSystems.newFileSystem(uri, emptyMap<String, Unit>()).use { fs ->
        fs.rootDirectories
            .asSequence()
            .flatMap { Files.list(it).asSequence() }
            .filter { it.toString().endsWith(".md") }
            .forEach {
                val body = Files.readAllLines(it)
                val tags = it.resolveSibling(it.fileName.toString() + ".tags")
                    .let { tagFile ->
                        when (Files.exists(tagFile)) {
                            true -> Files.lines(tagFile).distinct().toList()
                            else -> emptyList()
                        }
                    }
                issues.emitIssue(body, tags, configuration)
            }
    }
}

fun IssueService.emitIssue(markdown: Collection<String>, tags: Collection<String>, configuration: Configuration) {
    val issue = Issue()
    issue.title = markdown.first().replace("#", "").trim()
    issue.body = markdown.joinToString("\n")
    issue.labels = (tags + configuration.specialName).map { name ->
        Label().apply {
            this.color = configuration.color
            this.name = name
        }
    }

    println(GSON.toJson(issue))
    println(
        "Created to " + GSON.toJson(
            createIssue(
                configuration.user,
                configuration.repository,
                issue
            )
        )
    )
}

fun authenticate(configuration: Configuration): GitHubClient {
    val now = Instant.now()
    val expiresAt = now.plus(Duration.ofMinutes(10))

    val keySpec = PKCS8EncodedKeySpec(
        Base64.getDecoder().decode(
            Files.readAllLines(configuration.keyPath).joinToString("")
        )
    )

    val keyFactory = KeyFactory.getInstance("RSA")
    val privateKey = keyFactory.generatePrivate(keySpec) as RSAPrivateKey

    val jwt = JWT.create()
        .withIssuer(configuration.issuer)
        .withIssuedAt(Date.from(now))
        .withExpiresAt(Date.from(expiresAt))
        .sign(Algorithm.RSA256(null, privateKey))

    val client = GitHubClient.createClient(ENDPOINT)
    println(jwt)

    val permissions = mapOf(
        Pair(
            "repository_ids", configuration.permittedRepositories
        ),
        Pair(
            "permissions",
            mapOf(
                Pair("issues", "write")
            )
        )
    )

    println(GSON.toJson(permissions))

    val http = URL("$ENDPOINT/app/installations/${configuration.installationId}/access_tokens")
        .openConnection() as HttpURLConnection
    http.requestMethod = "POST"
    http.setRequestProperty("Authorization", "Bearer $jwt")
    http.setRequestProperty("Accept", "application/vnd.github.machine-man-preview+json")
    http.doInput = true
    http.connect()

    val response = http.inputStream.reader().use { GSON.fromJson(it, AccessToken::class.java) }
    client.setOAuth2Token(response.token)

    return client
}
