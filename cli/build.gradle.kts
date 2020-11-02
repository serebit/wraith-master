plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64 {
    compilations["main"].apply {
        defaultSourceSet {
            dependencies { implementation(project(":core")) }
            languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
        }
        enableEndorsedLibs = true
    }

    binaries.executable { entryPoint = "com.serebit.wraith.cli.main" }
}

val `package` by tasks.registering {
    dependsOn(":core:package", "linkReleaseExecutableLinuxX64", "linuxX64ProcessResources")

    doLast {
        val shouldStrip = properties["strip"] !in listOf(null, "false")
        val useGcompat = properties["usegcompat"].let { it is String && (it.isEmpty() || it == "true") }
        val manPages = properties["manpages"].let { it is String && (it.isEmpty() || it == "true") }

        buildDir.resolve("bin/linuxX64/releaseExecutable/cli.kexe")
            .copyTo(rootProject.buildDir.resolve("package/wraith-master"), overwrite = true)
            .also {
                if (shouldStrip) exec {
                    commandLine("strip", it.absolutePath)
                }
                if (useGcompat) {
                    exec { commandLine("patchelf", "--remove-needed", "libcrypt.so.1", it.absolutePath) }
                    exec { commandLine("patchelf", "--remove-needed", "libresolv.so.2", it.absolutePath) }
                    exec { commandLine("patchelf", "--add-needed", "libgcompat.so.0", it.absolutePath) }
                }
            }.setExecutable(true)

        if (manPages) {
            val scdPath = projectDir.resolve("resources/wraith-master.1.scd").path
            val manPath = rootProject.buildDir.resolve("package/resources/wraith-master.1").path
            exec {
                commandLine("sh", "-c", "scdoc < $scdPath > $manPath")
            }
        }
    }
}

tasks.register("install") {
    dependsOn(":core:install", `package`)

    doLast {
        val installMode = properties["installmode"] as? String
        val packageRoot = properties["packageroot"] as? String ?: rootProject.extra.properties["packageroot"] as? String

        val installDirPath = properties["installdir"] as? String
            ?: "/usr".takeIf { installMode == "system" || packageRoot != null && installMode != "local" }
            ?: "/usr/local"

        val installDir = if (packageRoot != null) {
            file(packageRoot).resolve(installDirPath.removePrefix("/"))
        } else {
            file(installDirPath)
        }

        val packageDir = rootProject.buildDir.resolve("package")

        packageDir.resolve("wraith-master")
            .copyTo(installDir.resolve("bin/wraith-master"), overwrite = true)
            .also { exec { commandLine("chmod", "00755", it.absolutePath) } }

        packageDir.resolve("resources/wraith-master.1")
            .takeIf { it.exists() }
            ?.copyTo(installDir.resolve("man/man1/wraith-master.1"), overwrite = true)
    }
}

tasks.register("valgrind") {
    dependsOn("linkDebugExecutableLinuxX64")

    doLast {
        exec {
            isIgnoreExitValue = true
            workingDir = buildDir.resolve("bin/linuxX64/debugExecutable")
            val programArgs = properties["cliargs"]?.toString()?.split(" ")?.toTypedArray() ?: emptyArray()
            val valgrindArgs = properties["valargs"]?.toString()?.split(" ")?.toTypedArray() ?: emptyArray()
            commandLine("valgrind", *valgrindArgs, "./cli.kexe", *programArgs)
        }
    }
}
