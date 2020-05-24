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

        val installMode = properties["installmode"] as? String
        val packageRoot = properties["packageroot"] as? String

        val installDirPath = properties["installdir"] as? String
            ?: "/usr".takeIf { installMode == "system" || packageRoot != null && installMode != "local" }
            ?: "/usr/local"

        val installDir = if (packageRoot != null) {
            file(packageRoot).resolve(installDirPath.removePrefix("/"))
        } else {
            file(installDirPath)
        }

        val forceUdev = (properties["forceudev"] as? String).let { it != null && it.isEmpty() }
        val noUdev = (properties["noudev"] as? String).let { it != null && it.isEmpty() }
        if (file("/sbin/udevadm").exists() && !noUdev || forceUdev) {
            val udevPath = properties["udevdir"] as? String
                ?: "/usr/lib/udev".takeIf { installMode == "system" || packageRoot != null && installMode != "local" }
                ?: "/etc/udev"

            val udevDir = if (packageRoot != null) {
                file(packageRoot).resolve(udevPath.removePrefix("/")).resolve("rules.d")
            } else {
                file(installDirPath).resolve("rules.d")
            }

            resourcesDir.resolve("99-wraith-master.rules")
                .copyTo(udevDir.resolve("99-wraith-master.rules"), overwrite = true)
        }

        resourcesDir.resolve("version.txt")
            .copyTo(installDir.resolve("share/wraith-master/version.txt"), overwrite = true)
    }
}
