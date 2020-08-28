package testApi.testRunner

import org.jetbrains.dokka.*
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.doc.Description
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.model.doc.Text
import java.io.File

fun dokkaConfiguration(block: TestDokkaConfigurationBuilder.() -> Unit): DokkaConfigurationImpl =
    TestDokkaConfigurationBuilder().apply(block).build()

@DslMarker
annotation class DokkaConfigurationDsl

@DokkaConfigurationDsl
class TestDokkaConfigurationBuilder {

    var moduleName: String = "root"
        set(value) {
            check(lazySourceSets.isEmpty()) { "Cannot set moduleName after adding source sets" }
            field = value
        }

    var outputDir: String = "out"
    var format: String = "html"
    var offlineMode: Boolean = false
    var cacheRoot: String? = null
    var pluginsClasspath: List<File> = emptyList()
    var pluginsConfigurations: Map<String, String> = emptyMap()
    var failOnWarning: Boolean = false
    private val lazySourceSets = mutableListOf<Lazy<DokkaSourceSetImpl>>()
    fun build() = DokkaConfigurationImpl(
        moduleName = moduleName,
        outputDir = File(outputDir),
        cacheRoot = cacheRoot?.let(::File),
        offlineMode = offlineMode,
        sourceSets = lazySourceSets.map { it.value }.toList(),
        pluginsClasspath = pluginsClasspath,
        pluginsConfiguration = pluginsConfigurations,
        modules = emptyList(),
        failOnWarning = failOnWarning
    )

    fun sourceSets(block: SourceSetsBuilder.() -> Unit) {
        lazySourceSets.addAll(SourceSetsBuilder(moduleName).apply(block))
    }

    fun sourceSet(block: DokkaSourceSetBuilder.() -> Unit): Lazy<DokkaSourceSetImpl> {
        val lazySourceSet = lazy { DokkaSourceSetBuilder(moduleName).apply(block).build() }
        lazySourceSets.add(lazySourceSet)
        return lazySourceSet
    }

    fun unattachedSourceSet(block: DokkaSourceSetBuilder.() -> Unit): DokkaSourceSetImpl {
        return DokkaSourceSetBuilder(moduleName).apply(block).build()
    }
}

@DokkaConfigurationDsl
class SourceSetsBuilder(val moduleName: String) : ArrayList<Lazy<DokkaSourceSetImpl>>() {
    fun sourceSet(block: DokkaSourceSetBuilder.() -> Unit): Lazy<DokkaConfiguration.DokkaSourceSet> =
        lazy { DokkaSourceSetBuilder(moduleName).apply(block).build() }.apply(::add)
}

@DokkaConfigurationDsl
class DokkaSourceSetBuilder(
    private val moduleName: String,
    var name: String = "main",
    var displayName: String = "JVM",
    var classpath: List<String> = emptyList(),
    var sourceRoots: List<String> = emptyList(),
    var dependentSourceSets: Set<DokkaSourceSetID> = emptySet(),
    var samples: List<String> = emptyList(),
    var includes: List<String> = emptyList(),
    var includeNonPublic: Boolean = false,
    var reportUndocumented: Boolean = false,
    var skipEmptyPackages: Boolean = false,
    var skipDeprecated: Boolean = false,
    var jdkVersion: Int = 8,
    var languageVersion: String? = null,
    var apiVersion: String? = null,
    var noStdlibLink: Boolean = false,
    var noJdkLink: Boolean = false,
    var suppressedFiles: List<String> = emptyList(),
    var analysisPlatform: String = "jvm",
    var perPackageOptions: List<PackageOptionsImpl> = emptyList(),
    var externalDocumentationLinks: List<ExternalDocumentationLinkImpl> = emptyList(),
    var sourceLinks: List<SourceLinkDefinitionImpl> = emptyList()
) {
    fun build() = DokkaSourceSetImpl(
        displayName = displayName,
        sourceSetID = DokkaSourceSetID(moduleName, name),
        classpath = classpath.map(::File),
        sourceRoots = sourceRoots.map(::File).toSet(),
        dependentSourceSets = dependentSourceSets,
        samples = samples.map(::File).toSet(),
        includes = includes.map(::File).toSet(),
        includeNonPublic = includeNonPublic,
        reportUndocumented = reportUndocumented,
        skipEmptyPackages = skipEmptyPackages,
        skipDeprecated = skipDeprecated,
        jdkVersion = jdkVersion,
        sourceLinks = sourceLinks.toSet(),
        perPackageOptions = perPackageOptions.toList(),
        externalDocumentationLinks = externalDocumentationLinks.toSet(),
        languageVersion = languageVersion,
        apiVersion = apiVersion,
        noStdlibLink = noStdlibLink,
        noJdkLink = noJdkLink,
        suppressedFiles = suppressedFiles.map(::File).toSet(),
        analysisPlatform = Platform.fromString(analysisPlatform)
    )
}

val defaultSourceSet = DokkaSourceSetImpl(
    displayName = "DEFAULT",
    sourceSetID = DokkaSourceSetID("DEFAULT", "DEFAULT"),
    classpath = emptyList(),
    sourceRoots = emptySet(),
    dependentSourceSets = emptySet(),
    samples = emptySet(),
    includes = emptySet(),
    includeNonPublic = false,
    reportUndocumented = false,
    skipEmptyPackages = true,
    skipDeprecated = false,
    jdkVersion = 8,
    sourceLinks = emptySet(),
    perPackageOptions = emptyList(),
    externalDocumentationLinks = emptySet(),
    languageVersion = null,
    apiVersion = null,
    noStdlibLink = false,
    noJdkLink = false,
    suppressedFiles = emptySet(),
    analysisPlatform = Platform.DEFAULT
)

fun sourceSet(name: String): DokkaConfiguration.DokkaSourceSet {
    return defaultSourceSet.copy(
        displayName = name,
        sourceSetID = defaultSourceSet.sourceSetID.copy(sourceSetName = name)
    )
}

fun documentationNode(vararg texts: String): DocumentationNode {
    return DocumentationNode(texts.toList().map { Description(Text(it)) })
}
