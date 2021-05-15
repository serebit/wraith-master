.POSIX:
.SUFFIXES:

PKGNAME?=wraith-master

GRADLE?=./gradlew
GRADLEFLAGS?=

PREFIX?=/usr/local
BINDIR?=$(DESTDIR)$(PREFIX)/bin
MANDIR?=$(DESTDIR)$(PREFIX)/man
UDEVDIR?=$(DESTDIR)/etc/udev/rules.d

DEFAULT_GRADLE_OPTS=--no-daemon

# Exists in GNUMake but not in NetBSD make and others.
RM?=rm -f


clean:
	$(GRADLE) $(DEFAULT_GRADLE_OPTS) $(GRADLEFLAGS) clean
	$(RM) -r .gradle


all: build/MAKE/gtk build/MAKE/cli

build/MAKE/cli:
	$(GRADLE) $(DEFAULT_GRADLE_OPTS) $(GRADLEFLAGS) :cli:linkReleaseExecutableLinuxX64
	install -Dm 0755 cli/build/bin/linuxX64/releaseExecutable/cli.kexe build/MAKE/cli
ifndef $(DISABLE_MAN_PAGES)
	scdoc < resources/cli/wraith-master.1.scd > build/MAKE/wraith-master.1
endif

build/MAKE/gtk:
	$(GRADLE) $(DEFAULT_GRADLE_OPTS) $(GRADLEFLAGS) :gtk:linkReleaseExecutableLinuxX64
	install -Dm 0755 gtk/build/bin/linuxX64/releaseExecutable/gtk.kexe build/MAKE/gtk
ifndef $(DISABLE_MAN_PAGES)
	scdoc < resources/gtk/wraith-master-gtk.1.scd > build/MAKE/wraith-master-gtk.1
endif


install: install-common install-gtk install-cli

install-common:
	install -Dm 0644 resources/common/99-$(PKGNAME).rules $(UDEVDIR)/99-$(PKGNAME).rules

install-cli: build/MAKE/cli
	install -Dm 0755 build/MAKE/cli $(BINDIR)/wraith-master
ifeq (,$(wildcard build/MAKE/wraith-master.1))
	install -Dm 0644 build/MAKE/wraith-master.1 $(MANDIR)/man1/wraith-master.1
endif

install-gtk: build/MAKE/gtk
	install -Dm 0755 build/MAKE/gtk $(BINDIR)/wraith-master-gtk
ifeq (,$(wildcard build/MAKE/wraith-master-gtk.1))
	install -Dm 0644 build/MAKE/wraith-master-gtk.1 $(MANDIR)/man1/wraith-master-gtk.1
endif

.DEFAULT_GOAL := all
