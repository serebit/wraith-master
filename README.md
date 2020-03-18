![Wraith Master][wraith-master-logo]

[![Pipeline Status][pipeline-status]](https://gitlab.com/serebit/wraith-master/commits/master)
[![License][license-badge]](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Ko-fi][kofi-badge]](https://ko-fi.com/serebit)

---

Wraith Master is a graphical and command-line application for controlling the RGB LEDs on AMD's Wraith stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add other Wraith coolers as well.

Designed for feature parity with the official Windows-only Cooler Master application, Wraith Master is intended to support all modes and settings that the Wraith Prism can use. Only one feature remains to be implemented (fan mirage color frequencies). As the Wraith coolers are capable of storing RGB configurations in-device, no daemon is required to maintain configurations, and the program can be uninstalled without fear of losing your settings.

## Supported Platforms

Wraith Master supports all 64-bit Linux distributions that use glibc as their libc implementation. This dependence on glibc is not my choice, but a limitation of the software stack; Kotlin/Native is [incompatible with musl](https://github.com/JetBrains/kotlin-native/issues/3771) as of the time I am writing this. Once it gains compatibility with more libc implementations, I fully intend to take advantage of that compatibility. I am open to bringing Wraith Master to Windows as well, if there is demand for it.

## Installing

There are precompiled binaries available for each release [here](https://gitlab.com/serebit/wraith-master/-/releases). Some Linux distributions have alternate installation methods, which are detailed below.

#### Arch/Derivatives

If you're on Arch Linux or one of its derivatives, you can install both frontends [from the AUR](https://aur.archlinux.org/packages/?K=wraith%2Dmaster), under `wraith-master-cli` for the CLI frontend and `wraith-master-gtk` for the GTK frontend. 

#### Solus

Solus has both frontends in the official repository. They can be installed either by running `sudo eopkg it wraith-master` in the terminal, or searching for the package in the Software Center.

## Screenshots

![Screenshot][wraith-master-screenshot]

## Building from Source

### Build Dependencies

In addition to a Java Developer Kit (JDK) of version 8 or newer, Wraith Master requires the following packages to build:
 
| Distribution       | Shared Dependencies                               | GTK-Only         |
|--------------------|---------------------------------------------------|------------------|
| Debian/Derivatives | `libusb-1.0-0-dev`, `gcc-multilib`, `libncurses5` | `libgtk-3-dev`   |
| Arch/Derivatives   | `ncurses5-compat-libs` (AUR)                      | `gtk3`           |
| Fedora             | `libusbx-devel`, `ncurses-compat-libs`            | `gtk3-devel`     |
| OpenSUSE           | `libusb-1_0-devel`, `libncurses5`                 | `gtk3-devel`     |
| Solus              | `libusb-devel`                                    | `libgtk-3-devel` |
| Gentoo             | `dev-libs/libusb`, `ncurses-compat`               | `gtk+`           |

You'll need the packages in Shared Dependencies to build Wraith Master, but if you're only building the command-line frontend, you don't need the packages in GTK-Only.

### Instructions

Each Gradle command that follows can be run with the subproject name as a prefix in order to only build and install specific artifacts. For instance, `:cli:package` would build the command line artifact, and `:gtk:package` would build the GTK artifact.

To build all artifacts and place them in the `build/package` directory, run the following:

```bash
./gradlew package
```

To install the built artifacts and their associated resources, run the following:

```bash
./gradlew install
```

This will install the packages in `/usr/local` by default. To change the installation directory, pass a parameter to the above task in the format `-Pinstalldir="/your/install/dir"`. You can also change the directory that the udev rules will be installed in by passing the parameter `-Pudevdir="/your/udev/dir"` (the rules are installed to `/etc/udev` by default). You can also disable the installation of udev rules entirely by passing the parameter `-Pnoudev`, although this argument is automatically applied on Linux distributions that don't use udev.

## Runtime Dependencies

| Distribution       | Shared Dependencies           | GTK-Only                |
|--------------------|-------------------------------|-------------------------|
| Debian/Derivatives | `libusb-1.0-0`, `libncurses5` | `glib2.0`, `libgtk-3.0` |
| Arch/Derivatives   | None                          | `gtk3`                  |
| Fedora             | None                          | `gtk3`                  |
| OpenSUSE           | None                          | `glib2`, `gtk3`         |
| Solus              | None                          | None                    |
| Gentoo             | `dev-libs/libusb`             | Untested                |

## Changelog

See `CHANGELOG.md` for notes on previous releases, along with changes that are currently in staging for the next release.

## License

Wraith Master is open-sourced under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

## Acknowledgements

- **gfduszynski**, for his work on [cm-rgb](https://github.com/gfduszynski/cm-rgb). Although I started Wraith Master before discovering cm-rgb, gfduszynski's work made it viable.
- **Adam Honse**, for his work on [OpenRGB](https://gitlab.com/CalcProgrammer1/OpenRGB). This had some extra documentation on specific functions that I was lacking.
- **ballsystemlord**, **Kirk**, and **tralamazza** from the [AdoredTV](https://adoredtv.com/) Discord server, along with **[Apache](https://github.com/Apache-HB)** and **my dad**, for helping me figure out how the mirage frequencies are converted to byte values.
- **Cooler Master**, for manufacturing these great stock coolers, and forwarding me to AMD when I inquired about how the Wraith Prism's USB interface worked.
- **AMD**, for telling me I'd need to sign an NDA to get that information. Sorry, but that's not really my style. Thanks for the great product though!

[wraith-master-logo]: https://serebit.com/images/wraith-master-banner-nopad.svg "Wraith Master"
[pipeline-status]: https://gitlab.com/serebit/wraith-master/badges/master/pipeline.svg "Pipeline Status"
[license-badge]: https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg "License"
[kofi-badge]: https://img.shields.io/badge/-ko--fi-ff5f5f?logo=ko-fi&logoColor=white "Ko-fi"
[wraith-master-screenshot]: https://serebit.com/images/wraith-master-screenshot.png "Screenshot"
