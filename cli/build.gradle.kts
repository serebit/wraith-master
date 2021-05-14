plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64 {
    compilations["main"].apply {
        defaultSourceSet.dependencies {
            implementation(project(":core"))
        }
        enableEndorsedLibs = true
    }

    binaries.executable {
        entryPoint = "com.serebit.wraith.cli.main"
        linkerOpts("--as-needed", "-z", "now", "-z", "relro")
        freeCompilerArgs = freeCompilerArgs + listOf("-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc")
    }
}

val `package` by tasks.registering {
    dependsOn(":core:package", "linkReleaseExecutableLinuxX64", "linuxX64ProcessResources")

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")

        val shouldStrip = properties["strip"].let { it is String && (it.isEmpty() || it == "true") }
        val disableManPages = properties["disable-man-pages"].let { it is String && (it.isEmpty() || it == "true") }

        buildDir.resolve("bin/linuxX64/releaseExecutable/cli.kexe")
            .copyTo(packageDir.resolve("wraith-master"), overwrite = true)
            .also {
                if (shouldStrip) exec {
                    commandLine("strip", it.absolutePath)
                }
                it.setExecutable(true, false)
            }

        if (!disableManPages) {
            val scdPath = projectDir.resolve("resources/wraith-master.1.scd").path
            val manPath = packageDir.resolve("resources/wraith-master.1").path
            exec { commandLine("sh", "-c", "scdoc < $scdPath > $manPath") }
        }
    }
}

val prepareInstall by tasks.registering {
    dependsOn(`package`)

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")
        val resourcesDir = packageDir.resolve("resources")
        val destDir = buildDir.resolve("preparedInstall")
        val disableManPages = properties["disable-man-pages"].let { it is String && (it.isEmpty() || it == "true") }

        packageDir.resolve("wraith-master")
            .copyTo(destDir.resolve("bin/wraith-master"), overwrite = true)
            .also { it.setExecutable(true, false) }

        if (!disableManPages) {
            resourcesDir.resolve("wraith-master.1")
                .takeIf { it.exists() }
                ?.copyTo(destDir.resolve("share/man/man1/wraith-master.1"), overwrite = true)
        }
    }
}

tasks.register<Copy>("install") {
    dependsOn(":core:install", prepareInstall)

    from(buildDir.resolve("preparedInstall"))
    destinationDir = file(properties["prefix"] ?: "/usr/local")
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
