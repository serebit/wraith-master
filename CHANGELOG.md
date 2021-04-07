# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased - v1.2.0-dev

#### Added

- [#3](https://gitlab.com/serebit/wraith-master/-/issues/3) - Partial support for Alpine, Adélie, and other
  distributions that use musl! Requires gcompat to be installed for Alpine and Adélie, but otherwise works out of the box
- [#13](https://gitlab.com/serebit/wraith-master/-/issues/13) - Man pages for both GTK and CLI, hand-written and
  compiled by scdoc
- [#16](https://gitlab.com/serebit/wraith-master/-/issues/16) - Support for manually resetting the USB port

#### Changed

- Removed `libusb_reset_device` call in initialization code. Makes initialization time 27x faster, which results in a
  10x speedup for the CLI frontend (tested on my machine, may vary by hardware)
- Version number is now inserted at build time, which means there are no scenarios in which the version number cannot be
  resolved

#### Fixed

- GTK now exits gracefully if the device is disconnected while the program is running

#### Meta

- Replace most compile-time Gradle properties (see
  [the wiki](https://gitlab.com/serebit/wraith-master/-/wikis/help/building-from-source) for more information)
- Add `disable-man-pages` Gradle property to disable automatic man page generation
- Add `releaseDeb` and `releaseRpm` tasks
- Rename `distTar` task to `releaseTar`

## v1.1.2 (2021-03-16)

#### Fixed

- Use custom Konan args to remove unused binary dependencies, such as glib-2.0 and libcrypt
- Additionally remove dependency on libgcc_s

#### Meta

- Update Kotlin to v1.4.31
- Update Gradle wrapper to v6.8.3

## v1.1.1 (2021-02-06)

#### Fixed

- Update GTK's `.desktop` file with accurate categories and a TryExec field
- Error codes no longer display as `CPointer`, and instead show the proper error name
- CLI can now properly set logo and fan modes (regressed in v1.1.0)
- [#19](https://gitlab.com/serebit/wraith-master/-/issues/19) - instead of crashing on receiving an invalid mode byte,
  store the default mode internally

#### Meta

- Update Kotlin to v1.4.30
- Update Gradle wrapper to v6.8.2

## v1.1.0 (2020-08-09)

#### Added

- Firmware version reporting
- Resetting to default profile
- Toggle for Enso mode
- Focus clears on pressing the escape button

#### Changed

- More padding in GTK, since it felt a bit cramped to some people

#### Fixed

- More precise finding of `version.txt`, allowing version detection if the `installdir` is set to something other
  than `/usr` or `/usr/local`
- Clear focus on click in the About dialog box
- Prevent potentially weird behavior or crashing with better memory management
- [#10](https://gitlab.com/serebit/wraith-master/-/issues/10) - Remove "plugdev" group requirement to resolve udev error
  on Fedora

#### Meta

- Remove `taskTree` gradle plugin, as it is no longer needed
- Fix valgrind tasks
- Update Kotlin to 1.4.0-rc
- Update Gradle to 6.6-rc-6
- Add Gradle properties `forceudev`, `installmode`, and `packageroot` (see the wiki for more details on these
  properties)

## v1.0.0 (2020-05-10)

#### Added

- **Mirage frequency control**, the last major feature that required implementation
- "About" dialog in GTK, viewable by clicking the info button in the title bar
- `version` option in CLI
- Dialog box in GTK upon attempting to exit with unsaved changes

#### Changed

- Increased width of morse textbox in GTK
- Made overall GTK design more compact
- In GTK, the Reset button no longer power-cycles the device
- In GTK: the Save, Reset, and morse's Apply buttons now change their sensitivity based on whether the current values
  are the same as the saved values
- In GTK, saved settings are restored both on app close and on app start
- In GTK, reordered component tabs to reflect Cooler Master application

#### Removed

- Brightness setting for the ring component's Morse mode is no longer available. Even though the Cooler Master
  application does show support for it, the setting doesn't affect the actual ring brightness

#### Fixed

- Reset button no longer segfaults after a few presses

#### Meta

- Update Kotlin to 1.4-M1 (retains 1.3.7x build compatibility for Solus)
- Update Gradle to 6.4
- Code cleanup in GTK and core, with a focus on reducing total LoC without sacrificing looks or functionality
- `distTar` Gradle task now produces a tarball with correct permissions
- Added `strip` property to `package` Gradle task, which strips packaged binaries if enabled

## v0.5.2 (2020-03-16)

#### Fixed

- Reset button causing segfault

## v0.5.1 (2020-03-13)

#### Added

- `verbose` flag to show program status in CLI frontend

#### Fixed

- Color button in GTK frontend no longer stays sensitive to input on mode change or startup when color randomization is
  enabled
- Reset button in GTK frontend updates widgets to the device's saved values
- Significantly reduced number of transfers to device in CLI frontend
- Significantly more checks to ensure parameter validity in CLI frontend
- Eliminated (tiny) memory leaks in setup caused by unfreed libusb resources

#### Meta

- Updated Kotlin to 1.3.70
- Reduced code complexity and duplication

## v0.5.0 (2020-03-01)

#### Added

- **Morse text setting**, which can parse plaintext along with literal morse code
- Randomized color option for supported modes

#### Changed

- Changed "Direction" label to "Rotation Direction" for clarity
- Adjusted padding of main settings box

#### Fixed

- Modified style to be more consistent across GTK themes while maintaining that theme's look and feel
- Fixed occasional crash on setting ring mode to "Breathe", not sure what caused it
- Fixed crash on swapping ring mode to a mode that supports both random colors and rotation direction if the random
  colors option was selected for that mode outside of Wraith Master
- Clicking on the window now clears the keyboard focus
- Color buttons now have the correct colors set on startup for modes that don't support color

#### Meta

- Updated Kotlin to 1.3.70-eap-3

## v0.4.2 (2020-01-21)

#### Added

- Informational dialog box when attempting to open a new Wraith Master window while one already exists

## v0.4.1 (2020-01-13)

#### Changed

- In both frontends, restore a mode's previous settings (color, speed, brightness, etc) when that mode is selected

#### Fixed

- Regression in GTK frontend where a failure in resolving the Wraith device would crash the program instead of providing
  the error message

## v0.4.0 (2020-01-30)

#### Added

- Mirage toggle for fan
- `noudev` Gradle property to disable automatic installation of udev rules
- Better error handling on opening USB device for transfers

#### Changed

- Options and arguments in `cli` frontend are no longer case-sensitive

#### Fixed

- Fan mode toggle applying to logo instead
- Fan settings not being disabled when a mode that can't use those settings is selected
- Creating additional broken "remote instance" windows is no longer allowed
- Both frontends no longer require `sudo` to run on systems that use `udev` or `eudev`
- Compilation now works on Debian without modification. Alpine is pending a fix
  for [this issue](https://github.com/JetBrains/kotlin-native/issues/3771)

## v0.3.1 (2020-01-13)

#### Fixed

- Both frontends no longer require `sudo` to run on systems that use `systemd`

## v0.3.0 (2020-01-13)

#### Added

- Error dialog for when a Wraith Prism USB device can't be found
- Direction control for ring modes that support it
- New page-based layout for GTK frontend
- Certain controls in the GTK frontend become inactive when a mode that doesn't support them is selected
- `install` Gradle task, to make installation easier for both packagers and regular users who build from source
- Scalable icon and desktop file for GTK frontend

## v0.2.0 (2020-01-06)

#### Added

- Mode support. All modes that are provided by Cooler Master's Windows application are supported, except for Morse,
  which will be added later
- Speed adjustment for modes that support it

#### Changed

- Brightness adjustment now has 3 settings instead of 5 to better reflect Cooler Master's application

#### Fixed

- Resources are now properly closed on program exit, on both the GTK frontend and libusb frontend

## v0.1.0 (2020-01-01)

#### Added

- Command-line application, in addition to the GTK application. Both will be developed together moving forward, and both
  can be built independently of each other
- Controls in `wraith-master-gtk` now show the colors that were present when the application started up
- Brightness controls

#### Changed

- Layout of `wraith-master-gtk` is no longer claustrophobic

#### Removed

- Removed debug print statements

#### Fixed

- Off-by-one error causing miscalculations in colors sent to hardware
