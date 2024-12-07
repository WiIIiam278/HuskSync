The HuskSync API (v3) provides methods for retrieving and updating [data snapshots](Data-Snapshot-API), a number of [[API Events]] for tracking when user data is synced and saved, and infrastructure for registering serializers to [synchronise custom data types](Custom-Data-API).

## Compatibility
[![Maven](https://repo.william278.net/api/badge/latest/releases/net/william278/husksync/husksync-common?color=00fb9a&name=Maven&prefix=v)](https://repo.william278.net/#/releases/net/william278/husksync/)

The HuskSync API shares version numbering with the plugin itself for consistency and convenience. Please note minor and patch plugin releases may make API additions and deprecations, but will not introduce breaking changes without notice.

| API Version |  HuskSync Versions   | Supported |
|:-----------:|:--------------------:|:---------:|
|    v3.x     | _v3.0&mdash;Current_ |     ✅     |
|    v2.x     | _v2.0&mdash;v2.2.8_  |     ❌     |
|    v1.x     | _v1.0&mdash;v1.4.1_  |    ❌️     |

### Platforms
> **Note:** For versions older than `v3.3`, the HuskSync API was only distributed for the Bukkit platform (as `net.william278:husksync`) 

The HuskSync API is available for the following platforms:

* `bukkit` - Bukkit, Spigot, Paper, etc. Provides Bukkit API event listeners and adapters to `org.bukkit` objects.
* `fabric` - Fabric API for Minecraft. Provides Fabric API event listeners and adapters to `net.minecraft` objects.
* `common` - Common API for all platforms.

<details>
<summary>Targeting older versions</summary>

* The HuskSync API was only distributed for the Bukkit module prior to `v3.3`; the artifact ID was `net.william278:husksync` instead of `net.william278.husksync:husksync-PLATFORM`. 
* HuskSync versions prior to `v2.2.5` are distributed on [JitPack](https://jitpack.io/#/net/william278/HuskSync), and you will need to use the `https://jitpack.io` repository instead.
</details>

## Table of Contents
1. [API Introduction](#api-introduction)
    1. [Setup with Maven](#11-setup-with-maven)
    2. [Setup with Gradle](#12-setup-with-gradle)
2. [Adding HuskSync as a dependency](#2-adding-husksync-as-a-dependency)
3. [Creating a class to interface with the API](#3-creating-a-class-to-interface-with-the-api)
4. [Checking if HuskSync is present and creating the hook](#4-checking-if-husksync-is-present-and-creating-the-hook)
5. [Getting an instance of the API](#5-getting-an-instance-of-the-api)
6. [CompletableFuture and Optional basics](#6-completablefuture-and-optional-basics)
7. [Next steps](#7-next-steps)

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
Add the dependency to your `pom.xml` as per below. Replace `HUSKSYNC_VERSION` with the latest version of HuskSync (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/HuskSync?color=%23282828&label=%20&style=flat-square). and `MINECRAFT_VERSION` with the version of Minecraft you want to target (e.g. `1.20.1`). A correctly formed version target should look like: `3.7+1.20.1`. Omit the plus symbol and Minecraft version if you are targeting the `common` platform.
```xml
<dependency>
    <groupId>net.william278.husksync</groupId>
    <artifactId>husksync-PLATFORM</artifactId>
    <version>HUSKSYNC_VERSION+MINECRAFT_VERSION</version>
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
Add the dependency as per below. Replace `HUSKSYNC_VERSION` with the latest version of HuskSync (without the v): ![Latest version](https://img.shields.io/github/v/tag/WiIIiam278/HuskSync?color=%23282828&label=%20&style=flat-square). and `MINECRAFT_VERSION` with the version of Minecraft you want to target (e.g. `1.20.1`). A correctly formed version target should look like: `3.7+1.20.1`. Omit the plus symbol and Minecraft version if you are targeting the `common` platform.

```groovy
dependencies {
    compileOnly 'net.william278.husksync:husksync-PLATFORM:HUSKSYNC_VERSION+MINECRAFT_VERSION'
}
```
</details>

## 2. Adding HuskSync as a dependency
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

## 3. Creating a class to interface with the API
- Unless your plugin completely relies on HuskSync, you shouldn't put HuskSync API calls into your main class, otherwise if HuskSync is not installed you'll encounter `ClassNotFoundException`s

```java
public class HuskSyncAPIHook {

    public HuskSyncAPIHook() {
        // Ready to do stuff with the API
    }

}
```
## 4. Checking if HuskSync is present and creating the hook
- Check to make sure the HuskSync plugin is present before instantiating the API hook class

```java
public class MyPlugin extends JavaPlugin {

    public HuskSyncAPIHook huskSyncAPIHook;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().getPlugin("HuskSync") != null) {
            this.huskSyncAPIHook = new HuskSyncAPIHook();
        }
    }
}
```

## 5. Getting an instance of the API
- You can now get the API instance by calling `HuskSyncAPI#getInstance()`
- If targeting the Bukkit platform, you can also use `BukkitHuskSyncAPI#getBukkitInstance()` to get the Bukkit-extended API instance (recommended)

```java
import net.william278.husksync.api.HuskSyncAPI;

public class HuskSyncAPIHook {

    private final HuskSyncAPI huskSyncAPI;

    public HuskSyncAPIHook() {
        this.huskSyncAPI = HuskSyncAPI.getInstance();
    }

}
```

## 6. CompletableFuture and Optional basics
- HuskSync's API methods often deal with `CompletableFuture`s and `Optional`s.
- A `CompletableFuture` is an asynchronous callback mechanism. The method will be processed asynchronously and the data returned when it has been retrieved. Then, use `CompletableFuture#thenAccept(data -> {})` to do what you want to do with the `data` you requested after it has asynchronously been retrieved, to prevent lag.
- An `Optional` is a null-safe representation of data, or no data. You can check if the Optional is empty via `Optional#isEmpty()` (which will be returned by the API if no data could be found for the call you made). If the optional does contain data, you can get it via `Optional#get()`.

> **Warning:** You should never call `#join()` on futures returned from the HuskSyncAPI as futures are processed on server asynchronous tasks, which could lead to thread deadlock and crash your server if you attempt to lock the main thread to process them.

### 7. Next steps
Now that you've got everything ready, you can start doing stuff with the HuskSync API!
- [[Data Snapshot API]] &mdash; Get, edit, create & delete data snapshots and update players
- [[Custom Data API]] &mdash; Register custom data types to sync your plugin's data with HuskSync
- [[API Events]] &mdash; Listen to, cancel and modify the result of data synchronization events
