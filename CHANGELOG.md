# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
