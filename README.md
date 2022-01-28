# Smite Battlecode 2021

This is team Smite's Battlecode 2021 repository.

### Previous Years

- [Battlecode 2020](https://github.com/mvpatel2000/Battlecode2020)
- [Battlecode 2019](https://github.com/mvpatel2000/Battlecode2019)
- [Battlecode 2018](https://github.com/mvpatel2000/battlecode-2018-smite)
- [Battlecode 2017](https://github.com/nthistle/battlecode-2017-segfault)

### Bots
- `scoutplayer`
    Base bot with comms and simple micro. Submitted to sprint 2.
- `fdr`
    Improved build order and revamped bot. Named after FDR because of the New Deal, and because of his cleanup after the Great Depression.
- `taft`
- `gore`
- `wbush`
    Better voting than `gore`, improved destinations, and less clogging so we can more effectively send our units to war.
- `clinton`
    Boosting our economy

### Project Structure

- `README.md`
    This file.
- `build.gradle`
    The Gradle build file used to build and run players.
- `src/`
    Player source code.
- `test/`
    Player test code.
- `client/`
    Contains the client. The proper executable can be found in this folder (don't move this!)
- `build/`
    Contains compiled player code and other artifacts of the build process. Can be safely ignored.
- `matches/`
    The output folder for match files.
- `maps/`
    The default folder for custom maps.
- `gradlew`, `gradlew.bat`
    The Unix (OS X/Linux) and Windows versions, respectively, of the Gradle wrapper. These are nifty scripts that you can execute in a terminal to run the Gradle build tasks of this project. If you aren't planning to do command line development, these can be safely ignored.
- `gradle/`
    Contains files used by the Gradle wrapper scripts. Can be safely ignored.


### Useful Commands

- `./gradlew run`
    Runs a game with the settings in gradle.properties
- `./gradlew update`
    Update to the newest version! Run every so often
- `python ./scripts/copy_bot.py bot1dir bot2dir`
    Duplicate and rename an existing bot package folder.

### Useful notes

- By convention, we use `MapLocation` for absolute locations and `int[2]` for relative locations.
