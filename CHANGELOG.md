# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

#### Meta 
- Updated Kotlin to 1.3.70

#### Added 
- Significantly more checks to ensure parameter validity in CLI frontend
- `verbose` flag to show program status in CLI frontend

#### Changed 
- Reduced code complexity and duplication

#### Fixed 
- Color button in GTK frontend no longer stays sensitive to input on mode change or startup when color randomization is enabled
- Reset button in GTK frontend updates widgets to the device's saved values
- Significantly reduced number of transfers to device in CLI frontend
- Eliminated (tiny) memory leaks in setup caused by unfreed libusb resources
- Eliminated some (tiny) memory leaks in GTK setup

## 0.5.0 (2020-03-01)

#### Meta
- Updated Kotlin to 1.3.70-eap-3

#### Added 
- **Morse text setting**, which can parse plaintext along with literal morse code
- Randomized color option for supported modes

#### Changed 
- Changed "Direction" label to "Rotation Direction" for clarity
- Adjusted padding of main settings box

#### Fixed 
- Modified style to be more consistent across GTK themes while maintaining that theme's look and feel
- Fixed occasional crash on setting ring mode to "Breathe", not sure what caused it
- Fixed crash on swapping ring mode to a mode that supports both random colors and rotation direction if the random colors option was selected for that mode outside of Wraith Master
- Clicking on the window now clears the keyboard focus
- Color buttons now have the correct colors set on startup for modes that don't support color

## 0.4.2 (2020-01-21)

#### Added 
- Informational dialog box when attempting to open a new Wraith Master window while one already exists

## 0.4.1 (2020-01-13)

#### Changed 
- In both frontends, restore a mode's previous settings (color, speed, brightness, etc) when that mode is selected

#### Fixed 
- Regression in GTK frontend where a failure in resolving the Wraith device would crash the program instead of providing the error message

## 0.4.0 (2020-01-30)

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
- Compilation now works on Debian without modification. Alpine is pending a fix for [this issue](https://github.com/JetBrains/kotlin-native/issues/3771)

## 0.3.1 (2020-01-13)

#### Fixed 
- Both frontends no longer require `sudo` to run on systems that use `systemd`

## 0.3.0 (2020-01-13)

#### Added 
- Error dialog for when a Wraith Prism USB device can't be found
- Direction control for ring modes that support it
- New page-based layout for GTK frontend
- Certain controls in the GTK frontend become inactive when a mode that doesn't support them is selected
- `install` Gradle task, to make installation easier for both packagers and regular users who build from source
- Scalable icon and desktop file for GTK frontend

## 0.2.0 (2020-01-06)

#### Added 
- Mode support. All modes that are provided by Cooler Master's Windows application are supported, except for Morse, which will be added later
- Speed adjustment for modes that support it

#### Changed 
- Brightness adjustment now has 3 settings instead of 5 to better reflect Cooler Master's application

#### Fixed 
- Resources are now properly closed on program exit, on both the GTK frontend and libusb frontend


## 0.1.0 (2020-01-01)

#### Added 
- Command-line application, in addition to the GTK application. Both will be developed together moving forward, and both can be built independently of each other
- Controls in `wraith-master-gtk` now show the colors that were present when the application started up
- Brightness controls

#### Changed 
- Layout of `wraith-master-gtk` is no longer claustrophobic

#### Removed 
- Removed debug print statements

#### Fixed 
- Off-by-one error causing miscalculations in colors sent to hardware
