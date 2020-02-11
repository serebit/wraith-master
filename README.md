![Wraith Master][wraith-master-logo]

[![pipeline status][pipeline-status]](https://gitlab.com/serebit/wraith-master/commits/master)
[![License][license-badge]](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Ko-fi][kofi-badge]](https://ko-fi.com/serebit)

---

Wraith Master is a graphical and command-line application for controlling the RGB LEDs on AMD's Wraith stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add other Wraith coolers as well.

## Download

There are precompiled binaries available for each release [here](https://gitlab.com/serebit/wraith-master/-/releases).

If you're on Arch Linux or one of its derivatives, you can also install both frontends from the AUR, under `wraith-master-cli` for the CLI frontend and `wraith-master-gtk` for the GTK frontend. 

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

### Instructions

Each Gradle command that follows can be run with the subproject name as a prefix in order to only build and install specific artifacts. For instance, `:cli:package` would build the command line artifact, and `:gtk:package` would build the GTK artifact.

To build all artifacts and place them in the `build/package` directory, run the following:

```bash
./gradlew package
```

To install the built artifacts, run the following:

```bash
./gradlew install
```

This will install the packages in `/usr/local` by default. To change the installation directory, pass a parameter to the above task in the format `-Pinstalldir="/your/install/dir"`.

## Runtime Dependencies

| Distribution       | Shared Dependencies           | GTK-Only        |
|--------------------|-------------------------------|-----------------|
| Debian/Derivatives | `libusb-1.0-0`, `libncurses5` | Untested        |
| Arch/Derivatives   | None                          | `gtk3`          |
| Fedora             | None                          | `gtk3`          |
| OpenSUSE           | None                          | `glib2`, `gtk3` |
| Solus              | None                          | None            |
| Gentoo             | `dev-libs/libusb`             | Untested        |

## Acknowledgements

- **gfduszynski**, for his work on [cm-rgb](https://github.com/gfduszynski/cm-rgb). Although I started Wraith Master before discovering cm-rgb, gfduszynski's work made it viable.
- **Cooler Master**, for manufacturing these great stock coolers, and forwarding me to AMD when I inquired about how the Wraith Prism's USB interface worked.
- **AMD**, for telling me I'd need to sign an NDA to get that information. Sorry, but that's not really my style. Thanks for the great product though!

[wraith-master-logo]: https://serebit.com/images/wraith-master-banner-nopad.svg "Wraith Master"
[pipeline-status]: https://gitlab.com/serebit/wraith-master/badges/master/pipeline.svg "Pipeline Status"
[license-badge]: https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg "License"
[kofi-badge]: https://img.shields.io/badge/-ko--fi-ff5f5f?logo=ko-fi&logoColor=white "Ko-fi"
