plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64 {
    binaries.executable {
        entryPoint = "com.serebit.wraith.gtk.main"
        linkerOpts("--as-needed")
        freeCompilerArgs = freeCompilerArgs + listOf("-Xoverride-konan-properties=linkerGccFlags=-lgcc -lgcc_eh -lc")
    }

    compilations["main"].apply {
        defaultSourceSet.dependencies {
            implementation(project(":core"))
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
