%global debug_package %{nil}

Name:      wraith-master
Version:   %%VERSION%%
Release:   1%{?dist}
Summary:   Software for controlling the RGB LEDs on AMD Wraith CPU coolers
License:   Apache-2.0
URL:       https://gitlab.com/serebit/%{name}

Source0:   %{name}-%{version}.tar
BuildArch: x86_64

%description
Wraith Master is a lightweight application for controlling the RGB LEDs on AMD's Wraith stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add other Wraith coolers as well. Both a command line interface and a graphical interface are provided.

%prep
%setup -q

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}
cp -a * %{buildroot}

%check
desktop-file-validate %{buildroot}%{_datadir}/applications/%{name}.desktop
appstream-util validate-relax --nonet %{buildroot}%{_datadir}/metainfo/*.metainfo.xml

%clean
rm -rf %{buildroot}

%files
%{_bindir}/%{name}
%{_bindir}/%{name}-gtk
%{_libdir}/udev/rules.d/99-%{name}.rules
%{_datadir}/applications/%{name}.desktop
%{_datadir}/metainfo/%{name}.metainfo.xml
%{_datadir}/icons/hicolor/scalable/apps/%{name}.svg
%{_mandir}/man1/wraith-master-gtk.1.gz
%{_mandir}/man1/wraith-master.1.gz
