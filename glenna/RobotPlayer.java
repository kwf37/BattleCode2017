package glenna;
import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    
    //Channels
    static int GARDENER_CHANNEL = 3;
    
    static int GARDENER_MAX = 10;
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
        }
	}

    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection();

                // Hire a gardener if enough slots
                int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
                    rc.hireGardener(dir);
                    rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                }
                
                //Move randomly
                wander();

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0,(int)myLocation.x);
                rc.broadcast(1,(int)myLocation.y);

                //Donate bullets
                if (rc.getTeamBullets() >= 1000) rc.donate(100);
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);   
                
                //Try watering a tree and move to the closest sensed one if it doesn't water.
                if (!tryWaterTree()){
                	TreeInfo[] trees = rc.senseNearbyTrees(-1, rc.getTeam());
                	if (trees.length != 0){
                		MapLocation me = rc.getLocation();
                		TreeInfo closest = trees[0];
                		float min = me.distanceTo(closest.getLocation());
                		for (int i = 1; i < trees.length; i++){
                			float distance = me.distanceTo(trees[i].getLocation());
                			if (distance < min){
                				min = distance;
                				closest = trees[i];
                			}
                		}
                		tryMoveToLoc(closest.getLocation());
                	}
                }
                
                //Try planting a tree, else wander
                if (!tryPlantTree(randomDirection(), 45, 4) && !rc.hasMoved()) 
                	wander();
                
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        
    }

    static void runLumberjack() throws GameActionException {
        
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
    
    static void wander() throws GameActionException {
        try {
            Direction dir = randomDirection();
            tryMove(dir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /** 
     * Try to plant a tree in multiple directions; if unable, return false.
     * 
     */
    static boolean tryPlantTree(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {
    	// First, try intended direction
        if (rc.canPlantTree(dir)) {
            rc.plantTree(dir);
            return true;
        }

        // Now try a bunch of similar angles
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canPlantTree(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.plantTree(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(rc.canPlantTree(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.plantTree(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    	
    }
    
    /**
     * Try to water a nearby tree if it needs watering.
     * 
     */
    static boolean tryWaterTree() throws GameActionException{
    	TreeInfo[] trees = rc.senseNearbyTrees(-1, rc.getTeam());
        for (TreeInfo t : trees){
        	if (rc.canWater(t.getID()) && t.getHealth() < 30){
        		rc.water(t.getID());
        		System.out.println("watered!");
        		return true;
        	}  	
        }
        return false;
    }
    
    /**
     * Attempts to move towards a given location that contains an object,
     * while avoiding small obstacles direction in the path.
     * 
     */
    static boolean tryMoveToLoc(MapLocation loc) throws GameActionException {
    	Direction dir = rc.getLocation().directionTo(loc);
        return tryMove(dir);
    }
}
