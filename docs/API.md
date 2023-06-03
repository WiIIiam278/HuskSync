[![HuskSync banner](https://raw.githubusercontent.com/WiIIiam278/HuskSync/master/images/banner.png)](https://github.com/WiIIiam278/HuskSync)
# HuskSync API v2
![](https://jitpack.io/v/WiIIiam278/HuskSync.svg)

The HuskSync API provides methods for retrieving and updating user data, as well as a number of events for tracking when user data is synced and saved. 

The API is distributed via [JitPack](https://jitpack.io/#net.william278/HuskSync). Please note that the HuskSync v1 API is not compatible.
(Some) javadocs are also available to view on Jitpack [here](https://javadoc.jitpack.io/net/william278/HuskSync/latest/javadoc/).

## Table of contents
1. Adding the API to your project
2. Adding HuskSync as a dependency
3. Next steps

## API Introduction
### 1.1 Setup with Maven
<details>
<summary>Maven setup information</summary>

- Add the repository to your `pom.xml` as per below.
```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```
- Add the dependency to your `pom.xml` as per below. Replace `version` with the latest version of HuskSync (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/HuskSync?color=%23282828&label=%20&style=flat-square)
```xml
<dependency>
    <groupId>net.william278</groupId>
    <artifactId>HuskSync</artifactId>
    <version>version</version>
    <scope>provided</scope>
</dependency>
```
</details>

### 1.2 Setup with Gradle
<details>
<summary>Gradle setup information</summary>

- Add the dependency like so to your `build.gradle`:
```groovy
allprojects {
	repositories {
		maven { url 'https://jitpack.io' }
	}
}
```
- Add the dependency as per below. Replace `version` with the latest version of HuskSync (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/HuskSync?color=%23282828&label=%20&style=flat-square)

```groovy
dependencies {
    compileOnly 'net.william278:HuskSync:version'
}
```
</details>

### 2. Adding HuskSync as a dependency
- Add HuskSync to your `softdepend` (if you want to optionally use HuskSync) or `depend` (if your plugin relies on HuskSync) section in `plugin.yml` of your project.

```yaml
name: MyPlugin
version: 1.0
main: net.william278.myplugin.MyPlugin
author: William278
description: 'A plugin that hooks with the HuskSync API!'
softdepend: # Or, use 'depend' here
  - HuskSync
```

### 3. Next steps
Now that you've got everything ready, you can start doing stuff with the HuskSync API!
- [[UserData API]] &mdash; Get data snapshots and update current user data
- [[API Events]] &mdash; Listen to, cancel and modify the result of data synchronization events