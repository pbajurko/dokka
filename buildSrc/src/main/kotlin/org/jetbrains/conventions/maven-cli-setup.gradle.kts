package org.jetbrains.conventions

import org.gradle.kotlin.dsl.support.serviceOf

/**
 * Utility for downloading and installing a Maven binary.
 *
 * Provides the `setupMavenProperties` extension that contains the default versions and locations
 * of the Maven binary.
 *
 * The task [installMavenBinary] will download and unzip the Maven bianry.
 */

plugins {
    base
}

abstract class SetupMavenProperties {
    abstract val mavenVersion: Property<String>
    abstract val mavenPluginToolsVersion: Property<String>
    abstract val mavenBuildDir: DirectoryProperty

    /** Directory that will contain the unpacked Apache Maven dependency */
    abstract val mavenInstallDir: DirectoryProperty

    /**
     * Path to the Maven executable.
     *
     * This should be different per OS:
     *
     * * Windows: `$mavenInstallDir/bin/mvn.cmd`
     * * Unix: `$mavenInstallDir/bin/mvn`
     */
    abstract val mvn: RegularFileProperty
}

val setupMavenProperties =
    extensions.create("setupMavenProperties", SetupMavenProperties::class).apply {
        mavenVersion.convention(providers.gradleProperty("mavenVersion"))
        mavenPluginToolsVersion.convention(providers.gradleProperty("mavenPluginToolsVersion"))

        mavenBuildDir.convention(layout.buildDirectory.dir("maven"))
        mavenInstallDir.convention(layout.buildDirectory.dir("apache-maven"))

        val isWindowsProvider =
            providers.systemProperty("os.name").map { "win" in it.toLowerCase() }

        mvn.convention(
            providers.zip(mavenInstallDir, isWindowsProvider) { mavenInstallDir, isWindows ->
                mavenInstallDir.file(
                    when {
                        isWindows -> "bin/mvn.cmd"
                        else -> "bin/mvn"
                    }
                )
            }
        )
    }

val mavenBinary by configurations.registering {
    description = "used to download the Maven binary"
    isCanBeResolved = true
    isCanBeConsumed = false
    isVisible = false

    defaultDependencies {
        addLater(setupMavenProperties.mavenVersion.map { mavenVersion ->
            project.dependencies.create(
                group = "org.apache.maven",
                name = "apache-maven",
                version = mavenVersion,
                classifier = "bin",
                ext = "zip"
            )
        })
    }
}

tasks.clean {
    delete(setupMavenProperties.mavenBuildDir)
    delete(setupMavenProperties.mavenInstallDir)
}

val installMavenBinary by tasks.registering(Sync::class) {
    val archives = serviceOf<ArchiveOperations>()
    from(
        mavenBinary.flatMap { conf ->
            conf.incoming.artifacts.resolvedArtifacts.map { artifacts ->
                artifacts.map { archives.zipTree(it.file) }
            }
        }
    ) {
        eachFile {
            // drop the first directory inside the zipped Maven bin (apache-maven-$version)
            relativePath = RelativePath(true, *relativePath.segments.drop(1).toTypedArray())
        }
        includeEmptyDirs = false
    }
    into(setupMavenProperties.mavenInstallDir)
}