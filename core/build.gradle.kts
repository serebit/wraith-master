plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64().compilations["main"].apply {
    defaultSourceSet.languageSettings.useExperimentalAnnotation("kotlin.Experimental")

    cinterops.create("libusb") {
        includeDirs("/opt/local/include", "/usr/include", "/usr/local/include")
    }
}

tasks.register("package") {
    dependsOn("build")

    doLast {
        val resourcesDir = rootProject.buildDir.resolve("package/resources")

        buildDir.resolve("processedResources/linuxX64/main/99-wraith-master.rules")
            .copyTo(resourcesDir.resolve("99-wraith-master.rules"), overwrite = true)

        resourcesDir.resolve("version.txt").writeText(version.toString())
    }
}

tasks.register("install") {
    dependsOn("package")

    doLast {
        val resourcesDir = rootProject.buildDir.resolve("package/resources")
        val installDir = file(properties["installdir"] as? String ?: "/usr/local")

        if (file("/sbin/udevadm").exists() && properties["noudev"] == null) {
            val udevDir = file(properties["udevdir"] as? String ?: "/etc/udev").resolve("rules.d")

            resourcesDir.resolve("99-wraith-master.rules")
                .copyTo(udevDir.resolve("99-wraith-master.rules"), overwrite = true)
        }

        resourcesDir.resolve("version.txt")
            .copyTo(installDir.resolve("share/wraith-master/version.txt"), overwrite = true)
    }
}
