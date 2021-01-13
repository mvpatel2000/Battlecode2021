package smite;
import battlecode.common.*;

public strictfp class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        Robot robot;

        // Get Robot Type
        switch(rc.getType()) {
            case ENLIGHTENMENT_CENTER:
                robot = new EnlightmentCenter(rc);
                break;
            case POLITICIAN:
                robot = new Politician(rc);
                break;
            case SLANDERER:
                robot = new Slanderer(rc);
                break;
            case MUCKRAKER:
                robot = new Muckraker(rc);
                break;
            default:
                //System.out.println\(rc.getType() + " is not supported.");
                return;
        }

        // Loop and call robot.run() each time
        while (true) {
            try {
                robot.run();
                Clock.yield();
            } catch (Exception e) {
                //System.out.println\(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }
}
