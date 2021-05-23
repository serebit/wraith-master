version := "1.2.0"

release: tar deb rpm

make:
    make GRADLE=gradle

tar: make
    rm -rf build/tmp/TAR && mkdir -p build/tmp/TAR
    cp -r build/PACKAGE build/tmp/TAR/wraith-master-v{{version}}
    cd build/tmp/TAR && tar -cJf wraith-master-v{{version}}.tar.xz wraith-master-v{{version}}
    install -D build/tmp/TAR/wraith-master-v{{version}}.tar.xz build/RELEASE/wraith-master-v{{version}}.tar.xz
    rm -rf build/tmp/TAR

deb: make
    rm -rf build/tmp/DEB && mkdir -p build/tmp/DEB
    make install DESTDIR=build/tmp/DEB/wraith-master-v{{version}} PREFIX=/usr
    install -D resources/debian-control.txt build/tmp/DEB/wraith-master-v{{version}}/DEBIAN/control
    sed -i 's/%%VERSION%%/{{version}}/g' build/tmp/DEB/wraith-master-v{{version}}/DEBIAN/control
    cd build/tmp/DEB && dpkg-deb -b wraith-master-v{{version}}
    install -D build/tmp/DEB/wraith-master-v{{version}}.deb build/RELEASE/wraith-master-v{{version}}.deb
    rm -rf build/tmp/DEB

rpm: make
    rm -rf build/tmp/RPM && mkdir -p build/tmp/RPM/SOURCES
    make install DESTDIR=build/tmp/RPM/SOURCES/wraith-master-{{version}} PREFIX=/usr LIBDIR=/usr/lib64
    cd build/tmp/RPM/SOURCES && tar -cf wraith-master-{{version}}.tar wraith-master-{{version}}
    install -D resources/fedora.spec build/tmp/RPM/SPECS/wraith-master.spec
    sed -i 's/%%VERSION%%/{{version}}/g' build/tmp/RPM/SPECS/wraith-master.spec
    cd build/tmp/RPM/SPECS && rpmbuild --define "_topdir $PWD/.." --define "_binary_payload w7.xzdio" -ba wraith-master.spec
    install -D build/tmp/RPM/RPMS/x86_64/wraith-master-{{version}}*.rpm build/RELEASE/wraith-master-v{{version}}.rpm
    rm -rf build/tmp/RPM
