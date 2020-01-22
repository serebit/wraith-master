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

tasks.register("install") {
    dependsOn("package")
    doLast {
        val resourcesDir = file("${rootProject.buildDir}/package/resources")

        if (file("/sbin/udevadm").exists() && property("noudev") == null) {
            val udevDir = rootDir.resolve(properties["udevdir"] as? String ?: "/etc/udev")
                .resolve("rules.d")
                .apply { mkdirs() }

            resourcesDir.resolve("99-wraith-master.rules")
                .copyTo(udevDir.resolve("99-wraith-master.rules"), overwrite = true)
        }
    }
}
