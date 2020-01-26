plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64 {
    compilations["main"].apply {
        defaultSourceSet {
            dependencies { implementation(project(":core")) }
            defaultSourceSet.languageSettings.useExperimentalAnnotation("kotlin.Experimental")
        }

        cinterops.create("gtk3") {
            listOf("/opt/local/include", "/usr/include", "/usr/local/include").forEach {
                includeDirs(
                    "$it/atk-1.0",
                    "$it/gdk-pixbuf-2.0",
                    "$it/cairo",
                    "$it/harfbuzz",
                    "$it/pango-1.0",
                    "$it/gtk-3.0",
                    "$it/glib-2.0"
                )
            }

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
    dependsOn("build")
    dependsOn(":core:package")

    doLast {
        val packageDir = rootProject.buildDir.resolve("package")
        val builtResourcesDir = buildDir.resolve("processedResources/linuxX64/main")
        val resourcesDir = packageDir.resolve("resources")

        buildDir.resolve("bin/linuxX64/releaseExecutable/gtk.kexe")
            .copyTo(packageDir.resolve("wraith-master-gtk"), overwrite = true)
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
        val installDir = rootDir.resolve(properties["installdir"] as? String ?: "/usr/local")

        packageDir.resolve("wraith-master-gtk")
            .copyTo(installDir.resolve("bin/wraith-master-gtk"), overwrite = true)
            .also { exec { commandLine("chmod", "00755", it.absolutePath) } }

        resourcesDir.resolve("wraith-master.svg")
            .copyTo(installDir.resolve("share/icons/hicolor/scalable/apps/wraith-master.svg"), overwrite = true)

        resourcesDir.resolve("wraith-master.desktop")
            .copyTo(installDir.resolve("share/applications/wraith-master.desktop"), overwrite = true)
    }
}
