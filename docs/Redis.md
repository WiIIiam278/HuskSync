Redis is a piece of server used for data caching and cross-server messaging. A Redis server running Redis v5.0+ is **required** in addition to a compatible [[Database]] to use HuskSync. There are a number of ways of [installing or getting a Redis server](#getting-a-redis-server).

For the best results, we recommend a Redis server with 1GB of RAM, hosted locally (on the same machine as all your other servers). If your setup has multiple machines, install Redis on the machine with your Velocity/BungeeCord/Waterfall proxy server and ensure lockstep syncing mode is in use.

## What is Redis?
[Redis](http://redis.io/) (**RE**mote **DI**ctionary **S**erver) is an open-source, in-memory data store server that can be used as a cache, message broker, streaming engine, or database.

HuskSync requires Redis and uses it for caching player data when they change server, and for pub/sub messaging to facilitate cross-server admin actions (such as the [`/invsee` command](Commands) to update a player's data on other servers). Check the [[FAQs]] for more details.

## Configuring
To configure Redis, navigate to your [`config.yml`](Config-File) file and modify the properties under `redis`.

<details>
<summary>Database options (config.yml)</summary>

```yaml
# Redis settings
redis:
  # Specify the credentials of your Redis server here. Set "password" to '' if you don't have one
  credentials:
    host: localhost
    port: 6379
    password: ''
    use_ssl: false
  # Options for if you're using Redis sentinel. Don't modify this unless you know what you're doing!
  sentinel:
    # The master set name for the Redis sentinel.
    master: ''
    # List of host:port pairs
    nodes: []
    password: ''
```
</details>

### Credentials
Enter the hostname, port, and default user password of your Redis server.

If your Redis default user doesn't have a password, leave the password field blank (`password: ''`') and the plugin will attempt to connect without a password.

### Default user password
Depending on the version of Redis you've installed, Redis may or may not set a random default user password. Please check this in your Redis server config. You can clear the password of the default user with the below command in `redis-cli`.

```bash
requirepass thepassword
user default on nopass ~* &* +@all
```

### Using Redis Sentinel
If you're using [Redis Sentinel](https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/), set this up by filling out the properties under the `sentinel` subsection.

You'll need to supply your master set name, your sentinel password, and a list of hosts/ports in the format `host:port`.

## Getting a Redis Server
HuskSync requires a Redis server. Instructions for getting Redis on different servers are detailed below. HuskSync is tested for the official Redis package, but should also work with Redis forks or other compatible software.

For the best results, we recommend a Redis server with 1GB of RAM, hosted locally (on the same machine as all your other servers). If your setup has multiple machines, install Redis on the machine with your Velocity/BungeeCord/Waterfall proxy server and ensure lockstep syncing mode is in use.

### If you're using a Minecraft server hosting provider
Please contact your host's customer support and request Redis. You can direct them to this page if you wish. Looking for a Minecraft Server host that supports Redis? We maintain a list of [server hosts which offer Redis](https://william278.net/docs/website/redis-hosts).

If your host doesn't offer Redis, you should consider whether HuskSync is the right plugin for you. If you still want to use HuskSync, you could choose to rent a cheap Redis server externally from a provider such as DigitalOcean, though note we don't recommend this as it increases the latency between your game servers and cache, which will impact syncing performance.

### Redis on Linux or macOS
You can [install Redis](https://redis.io/docs/latest/operate/oss_and_stack/install/install-redis/install-redis-on-linux/) on your distribution of Linux. Redis is widely available on most package manager repositories.

You can also [install Redis](https://redis.io/docs/latest/operate/oss_and_stack/install/install-redis/install-redis-on-mac-os/) on your macOS server.

### Redis on Windows
Redis isn't officially supported on Windows, but there's a number of [unofficial ports](https://github.com/tporadowski/redis/releases) you can install which work great and run Redis as a Windows service.

You can also [install Redis via WSL](https://redis.io/docs/latest/operate/oss_and_stack/install/install-redis/install-redis-on-windows/) if you prefer.

### Pterodactyl / Pelican panel hosts
If you're self-hosting your server on a Pterodactyl or Pelican panel, you will already have Redis installed and can use this server for HuskSync, too.

If you are hosting your Redis server on the same node as your servers, you need to use `172.18.0.1` as your host (or equivalent if you changed your network settings), and bind it in the Redis config `nano /etc/redis/redis.conf`.

You will also need to uncomment the `requirepass` directive and set a password to allow outside connections, or disable `protected-mode`. Once a password is set and Redis is restarted `systemctl restart redis`, you will also need to update the password in your pterodactyl `.env` (`nano /var/www/pterodactyl/.env`) and refresh the cache `cd /var/www/pterodactyl && php artisan config:clear`.

You may also need to allow connections from your firewall depending on your Linux distribution.