# Wraith Master

Wraith Master is a graphical application for controlling the RGB LEDs on AMD's Wraith stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add the Wraith Spire as well.

## Building from Source

Building Wraith Master requires GTK3 and `libusb` development libraries, along with at least JDK8.

To build from source, run the following:

```bash
git clone https://gitlab.com/serebit/wraith-master
cd wraith-master
./gradlew build
```

Once built, the executable will be located at `build/bin/linuxX64/releaseExecutable/wraith-master.kexe`.

## Acknowledgements

- **gfduszynski**, for his work on [cm-rgb](https://github.com/gfduszynski/cm-rgb). Although I started Wraith Master before discovering cm-rgb, gfduszynski's work made it a reality.
- **Cooler Master**, for manufacturing these great stock coolers, and forwarding me to AMD when I inquired about how the Wraith Prism's USB interface worked.
- **AMD**, for telling me I'd need to sign an NDA to get that information. Sorry, but that's not really my style. Thanks for the great product though!