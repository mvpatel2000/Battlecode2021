package scoutplayer;

import battlecode.common.*;

public class EnlightmentCenter extends Robot {

    static final RobotType[] spawnableRobot = {
        RobotType.POLITICIAN,
        RobotType.SLANDERER,
        RobotType.MUCKRAKER,
    };

    RelativeMap map;
    ScoutTracker st;

    public EnlightmentCenter(RobotController rc) throws GameActionException {
        super(rc);
        map = new RelativeMap();
        st = null;
    }

    @Override
    public void run() throws GameActionException {
        super.run();
        if (st == null) {
            if (rc.canBuildRobot(RobotType.POLITICIAN, Direction.EAST, 1)) {
                rc.buildRobot(RobotType.POLITICIAN, Direction.EAST, 1);
                MapLocation spawnLoc = rc.getLocation().add(Direction.EAST);
                System.out.println("Built Politician at " + spawnLoc.toString());
                int scoutID = rc.senseRobotAtLocation(spawnLoc).ID;
                st = new ScoutTracker(rc, scoutID, spawnLoc, map);
            }
        } else {
            st.update();
        }
    }

    /**
     * Returns a random spawnable RobotType
     *
     * @return a random RobotType
     */
    static RobotType randomSpawnableRobotType() {
        return spawnableRobot[(int) (Math.random() * spawnableRobot.length)];
    }
}