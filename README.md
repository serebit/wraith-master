![Wraith Master][wraith-master-logo]

[![Pipeline Status][pipeline-status]](https://gitlab.com/serebit/wraith-master/commits/master)
[![License][license-badge]](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Ko-fi][kofi-badge]](https://ko-fi.com/serebit)

---

Wraith Master is a feature-complete graphical and command-line application for controlling the RGB LEDs on AMD's Wraith stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add other Wraith coolers as well.

Designed for feature parity with the official Windows-only Cooler Master application, Wraith Master is intended to support all modes and settings that the Wraith Prism can use. Only one feature remains to be implemented (fan mirage color frequencies). As the Wraith coolers are capable of storing RGB configurations in-device, no daemon is required to maintain configurations, and the program can be uninstalled without fear of losing your settings.

## Supported Platforms

Wraith Master supports all 64-bit Linux distributions that use glibc as their libc implementation. This dependence on glibc is not my choice, but a limitation of the software stack; Kotlin/Native is [incompatible with musl](https://github.com/JetBrains/kotlin-native/issues/3771) as of the time I am writing this. Once it gains compatibility with more libc implementations, I fully intend to take advantage of that compatibility.

## Installing

#### Arch/Derivatives

If you're on Arch Linux or one of its derivatives, you can install both frontends [from the AUR](https://aur.archlinux.org/packages/?K=wraith%2Dmaster), under `wraith-master-cli` for the CLI frontend and `wraith-master-gtk` for the GTK frontend. 

#### Solus

Solus has both frontends in the official repository. They can be installed either by running `sudo eopkg install wraith-master` in the terminal, or searching for the package in the Software Center.

#### Other Distributions

There are precompiled binaries available for each release [here](https://gitlab.com/serebit/wraith-master/-/releases).

## Screenshots

![Screenshot][wraith-master-screenshot]

## Runtime Dependencies

| Distribution       | Shared Dependencies           | GTK-Only                |
|--------------------|-------------------------------|-------------------------|
| Debian/Derivatives | `libusb-1.0-0`, `libncurses5` | `glib2.0`, `libgtk-3.0` |
| Arch/Derivatives   |                               | `gtk3`                  |
| Fedora             |                               | `gtk3`                  |
| OpenSUSE           |                               | `glib2`, `gtk3`         |
| Solus              |                               |                         |
| Gentoo             | `dev-libs/libusb`             | `gtk+`                  |

## Building from Source

See [this section of the wiki](https://gitlab.com/serebit/wraith-master/-/wikis/help/building-from-source) for instructions on building this software yourself.

## Changelog

See `CHANGELOG.md` for notes on previous releases, along with changes that are currently in staging for the next release.

## License

Wraith Master is open-sourced under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).

## Acknowledgements

- **gfduszynski**, for his work on [cm-rgb](https://github.com/gfduszynski/cm-rgb). Although I started Wraith Master before discovering cm-rgb, gfduszynski's groundwork made it viable for me to continue working on it.
- **Adam Honse**, for his work on [OpenRGB](https://gitlab.com/CalcProgrammer1/OpenRGB). This had some extra documentation on specific functions that I was lacking.
- **ballsystemlord**, **Kirk**, and **tralamazza** from the [AdoredTV](https://adoredtv.com/) Discord server, along with **[Apache](https://github.com/Apache-HB)** and **my dad**, for helping me figure out how the mirage frequencies are converted to byte values.
- **Cooler Master**, for manufacturing these great stock coolers, and being as helpful as they're allowed to be when I asked about how the USB interface worked.
- **AMD**, for including actually decent stock coolers with their desktop processors.

[wraith-master-logo]: https://serebit.com/images/wraith-master-banner-nopad.svg "Wraith Master"
[pipeline-status]: https://gitlab.com/serebit/wraith-master/badges/master/pipeline.svg "Pipeline Status"
[license-badge]: https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg "License"
[kofi-badge]: https://img.shields.io/badge/-ko--fi-ff5f5f?logo=ko-fi&logoColor=white "Ko-fi"
[wraith-master-screenshot]: https://serebit.com/images/wraith-master-screenshot.png "Screenshot"
