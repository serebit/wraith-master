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
        file("$buildDir/bin/linuxX64/releaseExecutable/gtk.kexe")
            .copyTo(packageDir.resolve("wraith-master-gtk"), overwrite = true)
            .setExecutable(true)
    }
}
