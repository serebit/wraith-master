%global debug_package %{nil}

Name:          wraith-master
Version:       %%VERSION%%
Release:       1%{?dist}
Summary:       Software for controlling the RGB LEDs on AMD Wraith CPU coolers
License:       Apache-2.0
URL:           https://gitlab.com/serebit/%{name}

Source0:       %{name}-%{version}.tar
BuildArch:     x86_64

%description
Wraith Master is a lightweight application for controlling the RGB LEDs on AMD's Wraith stock coolers. At the moment, the only supported cooler is the Wraith Prism, but there are plans to add other Wraith coolers as well. Both a command line interface and a graphical interface are provided.

%prep
%setup -q

%install
rm -rf %{buildroot}
mkdir -p  %{buildroot}
cp -a * %{buildroot}

%clean
rm -rf %{buildroot}

%files
%{_bindir}/%{name}
%{_bindir}/%{name}-gtk
%{_prefix}/lib/udev/rules.d/99-%{name}.rules
%{_prefix}/share/applications/%{name}.desktop
%{_prefix}/share/icons/hicolor/scalable/apps/%{name}.svg
%{_prefix}/share/man/man1/wraith-master-gtk.1.gz
%{_prefix}/share/man/man1/wraith-master.1.gz
