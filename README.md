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

### Requirements

Building Wraith Master requires the following to be installed on the system:
- `libtinfo.so.5`, usually in a package called `ncurses-compat-libs`
- `libusb` development libraries
- A JDK, whether Open or Oracle, of at least version 8
- If building the GTK frontend, the GTK3 development libraries

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

## Dependencies

Both frontends require the following packages to run:
- `glibc`
- `libgcc`
- `libusb`

The GTK frontend also requires the following packages:
- `libgtk-3`
- `glib2`

## Acknowledgements

- **gfduszynski**, for his work on [cm-rgb](https://github.com/gfduszynski/cm-rgb). Although I started Wraith Master before discovering cm-rgb, gfduszynski's work made it viable.
- **Cooler Master**, for manufacturing these great stock coolers, and forwarding me to AMD when I inquired about how the Wraith Prism's USB interface worked.
- **AMD**, for telling me I'd need to sign an NDA to get that information. Sorry, but that's not really my style. Thanks for the great product though!

[wraith-master-logo]: https://serebit.com/images/wraith-master-banner-nopad.svg "Wraith Master"
[pipeline-status]: https://gitlab.com/serebit/wraith-master/badges/master/pipeline.svg "Pipeline Status"
[license-badge]: https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg "License"
[kofi-badge]: https://img.shields.io/badge/-ko--fi-ff5f5f?logo=ko-fi&logoColor=white "Ko-fi"
