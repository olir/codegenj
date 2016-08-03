# About

codegenj is a DBus Code Generator that helps to build Java APIs for DBus interfaces. The tool reads [D-Bus Introspection XML] (https://dbus.freedesktop.org/doc/dbus-specification.html#introspection-format)
 files and generates maven projects. It depends on [gdbus-codegen](https://developer.gnome.org/gio/stable/gdbus-codegen.html). 


## Build

### Maven

You can build the jar file and documentation with maven:

> mvn clean site site:run

Open the project documentation in your web browser on http://localhost:9000 
or open it without site:run under

> target/site/index.html
 
## HOWTO

Please read [Instructions](src/site/markdown/instructions.md).
 
## Notice
> Please read [Notice](Notice.html) and [Dependencies](dependencies.html).
