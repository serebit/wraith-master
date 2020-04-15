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
    }

    binaries.executable { entryPoint = "com.serebit.wraith.cli.main" }
}

tasks.register("package") {
    dependsOn("build")
    dependsOn(":core:package")

    doLast {
        val shouldStrip = properties["strip"] !in listOf(null, "false")
        buildDir.resolve("bin/linuxX64/releaseExecutable/cli.kexe")
            .copyTo(rootProject.buildDir.resolve("package/wraith-master"), overwrite = true)
            .also { if (shouldStrip) exec { commandLine("strip", it.absolutePath) } }
            .setExecutable(true)
    }
}

tasks.register("install") {
    dependsOn("package")
    dependsOn(":core:package")

    doLast {
        val installDir = file(properties["installdir"] as? String ?: "/usr/local")

        rootProject.buildDir.resolve("package/wraith-master")
            .copyTo(installDir.resolve("bin/wraith-master"), overwrite = true)
            .also { exec { commandLine("chmod", "00755", it.absolutePath) } }
    }
}

tasks.register("valgrind") {
    dependsOn("build")

    doLast {
        exec {
            isIgnoreExitValue = true
            workingDir = buildDir.resolve("bin/linuxX64/debugExecutable")
            val programArgs = properties["cliargs"].toString().split(" ").toTypedArray()
            val valgrindArgs = properties["valargs"].toString().split(" ").toTypedArray()
            commandLine("valgrind", *valgrindArgs, "./cli.kexe", *programArgs)
        }
    }
}
