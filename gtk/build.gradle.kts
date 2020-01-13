plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64 {
        compilations["main"].apply {
            defaultSourceSet.dependencies {
                implementation(project(":core"))
            }

            cinterops {
                create("gtk3") {
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
            }
            binaries.executable {
                entryPoint = "com.serebit.wraith.gtk.main"
            }
        }
    }

    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
}

tasks.register("package") {
    dependsOn("build")
    doLast {
        val packageDir = file("${rootProject.buildDir}/package").apply { mkdirs() }
        val resourcesDir = packageDir.resolve("resources").apply { mkdirs() }
        file("$buildDir/bin/linuxX64/releaseExecutable/gtk.kexe")
            .copyTo(packageDir.resolve("wraith-master-gtk"), overwrite = true)
            .setExecutable(true)
        file("$buildDir/processedResources/linuxX64/main/wraith-master.svg")
            .copyTo(resourcesDir.resolve("wraith-master.svg"), overwrite = true)
        file("$buildDir/processedResources/linuxX64/main/wraith-master.desktop")
            .copyTo(resourcesDir.resolve("wraith-master.desktop"), overwrite = true)
    }
}

tasks.register("install") {
    dependsOn("package")
    doLast {
        val packageDir = file("${rootProject.buildDir}/package")
        val resourcesDir = packageDir.resolve("resources")
        val installDir = file(properties["installdir"] ?: "/usr/local")
        val binDir = installDir.resolve("bin").apply { mkdirs() }
        val iconDir = installDir.resolve("share/icons/hicolor/scalable/apps").apply { mkdirs() }
        val appsDir = installDir.resolve("share/applications").apply { mkdirs() }

        packageDir.resolve("wraith-master-gtk")
            .copyTo(binDir.resolve("wraith-master-gtk"), overwrite = true)
            .also { exec { commandLine("chmod", "00755", it.absolutePath) } }
        resourcesDir.resolve("wraith-master.svg").copyTo(iconDir.resolve("wraith-master.svg"), overwrite = true)
        resourcesDir.resolve("wraith-master.desktop").copyTo(appsDir.resolve("wraith-master.desktop"), overwrite = true)
    }
}
