plugins {
    kotlin("multiplatform")
}

kotlin.linuxX64().compilations["main"].apply {
    cinterops.create("libusb") {
        includeDirs("/opt/local/include", "/usr/include", "/usr/local/include")
    }
}

projectDir.resolve("src/commonMain/kotlin").also { commonDir ->
    val stubText = commonDir.resolve("Version.ktstub").readText()
    commonDir.resolve("Version.kt").writeText(stubText.replace("%%VERSION%%", version.toString()))
}
