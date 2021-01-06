## Proposed Turn Flow For Non-ECs

1. Parse vision
    * Optionally update flag handlers
2. Take action
3. If robot moved:
    * Parse vision again
        * Update flag handlers
4. Set flag
    * Query flag handlers and select highest priority flag

## FlagHandlers

Each unit has some flag handlers based on the unit's purpose. For example, a cartographer politician may have a `MapTerrainFlagHandler`, which handles queueing of new map tiles and message formatting, as well as a `ECScoutFlagHandler`, which handles alerts about neutral or enemy ECs that are discovered. These handlers will update as the robot moves and/or sees new things. When queried, they will return a proposed flag for the unit to set, as well as a priority level. For example, `ECScoutFlagHandler` will suggest a flag notifying the home base of a newly discovered EC with a higher priority level than `MapTerrainFlagHandler`. This way, when there are no new ECs spotted the cartographer will send flags about the map terrain, but if the enemy base is spotted, that message takes priority.