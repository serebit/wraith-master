plugins {
    kotlin("multiplatform") version "1.3.61"
}

group = "com.serebit.wraith"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
}

kotlin {
    linuxX64 {
        compilations["main"].cinterops {
            create("libusb") { includeDirs("/usr/include") }
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
            entryPoint = "com.serebit.wraith.main"
        }
    }

    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
}
