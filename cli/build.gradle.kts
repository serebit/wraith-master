plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64 {
    compilations["main"].apply {
        defaultSourceSet {
            dependencies { implementation(project(":core")) }
            languageSettings.useExperimentalAnnotation("kotlin.Experimental")
        }
        enableEndorsedLibs = true
        kotlinOptions.freeCompilerArgs = listOf("-Xallocator=mimalloc")
    }

    binaries.executable { entryPoint = "com.serebit.wraith.cli.main" }
}

tasks.register("package") {
    dependsOn(":core:package", "linkReleaseExecutableLinuxX64", "linuxX64ProcessResources")

    doLast {
        val shouldStrip = properties["strip"] !in listOf(null, "false")
        buildDir.resolve("bin/linuxX64/releaseExecutable/cli.kexe")
            .copyTo(rootProject.buildDir.resolve("package/wraith-master"), overwrite = true)
            .also { if (shouldStrip) exec { commandLine("strip", it.absolutePath) } }
            .setExecutable(true)
    }
}

tasks.register("install") {
    dependsOn(":core:install", "package")

    doLast {
        val installMode = properties["installmode"] as? String
        val packageRoot = properties["packageroot"] as? String

        val installDirPath = properties["installdir"] as? String
            ?: "/usr".takeIf { installMode == "system" || packageRoot != null && installMode != "local" }
            ?: "/usr/local"

        val installDir = if (packageRoot != null) {
            file(packageRoot).resolve(installDirPath.removePrefix("/"))
        } else {
            file(installDirPath)
        }

        rootProject.buildDir.resolve("package/wraith-master")
            .copyTo(installDir.resolve("bin/wraith-master"), overwrite = true)
            .also { exec { commandLine("chmod", "00755", it.absolutePath) } }
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
