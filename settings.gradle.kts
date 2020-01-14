rootProject.name = "wraith-master"

include(":core", ":gtk", ":cli")

pluginManagement.repositories {
    gradlePluginPortal()
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}
