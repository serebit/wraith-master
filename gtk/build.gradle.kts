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

            // extra includes
            includeDirs(
                "/opt/local/lib/glib-2.0/include",
                "/usr/lib/x86_64-linux-gnu/glib-2.0/include",
                "/usr/local/lib/glib-2.0/include"
            )
        }
        binaries.executable { entryPoint = "com.serebit.wraith.gtk.main" }
    }
}

tasks.register("package") {
    dependsOn("linkReleaseExecutableLinuxX64")
    dependsOn("linuxX64ProcessResources")
    dependsOn(":core:package")

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")
        val builtResourcesDir = buildDir.resolve("processedResources/linuxX64/main")
        val resourcesDir = packageDir.resolve("resources")

        val shouldStrip = properties["strip"] !in listOf(null, "false")
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
    dependsOn("package")
    dependsOn(":core:install")

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")
        val resourcesDir = packageDir.resolve("resources")
        val installDir = file(properties["installdir"] as? String ?: "/usr/local")

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
            val programArgs = properties["cliargs"].toString().split(" ").toTypedArray()
            val valgrindArgs = properties["valargs"].toString().split(" ").toTypedArray()
            commandLine("valgrind", *valgrindArgs, "./gtk.kexe", *programArgs)
        }
    }
}
