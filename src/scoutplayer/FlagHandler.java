package scoutplayer;

import battlecode.common.*;

public abstract class FlagHandler {

    Robot robot;

    public FlagHandler(Robot r) throws GameActionException {
        robot = r;
    }

    public void update(boolean beforeMove) {

    }

    public int query() {
        return 0;
    }
}