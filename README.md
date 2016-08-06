codegenj - DBus Code Generator for Java APIs
============================================

[![Build Status](https://travis-ci.org/olir/codegenj.png)](https://travis-ci.org/olir/codegenj/builds)
[![Coverage Status](https://coveralls.io/repos/github/olir/codegenj/badge.svg?branch=master)](https://coveralls.io/github/olir/codegenj?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.serviceflow/codegenj/badge.png)](https://maven-badges.herokuapp.com/maven-central/de.serviceflow/codegenj)

# About

codegenj is a DBus Code Generator that helps to build Java APIs for DBus interfaces. The tool reads [D-Bus Introspection XML] (https://dbus.freedesktop.org/doc/dbus-specification.html#introspection-format)
 files and generates maven projects. It depends on [gdbus-codegen](https://developer.gnome.org/gio/stable/gdbus-codegen.html). 


## EXAMPLES 

See  [lbt4j](../lbt4j), a Linux Bluetooth Library for Java based on BlueZ's D-Bus interfaces.

## Build

### Maven

You can build the jar file and documentation with maven:

> mvn clean package site site:run

Open the project documentation in your web browser on http://localhost:9000 
or open it without site:run under

> target/site/index.html

