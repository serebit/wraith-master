plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64 {
        compilations["main"].apply {
            defaultSourceSet.dependencies {
                implementation(project(":core"))
            }
            enableEndorsedLibs = true
        }

        binaries.executable {
            entryPoint = "com.serebit.wraith.cli.main"
        }
    }

    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
}

tasks.register("package") {
    dependsOn("build")
    dependsOn(":core:package")
    doLast {
        val packageDir = file("${rootProject.buildDir}/package").apply { mkdirs() }
        file("$buildDir/bin/linuxX64/releaseExecutable/cli.kexe")
            .copyTo(packageDir.resolve("wraith-master"), overwrite = true)
            .setExecutable(true)
    }
}

tasks.register("install") {
    dependsOn("package")
    dependsOn(":core:package")
    doLast {
        val packageDir = file("${rootProject.buildDir}/package")
        val installDir = rootDir.resolve(properties["installdir"] as? String ?: "/usr/local")

        val binDir = installDir.resolve("bin").apply { mkdirs() }

        packageDir.resolve("wraith-master")
            .copyTo(binDir.resolve("wraith-master"), overwrite = true)
            .also { exec { commandLine("chmod", "00755", it.absolutePath) } }
    }
}
