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

        cinterops.create("gtk3")
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
