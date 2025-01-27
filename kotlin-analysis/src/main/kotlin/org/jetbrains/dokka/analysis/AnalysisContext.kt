package org.jetbrains.dokka.analysis

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.Platform
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import java.io.Closeable
import java.io.File

internal fun createAnalysisContext(
    logger: DokkaLogger,
    sourceSets: List<DokkaConfiguration.DokkaSourceSet>,
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    analysisConfiguration: DokkaAnalysisConfiguration
): AnalysisContext {
    val parentSourceSets = sourceSets.filter { it.sourceSetID in sourceSet.dependentSourceSets }
    val classpath = sourceSet.classpath + parentSourceSets.flatMap { it.classpath }
    val sources = sourceSet.sourceRoots + parentSourceSets.flatMap { it.sourceRoots }

    return createAnalysisContext(
        logger = logger,
        classpath = classpath,
        sourceRoots = sources,
        sourceSet = sourceSet,
        analysisConfiguration = analysisConfiguration
    )
}

internal fun createAnalysisContext(
    logger: DokkaLogger,
    classpath: List<File>,
    sourceRoots: Set<File>,
    sourceSet: DokkaConfiguration.DokkaSourceSet,
    analysisConfiguration: DokkaAnalysisConfiguration
): AnalysisContext {
    val analysisEnvironment = AnalysisEnvironment(DokkaMessageCollector(logger), sourceSet.analysisPlatform).apply {
        if (analysisPlatform == Platform.jvm) {
            configureJdkClasspathRoots()
        }
        addClasspath(classpath)
        addSources(sourceRoots)

        loadLanguageVersionSettings(sourceSet.languageVersion, sourceSet.apiVersion)
    }

    val environment = analysisEnvironment.createCoreEnvironment()
    val (facade, _) = analysisEnvironment.createResolutionFacade(
        environment,
        analysisConfiguration.ignoreCommonBuiltIns
    )

    return AnalysisContext(environment, facade, analysisEnvironment)
}

class DokkaMessageCollector(private val logger: DokkaLogger) : MessageCollector {
    override fun clear() {
        seenErrors = false
    }

    private var seenErrors = false

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        if (severity == CompilerMessageSeverity.ERROR) {
            seenErrors = true
        }
        logger.info(MessageRenderer.PLAIN_FULL_PATHS.render(severity, message, location))
    }

    override fun hasErrors() = seenErrors
}

// It is not data class due to ill-defined equals
class AnalysisContext(
    environment: KotlinCoreEnvironment,
    facade: DokkaResolutionFacade,
    private val analysisEnvironment: AnalysisEnvironment
) : Closeable {
    private var isClosed: Boolean = false
    val environment: KotlinCoreEnvironment = environment
        get() = field.takeUnless { isClosed } ?: throw IllegalStateException("AnalysisEnvironment is already closed")
    val facade: DokkaResolutionFacade = facade
        get() = field.takeUnless { isClosed } ?: throw IllegalStateException("AnalysisEnvironment is already closed")

    operator fun component1() = environment
    operator fun component2() = facade
    override fun close() {
        isClosed = true
        analysisEnvironment.dispose()
    }
}
