# Testing HuskSync
This is a rudimentary Python script for running a little Proxy network of servers for quickly testing HuskSync.

Run the script to spin up a Velocity proxy and a pair of Paper servers for testing HuskSync.

* Useful for development & feature testing
* Not useful for stress or integration testing.
* Only works on Windows (as it deals with bash scripts)
* Only spins up Paper servers at the moment ()

If you don't want to do this you can also run a single-server test with the various `runServer` tasks in the Bukkit/Fabric modules. 

_PRs to improve testing are welcomed with open arms & cups of tea!_

## Requirements
* Windows
* Python 3.14
* MySQL DB running locally
* Redis running locally

## How to run
1. Edit `spin_network.py` to your liking (change the MC version, add your name/UUID as a server operator)
2. Configure HuskSync to point to your local MySQL/Redis DB (edit `~/test/config.yml`)
3. Run `pip install -r requirements.txt
4. From the repository route, open terminal and run `cd ./test`, then `python3 ./spin_network.py`

## Tips
* Delete `~/test/servers` and `~/test/HuskSync` each time you want to download Paper/Velocity & re-create worlds, etc.
* Create an IntelliJ Run & Debug Python task to do this with a `Run Gradle Task before` to `clean build` the project before.