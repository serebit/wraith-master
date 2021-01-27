plugins {
    kotlin("multiplatform") version "1.4.30-RC" apply false
    id("com.github.ben-manes.versions") version "0.36.0"
    base // to add clean task, for cleaning up package output
}

allprojects {
    group = "com.serebit.wraith"
    version = "1.2.0-dev"

    repositories {
        mavenCentral()
    }
}

val releaseTar by tasks.registering {
    dependsOn(":cli:package", ":gtk:package")

    doLast {
        val tarballName = "wraith-master-$version.tar.xz"

        temporaryDir.resolve("wraith-master-$version").also { tempDir ->
            buildDir.resolve("package").copyRecursively(tempDir, true)

            tempDir.resolve("wraith-master").setExecutable(true)
            tempDir.resolve("wraith-master-gtk").setExecutable(true)
            exec { workingDir = temporaryDir; commandLine("tar", "-cJf", tarballName, tempDir.name) }
        }

        temporaryDir.resolve(tarballName).copyTo(buildDir.resolve("dist/$tarballName"), true)
        temporaryDir.deleteRecursively()
    }
}

val releaseDeb by tasks.registering {
    dependsOn(":core:prepareInstall", ":cli:prepareInstall", ":gtk:prepareInstall")

    doLast {
        val workDir = temporaryDir.resolve("wraith-master")

        subprojects {
            buildDir.resolve("preparedInstall").copyRecursively(workDir.resolve("usr"), overwrite = true)
        }

        projectDir.resolve("resources/debian-control.txt")
            .copyTo(temporaryDir.resolve("wraith-master/DEBIAN/control"))
            .also { it.writeText(it.readText().replace("%%VERSION%%", version.toString())) }

        exec {
            workingDir = temporaryDir
            commandLine("dpkg-deb", "-b", "wraith-master")
        }

        temporaryDir.resolve("wraith-master.deb")
            .copyTo(buildDir.resolve("dist/wraith-master-$version.deb"), overwrite = true)

        temporaryDir.deleteRecursively()
    }
}

val releaseRpm by tasks.registering {
    dependsOn(":core:prepareInstall", ":cli:prepareInstall", ":gtk:prepareInstall")

    doLast {
        val sanitizedVersion = version.toString().replace("-", "_")
        val workDir = temporaryDir.resolve("INSTALL/wraith-master-$sanitizedVersion")

        subprojects {
            buildDir.resolve("preparedInstall").copyRecursively(workDir.resolve("usr"), overwrite = true)
        }

        projectDir.resolve("resources/fedora.spec")
            .copyTo(temporaryDir.resolve("SPECS/wraith-master.spec"), overwrite = true)
            .apply { readText().replace("%%VERSION%%", sanitizedVersion).also { writeText(it) } }

        exec {
            workingDir = temporaryDir.resolve("INSTALL")
            val destTar = temporaryDir.resolve("SOURCES")
                .also { it.mkdirs() }
                .resolve("wraith-master-$sanitizedVersion.tar")
            commandLine("tar", "-cf", destTar.absolutePath, *workingDir.list())
        }

        exec {
            workingDir = temporaryDir.resolve("SPECS")
            commandLine("sh", "-c", "rpmbuild --define \"_topdir $temporaryDir\" -ba wraith-master.spec")
        }

        temporaryDir.resolve("RPMS/x86_64").listFiles()!!.single()
            .copyTo(buildDir.resolve("dist/wraith-master-$version.rpm"), overwrite = true)
        temporaryDir.deleteRecursively()
    }
}

tasks.register("release") {
    dependsOn(releaseTar, releaseDeb, releaseRpm)
}
