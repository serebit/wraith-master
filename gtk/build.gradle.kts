plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64 {
    compilations["main"].apply {
        defaultSourceSet {
            dependencies { implementation(project(":core")) }
            languageSettings.useExperimentalAnnotation("kotlin.Experimental")
        }

        cinterops.create("gtk3") {
            val includePaths = listOf("/opt/local/include/", "/usr/include/", "/usr/local/include/")
            val headers = listOf("atk-1.0", "gdk-pixbuf-2.0", "cairo", "harfbuzz", "pango-1.0", "gtk-3.0", "glib-2.0")

            includePaths.associateWith { headers }.flatMap { (key, value) -> value.map { key + it } }.also {
                includeDirs(*it.toTypedArray())
            }
        }

        kotlinOptions.freeCompilerArgs = listOf("-Xallocator=mimalloc")

        binaries.executable { entryPoint = "com.serebit.wraith.gtk.main" }
    }
}

val `package` by tasks.registering {
    dependsOn(":core:package", "linkReleaseExecutableLinuxX64", "linuxX64ProcessResources")

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")
        val builtResourcesDir = buildDir.resolve("processedResources/linuxX64/main")
        val resourcesDir = packageDir.resolve("resources")

        val shouldStrip = properties["strip"].let { it is String && (it.isEmpty() || it == "true") }
        buildDir.resolve("bin/linuxX64/releaseExecutable/gtk.kexe")
            .copyTo(packageDir.resolve("wraith-master-gtk"), overwrite = true)
            .also { if (shouldStrip) exec { commandLine("strip", it.absolutePath) } }
            .setExecutable(true)

        builtResourcesDir.resolve("wraith-master.svg")
            .copyTo(resourcesDir.resolve("wraith-master.svg"), overwrite = true)

        builtResourcesDir.resolve("wraith-master.desktop")
            .copyTo(resourcesDir.resolve("wraith-master.desktop"), overwrite = true)
    }
}

tasks.register("install") {
    dependsOn(":core:install", `package`)

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")
        val resourcesDir = packageDir.resolve("resources")

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

        packageDir.resolve("wraith-master-gtk")
            .copyTo(installDir.resolve("bin/wraith-master-gtk"), overwrite = true)
            .also { exec { commandLine("chmod", "00755", it.absolutePath) } }

        resourcesDir.resolve("wraith-master.svg")
            .copyTo(installDir.resolve("share/icons/hicolor/scalable/apps/wraith-master.svg"), overwrite = true)

        resourcesDir.resolve("wraith-master.desktop")
            .copyTo(installDir.resolve("share/applications/wraith-master.desktop"), overwrite = true)
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
            commandLine("valgrind", *valgrindArgs, "./gtk.kexe", *programArgs)
        }
    }
}
