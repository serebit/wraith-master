![Wraith Master][wraith-master-logo]

[![License][license-badge]](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Ko-fi][kofi-badge]](https://ko-fi.com/serebit)

---

Wraith Master is a graphical and command-line application for controlling the RGB LEDs on AMD's Wraith stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add other Wraith coolers as well.

## Building from Source

##### Requirements

Building Wraith Master requires the following to be installed on the system:
- `libtinfo.so.5`, usually in a package called `ncurses-compat-libs`
- `libusb` development libraries
- A JDK, whether Open or Oracle, of at least version 8
- If building the GTK frontend, the GTK3 development libraries

##### Instructions

Once built, the executables will be located in the `build/package` directory.

To build just the command-line application, run the following:

```bash
./gradlew :cli:package
```

To build just the graphical application, run the following:

```bash
./gradlew :gtk:package
```

To build all artifacts, run the following:

```bash
./gradlew package
```

## Acknowledgements

- **gfduszynski**, for his work on [cm-rgb](https://github.com/gfduszynski/cm-rgb). Although I started Wraith Master before discovering cm-rgb, gfduszynski's work made it viable.
- **Cooler Master**, for manufacturing these great stock coolers, and forwarding me to AMD when I inquired about how the Wraith Prism's USB interface worked.
- **AMD**, for telling me I'd need to sign an NDA to get that information. Sorry, but that's not really my style. Thanks for the great product though!

[wraith-master-logo]: https://serebit.com/images/wraith-master-banner-nopad.svg "Wraith Master"
[license-badge]: https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg "License"
[kofi-badge]: https://img.shields.io/badge/-ko--fi-ff5f5f?logo=ko-fi&logoColor=white "Ko-fi"
