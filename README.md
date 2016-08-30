codegenj - DBus Code Generator for Java APIs
============================================

[![Build Status](https://travis-ci.org/olir/codegenj.png)](https://travis-ci.org/olir/codegenj/builds)
[![Coverage Status](https://coveralls.io/repos/github/olir/codegenj/badge.svg?branch=master)](https://coveralls.io/github/olir/codegenj?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.serviceflow/codegenj/badge.png)](https://maven-badges.herokuapp.com/maven-central/de.serviceflow/codegenj)

# About

codegenj is a DBus Code Generator that helps to build Java APIs for DBus interfaces. The tool reads [D-Bus Introspection XML] (https://dbus.freedesktop.org/doc/dbus-specification.html#introspection-format)
 files and generates maven projects. It depends on [gdbus-codegen](https://developer.gnome.org/gio/stable/gdbus-codegen.html). 


## EXAMPLES 

See  [lbt4j](https://github.com/olir/lbt4j), a Linux Bluetooth Library for Java based on BlueZ's D-Bus interfaces.

## Build

### Maven

You can build the jar file and documentation with maven:

> mvn clean package site site:run

Open the project documentation in your web browser on http://localhost:9000 
or open it without site:run under

> target/site/index.html

## codegenj's annotations for D-Bus Introspection XML

codegenj defines some new annotations that can be used to improve the API it generates. Some D-Bus types including 'o' are 'ao' are ignored; using this annotation is the only way to make them available.

<table>
    <tr>
        <th>name</th><th>value</th><th>Description</th><th>context</th>
    </tr>
    <tr>
        <td rowspan=3>de.serviceflow.codegenj.CollectorAPI</td><td>classname#methodname</td><td>Creates an getter method in the named interface or objectmanager (de.serviceflow.codegenj.ObjectManager) that returns "top-level" interfaces from D-Bus of the type the annotation is placed in and returns a collection of them.</td><td>interface</td>
    </tr>
    <tr>
        <td>de.serviceflow.codegenj.CollectorAPI</td><td>classname#<b>*</b>methodname</td><td>Creates an getter method in the named interface or objectmanager (de.serviceflow.codegenj.ObjectManager) that queries "current" interfaces from D-Bus of the type the annotation is placed in and returns a collection of them.</td><td>interface</td>
    </tr>
    <tr>
        <td>de.serviceflow.codegenj.CollectorAPI</td><td>classname</td><td>Modifies a property method to cast "child" interfaces to the type given in the value arg.</td><td>property of access type 'read' with type 'ao'</td>
    </tr>
    <tr>
        <td>de.serviceflow.codegenj.ParentAPI</td><td>interfacename</td><td>Substitutes a D-Bus call by returning the stored parent object (that should match the type of interfacename in value arg) for the object's path.</td><td>property of access type 'read' with type 'o'</td>
    </tr>
    <tr>
        <td>de.serviceflow.codegenj.SkeletonAPI</td><td><i>leave empty</i></td><td>Used to tag interfaces with callbacks</td><td>interface</td>
    </tr>
</table>
