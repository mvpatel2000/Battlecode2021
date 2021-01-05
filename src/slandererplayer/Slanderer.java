package slandererplayer;

import battlecode.common.*;

public class Slanderer extends Unit {

    public Slanderer(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        if (tryMove(randomDirection()))
            System.out.println("I moved!");
    }
}