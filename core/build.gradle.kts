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
        buildDir.resolve("processedResources/linuxX64/main/99-wraith-master.rules")
            .copyTo(rootProject.buildDir.resolve("package/resources/99-wraith-master.rules"), overwrite = true)
    }
}

tasks.register("install") {
    dependsOn("package")

    doLast {
        if (file("/sbin/udevadm").exists() && properties["noudev"] == null) {
            val udevDir = rootDir.resolve(properties["udevdir"] as? String ?: "/etc/udev").resolve("rules.d")

            rootProject.buildDir.resolve("package/resources/99-wraith-master.rules")
                .copyTo(udevDir.resolve("99-wraith-master.rules"), overwrite = true)
        }
    }
}
