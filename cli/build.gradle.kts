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
    doLast {
        val packageDir = file("${rootProject.buildDir}/package").apply { mkdirs() }
        file("$buildDir/bin/linuxX64/releaseExecutable/cli.kexe")
            .copyTo(packageDir.resolve("wraith-master"), overwrite = true)
            .setExecutable(true)
    }
}