The HuskSync API provides methods for retrieving and updating user data, as well as a number of events for tracking when user data is synced and saved. 

## Compatibility
[![Maven](https://repo.william278.net/api/badge/latest/releases/net/william278/husksync?color=00fb9a&name=Maven&prefix=v)](https://repo.william278.net/#/releases/net/william278/husksync/)

The HuskSync API shares version numbering with the plugin itself for consistency and convenience. Please note minor and patch plugin releases may make API additions and deprecations, but will not introduce breaking changes without notice.

| API Version |  HuskSync Versions  | Supported |
|:-----------:|:--------------------:|:---------:|
|    v2.x     | _v2.0&mdash;Current_ |     ✅     |
|    v1.x     | _v1.0&mdash;v1.4.1_  |    ❌️     |

<details>
<summary>Targeting older versions</summary>

HuskSync versions prior to `v2.2.5` are distributed on [JitPack](https://jitpack.io/#/net/william278/HuskSync), and you will need to use the `https://jitpack.io` repository instead.
</details>

## Table of contents
1. Adding the API to your project
2. Adding HuskSync as a dependency
3. Next steps

## API Introduction
### 1.1 Setup with Maven
<details>
<summary>Maven setup information</summary>

Add the repository to your `pom.xml` as per below. You can alternatively specify `/snapshots` for the repository containing the latest development builds (not recommended).
```xml
<repositories>
    <repository>
        <id>william278.net</id>
        <url>https://repo.william278.net/releases</url>
    </repository>
</repositories>
```
Add the dependency to your `pom.xml` as per below. Replace `VERSION` with the latest version of HuskSync (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/HuskSync?color=%23282828&label=%20&style=flat-square)
```xml
<dependency>
    <groupId>net.william278</groupId>
    <artifactId>husksync</artifactId>
    <version>VERSION</version>
    <scope>provided</scope>
</dependency>
```
</details>

### 1.2 Setup with Gradle
<details>
<summary>Gradle setup information</summary>

Add the dependency as per below to your `build.gradle`. You can alternatively specify `/snapshots` for the repository containing the latest development builds (not recommended).
```groovy
allprojects {
	repositories {
		maven { url 'https://repo.william278.net/releases' }
	}
}
```
Add the dependency as per below. Replace `VERSION` with the latest version of HuskSync (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/HuskSync?color=%23282828&label=%20&style=flat-square)

```groovy
dependencies {
    compileOnly 'net.william278:husksync:VERSION'
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