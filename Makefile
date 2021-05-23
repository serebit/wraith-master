PKGNAME ?= wraith-master

GRADLE ?= ./gradlew
GRADLEFLAGS ?=

PREFIX ?= /usr/local
BINDIR ?= $(PREFIX)/bin
LIBDIR ?= $(PREFIX)/lib
SHAREDIR ?= $(PREFIX)/share
MANDIR ?= $(SHAREDIR)/man
UDEVDIR ?= $(LIBDIR)/udev/rules.d

DEFAULT_GRADLE_OPTS = --no-daemon --console=plain


common build/PACKAGE/udev/99-wraith-master.rules:
ifneq ($(DISABLE_UDEV),true)
	install -Dm 0644 resources/common/99-wraith-master.rules build/PACKAGE/udev/99-wraith-master.rules
endif

cli: $(if $(filter true,$(DISABLE_MAN_PAGES)),,build/PACKAGE/man/wraith-master.1)
	$(GRADLE) $(DEFAULT_GRADLE_OPTS) $(GRADLEFLAGS) :cli:linkReleaseExecutableLinuxX64
	install -Dm 0755 cli/build/bin/linuxX64/releaseExecutable/cli.kexe build/PACKAGE/wraith-master

gtk: $(if $(filter true,$(DISABLE_MAN_PAGES)),,build/PACKAGE/man/wraith-master-gtk.1)
	$(GRADLE) $(DEFAULT_GRADLE_OPTS) $(GRADLEFLAGS) :gtk:linkReleaseExecutableLinuxX64
	install -Dm 0755 gtk/build/bin/linuxX64/releaseExecutable/gtk.kexe build/PACKAGE/wraith-master-gtk
	install -Dm 0644 resources/gtk/wraith-master.desktop build/PACKAGE/desktop/wraith-master.desktop
	install -Dm 0644 resources/gtk/wraith-master.metainfo.xml build/PACKAGE/metainfo/wraith-master.metainfo.xml
	install -Dm 0644 resources/gtk/wraith-master.svg build/PACKAGE/icons/wraith-master.svg

all: build/PACKAGE/udev/99-wraith-master.rules $(if $(DISABLE_MAN_PAGES),,build/PACKAGE/man/wraith-master.1 build/PACKAGE/man/wraith-master-gtk.1)
	$(GRADLE) $(DEFAULT_GRADLE_OPTS) $(GRADLEFLAGS) linkReleaseExecutableLinuxX64
	install -Dm 0755 cli/build/bin/linuxX64/releaseExecutable/cli.kexe build/PACKAGE/wraith-master
	install -Dm 0755 gtk/build/bin/linuxX64/releaseExecutable/gtk.kexe build/PACKAGE/wraith-master-gtk
	install -Dm 0644 resources/gtk/wraith-master.desktop build/PACKAGE/desktop/wraith-master.desktop
	install -Dm 0644 resources/gtk/wraith-master.metainfo.xml build/PACKAGE/metainfo/wraith-master.metainfo.xml
	install -Dm 0644 resources/gtk/wraith-master.svg build/PACKAGE/icons/wraith-master.svg

build/PACKAGE/man:
	mkdir -p build/PACKAGE/man

build/PACKAGE/man/wraith-master.1: build/PACKAGE/man
	scdoc < resources/cli/wraith-master.1.scd > build/PACKAGE/man/wraith-master.1

build/PACKAGE/man/wraith-master-gtk.1: build/PACKAGE/man
	scdoc < resources/gtk/wraith-master-gtk.1.scd > build/PACKAGE/man/wraith-master-gtk.1


install: install-common install-gtk install-cli

install-common:
ifneq ("$(wildcard build/PACKAGE/udev/99-wraith-master.rules)","")
	install -Dm 0644 resources/common/99-wraith-master.rules $(DESTDIR)$(UDEVDIR)/99-wraith-master.rules
endif

install-cli: build/PACKAGE/wraith-master
	install -Dm 0755 build/PACKAGE/wraith-master $(DESTDIR)$(BINDIR)/wraith-master
ifneq ("$(wildcard build/PACKAGE/man/wraith-master.1)","")
	install -Dm 0644 build/PACKAGE/man/wraith-master.1 $(DESTDIR)$(MANDIR)/man1/wraith-master.1
endif

install-gtk: build/PACKAGE/wraith-master-gtk
	install -Dm 0755 build/PACKAGE/wraith-master-gtk $(DESTDIR)$(BINDIR)/wraith-master-gtk
	install -Dm 0644 build/PACKAGE/desktop/wraith-master.desktop $(DESTDIR)$(SHAREDIR)/applications/wraith-master.desktop
	install -Dm 0644 build/PACKAGE/metainfo/wraith-master.metainfo.xml $(DESTDIR)$(SHAREDIR)/metainfo/wraith-master.metainfo.xml
	install -Dm 0644 build/PACKAGE/icons/wraith-master.svg $(DESTDIR)$(SHAREDIR)/icons/hicolor/scalable/apps/wraith-master.svg
ifneq ("$(wildcard build/PACKAGE/man/wraith-master-gtk.1)","")
	install -Dm 0644 build/PACKAGE/man/wraith-master-gtk.1 $(DESTDIR)$(MANDIR)/man1/wraith-master-gtk.1
endif


.DEFAULT_GOAL := all
