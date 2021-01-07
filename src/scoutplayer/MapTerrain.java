package scoutplayer;

import battlecode.common.*;

public class MapTerrain {

    public MapLocation loc;
    public double pa;
   
    public MapTerrain(MapLocation location, double passability) {
        this.loc = location;
        this.pa = passability;
    }
    
}
