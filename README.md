![Wraith Master][wraith-master-logo]

[![GitLab][gitlab-badge]](https://gitlab.com/serebit/wraith-master)
[![GitHub Mirror][github-badge]](https://github.com/serebit/wraith-master)
[![Ko-fi][kofi-badge]](https://ko-fi.com/serebit)
[![AUR][aur-badge]](https://aur.archlinux.org/packages/?K=wraith-master)
[![License][license-badge]](https://www.apache.org/licenses/LICENSE-2.0.html)

---

Wraith Master is a feature-complete graphical and command-line application for controlling the RGB LEDs on AMD's Wraith
stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add other Wraith
coolers as well. It exists as an independent companion to [OpenRGB](https://openrgb.org/), and is designed to provide
control over all functionality exposed by the hardware. In summary: it's **lightweight**, it's **native**, it's
**fast**, it's **complete**, and it's **self-contained.**

![GTK Screenshot][wraith-master-screenshot]

![CLI Screenshot][wraith-master-terminal-screenshot]

## Installing

![Packaging Status](https://repology.org/badge/vertical-allrepos/wraith-master.svg)

### Debian, Ubuntu, Mint

Prebuilt `.deb` packages are available on the Releases page on both GitLab and GitHub.

### Red Hat, Fedora

Prebuilt `.rpm` packages are available on the Releases page on both GitLab and GitHub.

### Arch Linux

[![AUR package](https://repology.org/badge/version-for-repo/aur/wraith-master.svg)](https://aur.archlinux.org/packages/?K=wraith%2Dmaster)

If you're on Arch Linux or one of its derivatives, you can install both frontends from the AUR. If you'd rather not
build from source, you'll want to install `wraith-master-bin`; otherwise, it's under `wraith-master-cli` for the CLI
frontend and `wraith-master-gtk` for the GTK frontend.

### Solus

[![Solus package](https://repology.org/badge/version-for-repo/solus/wraith-master.svg)](https://packages.getsol.us/shannon/w/wraith-master/)

Solus has both frontends in the official repository. They can be installed either by
running `sudo eopkg install wraith-master` in the terminal, or searching for the package in the Software Center.

### Other Distributions

There are precompiled binaries available for each release [here](https://gitlab.com/serebit/wraith-master/-/releases),
with all binaries and resources included.

## Runtime Dependencies

These dependencies only need to be manually installed if you either built the software yourself, or are using a
standalone binary rather than a distribution package (which would prompt these dependencies to install).

#### Normal Version

| Distribution         | Shared Dependencies | GTK-Only     |
|----------------------|---------------------|--------------|
| Debian, Ubuntu, etc. | `libusb-1.0-0`      | `libgtk-3.0` |
| Arch, Manjaro, etc.  |                     | `gtk3`       |
| Fedora               |                     | `gtk3`       |
| OpenSUSE             |                     | `gtk3`       |
| Solus                |                     |              |
| Void                 |                     |              |
| Gentoo               | `dev-libs/libusb`   | `gtk+`       |
| Alpine               | `libusb`            | `gtk+3.0`    |

## Building from Source

See [this section of the wiki](https://gitlab.com/serebit/wraith-master/-/wikis/help/building-from-source) for
instructions on building this software yourself.

## Changelog

See `CHANGELOG.md` for notes on previous releases, along with changes that are currently in staging for the next
release.

## License

Wraith Master is open-sourced under the [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.html).
All contributions to this repository will be subject to this license.

## In the News

- [Phoronix: Wraith Master 1.0 Released For Controlling AMD RGB Fans On Linux](https://www.phoronix.com/scan.php?page=news_item&px=Wraith-Master-1.0)
  by Michael Larabel
- [Gaming On Linux: Take control of your AMD Wraith Prism RGB on Linux with Wraith Master](https://www.gamingonlinux.com/2020/08/take-control-of-your-amd-wraith-prism-rgb-on-linux-with-wraith-master)
  by Liam Dawe

## Acknowledgements

- **gfduszynski**, for his work on [cm-rgb](https://github.com/gfduszynski/cm-rgb). Although I started Wraith Master
  before discovering cm-rgb, gfduszynski's groundwork made it viable for me to continue working on it.
- **Adam Honse**, for his work on [OpenRGB](https://openrgb.org/). This had some extra documentation on specific
  functions that I was lacking.
- **ballsystemlord**, **Kirk**, and **tralamazza** from the [AdoredTV](https://adoredtv.com/) Discord server, along
  with **[Apache](https://github.com/Apache-HB)** and **my dad**, for helping me figure out how the mirage frequencies
  are converted to byte values.
- The fine people of the `#musl` channel on Freenode, for helping me out with getting Wraith Master working on Alpine
  and Adélie.
- **AMD** and **Cooler Master**. Please don't sue me :)

[wraith-master-logo]: https://serebit.com/images/wraith-master-banner-nopad.svg "Wraith Master"

[gitlab-badge]: https://img.shields.io/badge/-gitlab-6e49cb?logo=gitlab "GitLab"

[github-badge]: https://img.shields.io/badge/-github-505050?logo=github "GitLab"

[kofi-badge]: https://img.shields.io/badge/-ko--fi-ff5f5f?logo=ko-fi&logoColor=white "Ko-fi"

[aur-badge]: https://img.shields.io/aur/version/wraith-master-gtk

[pipeline-status-badge]: https://gitlab.com/serebit/wraith-master/badges/master/pipeline.svg "Pipeline Status"

[license-badge]: https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg "License"

[wraith-master-screenshot]: https://serebit.com/images/wraith-master-screenshot.png "GTK Screenshot"

[wraith-master-terminal-screenshot]: https://serebit.com/images/wraith-master-terminal-screenshot.png "CLI Screenshot"
