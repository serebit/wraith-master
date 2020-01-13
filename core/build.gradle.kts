import java.io.ByteArrayOutputStream

plugins {
    kotlin("multiplatform")
}

kotlin {
    linuxX64().compilations["main"].cinterops {
        create("libusb") { includeDirs("/usr/include") }
    }

    sourceSets.all {
        languageSettings.useExperimentalAnnotation("kotlin.Experimental")
    }
}

tasks.register("package") {
    dependsOn("build")
    doLast {
        val resourcesDir = file("${rootProject.buildDir}/package/resources").apply { mkdirs() }
        file("$buildDir/processedResources/linuxX64/main/99-wraith-master.rules")
            .copyTo(resourcesDir.resolve("99-wraith-master.rules"), overwrite = true)
    }
}

val usesSystemd: Boolean
    get() = ByteArrayOutputStream().use {
        exec {
            commandLine("ps", "--no-headers", "-o", "comm", "1")
            standardOutput = it
        }
        it.toString().trim() == "systemd"
    }

tasks.register("install") {
    dependsOn("package")
    doLast {
        val resourcesDir = file("${rootProject.buildDir}/package/resources")

        if (usesSystemd) {
            val udevDir = rootDir.resolve(properties["udevdir"] as? String ?: "/etc/udev")
                .resolve("rules.d")
                .apply { mkdirs() }

            resourcesDir.resolve("99-wraith-master.rules")
                .copyTo(udevDir.resolve("99-wraith-master.rules"), overwrite = true)
        }
    }
}
