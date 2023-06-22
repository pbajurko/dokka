package org.jetbrains.dokka.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.jetbrains.dokka.DokkaVersion

internal val Project.dokkaArtifacts get() = DokkaArtifacts(this)

internal class DokkaArtifacts(private val project: Project) {
    private fun fromModuleName(name: String): Dependency =
        project.dependencies.create("com.github.pbajurko.dokka:$name:${DokkaVersion.version}")

    val dokkaAnalysis get() = fromModuleName("dokka-analysis")
    val allModulesPage get() = fromModuleName("all-modules-page-plugin")
    val dokkaCore get() = fromModuleName("dokka-core")
    val dokkaBase get() = fromModuleName("dokka-base")
    val javadocPlugin get() = fromModuleName("javadoc-plugin")
    val gfmPlugin get() = fromModuleName("gfm-plugin")
    val gfmTemplateProcessing get() = fromModuleName("gfm-template-processing-plugin")
    val jekyllTemplateProcessing get() = fromModuleName("jekyll-template-processing-plugin")
    val jekyllPlugin get() = fromModuleName("jekyll-plugin")
}
