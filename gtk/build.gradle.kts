plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64 {
    binaries.executable { entryPoint = "com.serebit.wraith.gtk.main" }

    compilations["main"].apply {
        defaultSourceSet {
            dependencies { implementation(project(":core")) }
            languageSettings.apply {
                useExperimentalAnnotation("kotlin.RequiresOptIn")
                useExperimentalAnnotation("kotlin.ExperimentalUnsignedTypes")
            }
        }

        cinterops.create("gtk3") {
            val includePaths = listOf("/opt/local/include/", "/usr/include/", "/usr/local/include/")
            val headers = listOf("atk-1.0", "gdk-pixbuf-2.0", "cairo", "harfbuzz", "pango-1.0", "gtk-3.0", "glib-2.0")

            includePaths.associateWith { headers }.flatMap { (key, value) -> value.map { key + it } }.also {
                includeDirs(*it.toTypedArray())
            }
        }
    }
}

val `package` by tasks.registering {
    dependsOn(":core:package", "linkReleaseExecutableLinuxX64", "linuxX64ProcessResources")

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")
        val packageResourcesDir = packageDir.resolve("resources")
        val resourcesDir = projectDir.resolve("resources")

        val shouldStrip = properties["strip"].let { it is String && (it.isEmpty() || it == "true") }
        val useGcompat = properties["enable-gcompat"].let { it is String && (it.isEmpty() || it == "true") }
        val disableManPages = properties["disable-man-pages"].let { it is String && (it.isEmpty() || it == "true") }

        buildDir.resolve("bin/linuxX64/releaseExecutable/gtk.kexe")
            .copyTo(packageDir.resolve("wraith-master-gtk"), overwrite = true)
            .also {
                if (shouldStrip) exec {
                    commandLine("strip", it.absolutePath)
                }
                if (useGcompat) {
                    exec { commandLine("patchelf", "--remove-needed", "libcrypt.so.1", it.absolutePath) }
                    exec { commandLine("patchelf", "--remove-needed", "libresolv.so.2", it.absolutePath) }
                    exec { commandLine("patchelf", "--add-needed", "libgcompat.so.0", it.absolutePath) }
                }
                it.setExecutable(true)
            }

        if (!disableManPages) {
            val scdPath = projectDir.resolve("resources/wraith-master-gtk.1.scd").path
            val manPath = packageResourcesDir.resolve("wraith-master-gtk.1").path
            exec { commandLine("sh", "-c", "scdoc < $scdPath > $manPath") }
        }

        resourcesDir.resolve("wraith-master.svg")
            .copyTo(packageResourcesDir.resolve("wraith-master.svg"), overwrite = true)

        resourcesDir.resolve("wraith-master.desktop")
            .copyTo(packageResourcesDir.resolve("wraith-master.desktop"), overwrite = true)
    }
}

val prepareInstall by tasks.registering {
    dependsOn(`package`)

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")
        val resourcesDir = packageDir.resolve("resources")
        val destDir = buildDir.resolve("preparedInstall")
        val disableManPages = properties["disable-man-pages"].let { it is String && (it.isEmpty() || it == "true") }

        packageDir.resolve("wraith-master-gtk")
            .copyTo(destDir.resolve("bin/wraith-master-gtk"), overwrite = true)
            .also { it.setExecutable(true) }

        resourcesDir.resolve("wraith-master.svg")
            .copyTo(destDir.resolve("share/icons/hicolor/scalable/apps/wraith-master.svg"), overwrite = true)

        resourcesDir.resolve("wraith-master.desktop")
            .copyTo(destDir.resolve("share/applications/wraith-master.desktop"), overwrite = true)


        if (!disableManPages) {
            resourcesDir.resolve("wraith-master-gtk.1")
                .takeIf { it.exists() }
                ?.copyTo(destDir.resolve("share/man/man1/wraith-master-gtk.1"))
        }
    }
}

tasks.register("install") {
    dependsOn(":core:install", prepareInstall)

    doLast {
        val prefix = file(properties["prefix"] ?: "/usr/local")
        buildDir.resolve("preparedInstall").copyRecursively(prefix, overwrite = true)
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
