plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64().compilations["main"].apply {
    cinterops.create("libusb") {
        includeDirs("/opt/local/include", "/usr/include", "/usr/local/include")
    }
}

val `package` by tasks.registering {
    dependsOn("build")

    doLast {
        projectDir.resolve("resources/99-wraith-master.rules")
            .copyTo(rootProject.buildDir.resolve("package/resources/99-wraith-master.rules"), overwrite = true)
    }
}

val prepareInstall by tasks.registering {
    doLast {
        val resourcesDir = projectDir.resolve("resources")
        val destDir = buildDir.resolve("preparedInstall")
        val noUdev = properties["disable-udev"].let { it is String && (it.isEmpty() || it == "true") }

        if (!noUdev) {
            resourcesDir.resolve("99-wraith-master.rules")
                .copyTo(destDir.resolve("lib/udev/rules.d/99-wraith-master.rules"), overwrite = true)
        }
    }
}

tasks.register<Copy>("install") {
    dependsOn(prepareInstall)

    from(buildDir.resolve("preparedInstall"))
    destinationDir = file(properties["prefix"] ?: "/usr/local")
}

projectDir.resolve("src/commonMain/kotlin").also { commonDir ->
    val stubText = commonDir.resolve("Version.ktstub").readText()
    commonDir.resolve("Version.kt").writeText(stubText.replace("%%VERSION%%", version.toString()))
}
