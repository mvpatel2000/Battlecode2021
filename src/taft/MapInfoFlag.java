package taft;

import battlecode.common.*;

/**
 * Flag that scouts use to communicate map edges to ECs.
 */
public class MapInfoFlag extends LocationFlag {
    /**
     * Flag breakdown:
     * - flag schema (SCHEMA_BITS)
     * - x coordinate mod 128 (COORD_BITS)
     * - y coordinate mod 128 (COORD_BITS)
     * - map edge (MAP_EDGE_BITS)
     *
     * Total bits used: 3 + 7 + 7 + 2 = 19
     */

    final static int COORD_BITS = 7;
    final static int MAP_EDGE_BITS = 2;

    public MapInfoFlag() {
        super(true);    // init without LocationFlag schema
        setSchema(MAP_INFO_SCHEMA);
    }

    public MapInfoFlag(Direction dirToEdge, MapLocation offMapLoc) {
        super(true);    // init without LocationFlag schema
        setSchema(MAP_INFO_SCHEMA);
        writeLocation(offMapLoc);
        writeEdge(dirToEdge);
    }


    public MapInfoFlag(int flag) {
        super(flag);
    }

    public boolean writeEdge(Direction dirToEdge) {
        int edgeInt = directionToMapEdge(dirToEdge);
        if (edgeInt != -1) {
            return writeToFlag(edgeInt, MAP_EDGE_BITS);
        } else {
            return false;
        }
    }

    public Direction readEdgeDirection() {
        int edge = readFromFlag(SCHEMA_BITS + COORD_BITS + COORD_BITS, MAP_EDGE_BITS);
        return mapEdgeToDirection(edge);
    }

    public int directionToMapEdge(Direction d) {
        switch(d) {
            case NORTH:
                return 0;
            case EAST:
                return 1;
            case SOUTH:
                return 2;
            case WEST:
                return 3;
            default:
                return -1;
        }
    }

    public Direction mapEdgeToDirection(int edge) {
        switch(edge) {
            case 0:
                return Direction.NORTH;
            case 1:
                return Direction.EAST;
            case 2:
                return Direction.SOUTH;
            case 3:
                return Direction.WEST;
            default:
                return null;
        }
    }

}
