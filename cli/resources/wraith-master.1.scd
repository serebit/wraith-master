wraith-master(1)

# NAME

*wraith-master* - The CLI frontend for Wraith Master, a Wraith cooler RGB control application

# SYNOPSIS

*wraith-master* [_SPECIAL-OPTIONS_] [_GLOBAL-OPTIONS_]

*wraith-master* {logo|fan|ring} [_COMPONENT-OPTIONS_] [_GLOBAL-OPTIONS_]

# DESCRIPTION

Wraith Master is a feature-complete graphical and command-line application for controlling the RGB LEDs on AMD's Wraith
stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add other Wraith
coolers as well.

Designed for feature parity with the official Windows-only Cooler Master application, Wraith Master supports all modes
and settings that the Wraith Prism can use. As the Wraith coolers are capable of storing RGB configurations in-device,
no daemon is required to maintain configurations, and the program can be uninstalled without fear of losing your
settings.

The CLI frontend can be used from installations without a desktop environment, or by those who prefer the command line.

# GLOBAL OPTIONS

*-h*, *--help*
	Print program help to the console.

*-V*, *--verbose*
	Print debug information to the console as the program is running.

# SPECIAL OPTIONS

*-v*, *--version*
	Print the program version to the console.

*-f*, *--firmwareversion*
	Print the version of the connected cooler's firmware to the console.

*-R*, *--resettodefault*
	Resets the configuration to its out-of-the-box defaults.

*-e*, *--toggleenso*
	Toggles Enso mode on/off. Enabling Enso mode clears all settings, and disabling Enso mode restores default settings.

# COMPONENT OPTIONS

*-m*, *--mode*=_MODE_
	Changes the mode of this component's LEDs. Each mode varies in terms of what settings it supports, so the following
	options may not work depending on which mode is selected. All components share a few modes, those being *off*,
	*static*, *cycle*, and *breathe*. The *ring* component additionally has the following modes: *rainbow*, *swirl*,
	*chase*, *bounce*, and *morse*.

*-c*, *--color*=_COLOR_
	Changes the color of this component's LEDs. The value given to this option can either be a hex color in the format
	RRGGBB, or a set of three integers r,g,b.

*-b*, *--brightness*=_BRIGHTNESS_
	Changes the brightness of the component's LEDs. The value given to this option should be an integer from 1 to 3,
	where 1 is the lowest brightness and 3 is the highest brightness.

*-s*, *--speed*=_SPEED_
	Changes the speed of the component's mode. This setting applies to modes where there is some motion involved, like
	*rainbow* or *cycle*, which rotate around the ring and translate between colors respectively. The value given to
	this option should be an integer from 1 to 5, where 1 is the lowest speed and 5 is the highest speed.

*-d*, *--direction*=_DIRECTION_
	Changes the direction of rotation for the ring's mode. This setting only applies to ring modes that rotate, such as
	*rainbow* and *swirl*. The value given to this option should be either *clockwise* or *counterclockwise*.

*-r*, *--randomcolor*
	Toggles the flag that allows the mode to randomly select a color, rather than use the user-defined color. This
	setting only applies to modes that both have motion and allow setting a specific color, like *breathe* and *swirl*.

*-M*, *--mirage*=_VALUE_
	Enables or disables fan mirage on the Wraith Prism. This option is independent of the fan's LED mode, and controls
	how quickly the fan LEDs should turn on and off to simulate a visual pattern of motion for the fan blades. The value
	given to this option should either be three frequencies in the format of r,g,b with values in the range of 45-2000,
	or one of the strings "on" or "off".

*-t*, *--morsetext*=_TEXT_
	Sets the text used for the ring's *morse* mode. The value given to this option can be either a string of morse code
	using dots (.) and dashes (-), along with spaces ( ), or a string of ASCII text to be converted to morse code.

## EXIT STATUS

On success, 0 is returned. A non-zero return code signals a failure.

## COPYRIGHT

Copyright © 2021 Campbell Jones, License: Apache-2.0

## SEE ALSO

*wraith-master-gtk*(1)

https://gitlab.com/serebit/wraith-master

## NOTES

Apache License 2.0

https://www.apache.org/licenses/LICENSE-2.0/
