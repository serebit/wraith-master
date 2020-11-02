plugins {
    kotlin("multiplatform") version "1.4.10" apply false
    id("com.github.ben-manes.versions") version "0.33.0"
    base // to add clean task, for cleaning up package output
}

allprojects {
    group = "com.serebit.wraith"
    version = "1.2.0-dev"

    repositories {
        mavenCentral()
    }
}

tasks.register("distTar") {
    dependsOn(":cli:package", ":gtk:package")

    doLast {
        val tarballName = "wraith-master-v$version.tar.xz"

        temporaryDir.resolve("wraith-master").also { tempDir ->
            buildDir.resolve("package").copyRecursively(tempDir, true)

            tempDir.resolve("wraith-master").setExecutable(true)
            tempDir.resolve("wraith-master-gtk").setExecutable(true)
            exec { workingDir = temporaryDir; commandLine("tar", "-cf", tarballName, tempDir.name) }
        }

        temporaryDir.resolve(tarballName).copyTo(buildDir.resolve("dist/$tarballName"), true)
        temporaryDir.deleteRecursively()
    }
}

tasks.register("distDeb") {
    dependsOn(":cli:install", ":gtk:install")
    rootProject.extra["packageroot"] = buildDir.resolve("debian/wraith-master").absolutePath

    doLast {
        projectDir.resolve("resources/debian-control.txt")
            .copyTo(buildDir.resolve("debian/wraith-master-v$version/DEBIAN/control"))
            .also { it.writeText(it.readText().replace("%%VERSION%%", version.toString())) }

        exec {
            workingDir = buildDir.resolve("debian")
            commandLine("dpkg-deb", "-b", "wraith-master-v$version")
        }
    }
}
