HuskSync allows you to save and synchronize custom data through the existing versatile DataSnapshot format. This page assumes you've read the [[API]] introduction and are familiar with the aforementioned [[Data Snapshot API]]. This page discusses API implementations that target the Bukkit platform.

To do this, you create and register an implementation of a platform `Data` class (e.g., `BukkitData`) and a corresponding `Serializer` class (e.g., `BukkitSerializer`). You can then apply your custom data type to a user using the `OnlineUser#setData(Identifier, Data)` method.

> **Note:** Before you begin, consider if this is what you'd like to do. For simpler/smaller data sync tasks you may wish to consider using the PersistentDataContainer API format instead, which is a bit more portable if you decide to exit the HuskSync ecosystem.

If you'd like to have a look at an example of a data extension for HuskSync that provides serializers for Pixelmon data when running the plugin on Arclight, check out [PokeSync by GsTio86](https://github.com/GsTio86/PokeSync)!

## Table of Contents
1. [Extending the BukkitData Class](#1-extending-the-bukkitdata-class)
   1. [Implementing Adaptable](#11-implementing-adaptable) 
2. [Extending the BukkitSerializer Class](#2-extending-the-bukkitserializer-class)
3. [Identifiers and registering our Serializer](#3-identifiers--registering-our-serializer)
4. [Setting and Getting our Data to/from a User](#4-setting-and-getting-our-data-tofrom-a-user)
   1. [Persisting custom data on the DataSaveEvent](#41-persisting-custom-data-on-the-datasaveevent) 

## 1. Extending the BukkitData Class
* HuskSync provides a `Data` interface that you must implement that will represent your custom data.
* On the Bukkit platform, you should create a class that `extends` `BukkitData`. This class has method to implement: `#apply(BukkitUser, BukkitHuskSync)`, which will be called when the data needs to be applied to the player.
* You can use the `BukkitUser` class to get the `Player` object of the user. Avoid using the `BukkitHuskSync` class as this is for accessing plugin internals.

<details>
<summary>Code Example &mdash; LoginParticleData class</summary>

```java
// An example of a BukkitData class that you could use in a cosmetic plugin to store player particle data.
public class LoginParticleData extends BukkitData {
    
    private String particleId;
    private int numberOfParticles;

    public LoginParticleData(String particleId, int numberOfParticles) {
        this.particleId = particleId;
        this.numberOfParticles = numberOfParticles;
    }

    // This method is called whenever a user has their data applied.
    // If you just want to use HuskSync to sync data used elsewhere, you don't have to do anything here, of course
    @Override
    public void apply(BukkitUser user, BukkitHuskSync plugin) {
        final Player player = user.getPlayer();

        // Let's use the Bukkit API to spawn some particles when a user's data is applied (e.g. when they login).
        player.spawnParticle(Particle.valueOf(particleId), player.getLocation(), numberOfParticles);
    }

}
```
</details>

### 1.1 Implementing Adaptable
* HuskSync provides the `Adaptable` marker interface to make it easier to Serialize and Deserialize your data using Gson (needed in the next step).
* I strongly advise implementing `Adaptable`. This requires a zero-arg constructor. Note that you _cannot_ serialize proprietary data types or `final` fields using `Adaptable`.

<details>
<summary>Code Example &mdash; Adaptable LoginParticleData class</summary>

```java
// We've implemented Adaptable here to make it easier to serialize and deserialize our data using Gson.
public class LoginParticleData extends BukkitData implements Adaptable {

    private String particleId;
    private int numberOfParticles;

    public LoginParticleData(String particleId, int numberOfParticles) {
        this.particleId = particleId;
        this.numberOfParticles = numberOfParticles;
    }

    @SuppressWarnings("unused") // Suppress compiler warnings
    private LoginParticleData() {
        // This is required for the Adaptable interface so that Gson can intantiate the class when deserializing.
    }

    @Override
    public void apply(BukkitUser user, BukkitHuskSync plugin) {
        user.getPlayer().spawnParticle(Particle.valueOf(particleId), player.getLocation(), numberOfParticles);
    }

}
```
</details>

## 2. Extending the BukkitSerializer Class
* To store your `Data`, you'll need to provide a `Serializer` class that will be used to serialize and deserialize your `Data` to and from a java `String` that can later be converted to a byte array and compressed/stored by HuskSync.
  * For instance, you'd need a class like: `public class LoginParticleSerializer extends BukkitSerializer implements Serializer<LoginParticleData>` to serialize the previous example.
  * You'd then need to implement the `Serializer` interface, which has two methods: `#serialize(T)` and `#deserialize(String)`.
* If you made your class `Adaptable` as per above HuskSync also provides a `BukkitSerializer.Json<T extends Adaptable>` class which you can extend to create a simple serializer using Gson.
  * This is the recommended way of creating a serializer, though, if you're dealing with NBT data, you may wish to implement a base Serializer with your own methods.

```java
// An example of a BukkitSerializer class that you could use in a cosmetic plugin to store player particle data.
public class LoginParticleSerializer extends BukkitSerializer.Json<LoginParticleData> implements Serializer<LoginParticleData> {
    
    // We need to create a constructor that takes our instance of the API
    public LoginParticleSerializer(@NotNull HuskSyncAPI api) {
        super(api, LoginParticleData.class); // We pass the class type here so that Gson knows what class we're serializing
    }

}
```

## 3. Identifiers & registering our Serializer
* Now that we have our `Data` and `Serializer` classes, we need to register them with HuskSync.
* To do this, we register an `Identifier` with HuskSync. This is a unique identifier key for your data type.
  * Use `Identifer#from(String, String)` or `Identifier#from(Key)` to create an identifier from a namespace-value pair or an adventure `Key` object.
* Make sure that the plugin you're writing registers the serializer on every server so that HuskSync will serialize the data.

```java
// Create an identifier for our data (you may wish to store this somewhere where it can be accessed statically)
public static Identifier LOGIN_PARTICLES_ID = Identifier.from("myplugin", "login_particles");

// (...)

// Register our serializer
huskSyncAPI.registerSerializer(LOGIN_PARTICLES_ID, new LoginParticleSerializer(HuskSyncAPI.getInstance()));
```

### 3.1 Identifier dependencies
* HuskSync lets you specify a set of `Dependency` objects when creating an `Identifier`. These are used to deterministically apply data in a specific order.
* Dependencies are references to other data type identifiers. HuskSync will apply data in dependency-order; that is, it will apply the data of the dependencies before applying the data of the dependent.
* This is useful when you have data that relies on other data to be applied first; for example, if you're writing an add-on for additional modded inventory data and you need to apply the base inventory data first.
* You can specify whether a dependency is required or optional. HuskSync will not sync data of a type that has a required dependency that is missing (for instance, if it is disabled in the config, or - if provided by another plugin - has failed to register).
* Use `Identifer#from(String, String, Set<Dependency>)` or `Identifier#from(Key, Set<Dependency>)` to create an identifier with dependencies
* Dependencies can be created with `Dependency.optional(Identifier)` or `Dependency.required(Identifier)` for optional or required dependencies respectively.

## 4. Setting and getting our Data to/from a User
* Now that we've registered our `Data` and `Serializer` classes, we can set our data to a user, applying it to them.
* To do this, we use the `OnlineUser#setData(Identifier, Data)` method.
  * This method will apply the data to the user, and store the data to the plugin player custom data map, to allow the data to be retrieved later and be saved to snapshots.
* Snapshots created on servers where the data type is registered will now contain our data and synchronise between instances!

```java
// Create an identifier for our data requiring the user's location to have been set first
public static Identifier LOGIN_PARTICLES_ID = Identifier.from("myplugin", "login_particles", Set.of(Dependency.optional(Key.key("husksync", "location"))));
// We can then register this as we did previously (...)
```

```java
// Create an instance of our data
LoginParticleData loginParticleData = new LoginParticleData("FIREWORKS_SPARK", 10);

// Set the data to a player
huskSyncAPI.getUser(player).setData(LOGIN_PARTICLES_ID, loginParticleData);

// Get our data from a player
LoginParticleData loginParticleData = (LoginParticleData) huskSyncAPI.getUser(player).getData(LOGIN_PARTICLES_ID);
```

### 4.1 Persisting custom data on the DataSaveEvent
Add an EventListener to the `DataSaveEvent` and use the `#editData` consumer method to apply custom data during standard DataSaves. This will persist data to users any time the data save routine executes (on user logout, server shutdown, world save, etc.)

```java
@EventHandler
public void onDataSave(BukkitDataSaveEvent event) {
    event.editData((unpacked) -> unpacked.setData(LOGIN_PARTICLES_ID, new LoginParticleData("FIREWORKS_SPARK", 10)));
}
```
