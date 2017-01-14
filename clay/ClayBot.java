package examplefuncsplayer;
import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	//Channels
	static int GARDENER_CHANNEL = 3;
	static int XCOORDENEMY = 2;
	static int YCOORDENEMY=4;
	static int SHOULDCOME = 5;
	//Max number of bots
	static int GARDENER_MAX = 10;

	//Max HP of bots
	static float GARDENER_MAX_HP = 100;
	
	static int ROUND = 0;
	
	static int COME = 1;
	static int DONTCOME = 0;

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
		case SCOUT: 
			runScout();
			break;
		}
	}
	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		Direction dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
    	while (true) {
    		try {
    			if(rc.canMove(dir)) {
    				rc.move(dir);
    			} else {
    				dir = dir.rotateRightDegrees((float)(Math.random()*180));
    				tryMove(dir);
    			}
    			 Team enemy = rc.getTeam().opponent();
                 RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                 if (robots.length > 0) {
                 	//Broadcast your location for other soldiers to go to
                 	rc.broadcast(XCOORDENEMY, (int)robots[0].getLocation().x);
                 	rc.broadcast(YCOORDENEMY, (int)robots[0].getLocation().y);
                 	rc.broadcast(5, COME);
                 	if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        System.out.println("lol");
                    	rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                 }else {
                	 rc.broadcast(5, DONTCOME);
                 }
                 Clock.yield();
    		} catch (Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
    	}
	}
	static void runArchon() throws GameActionException {
		System.out.println("I'm an archon!");

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				ROUND++;
				System.out.println(ROUND);
				// Generate a random direction
				Direction dir = randomDirection();

				// Hire a gardener if enough slots in a random direction
				int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
				if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
					rc.hireGardener(dir);
					rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
				}

				//Move randomly
				//wander();

				// Broadcast archon's location for other robots on the team to know
				MapLocation myLocation = rc.getLocation();
				rc.broadcast(0,(int)myLocation.x);
				rc.broadcast(1,(int)myLocation.y);

				//Donate bullets
				if (rc.getTeamBullets() >= 10000) rc.donate(10000);

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
		MapLocation target = tryFindSpot();
		boolean settled = false;
		boolean canPlant  = true;
		int birthDay = 0;
		Direction dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				// Listen for home archon's location
				int xPos = rc.readBroadcast(0);
				int yPos = rc.readBroadcast(1);
				MapLocation archonLoc = new MapLocation(xPos,yPos);   				
				// Try finding a target by wandering around.
				if (target == null){
					if (!tryMove(dir)) {
						dir.rotateRightDegrees((float)(Math.random()*180));
						tryMove(dir);
					}
					target = tryFindSpot();
				}
				if(birthDay>5 && settled == false) {
					if(tryBuildRobot(RobotType.LUMBERJACK)) {System.out.println("it works");};
					System.out.println("wtf");
				}

				// Found a spot, try moving to it if needed.
				if (target!=null && !rc.hasMoved() && !rc.getLocation().equals(target) && !settled){
					rc.setIndicatorLine(rc.getLocation(), target, 255, 0, 0);
					if (!rc.canMove(target)){
						if (!tryMoveToLoc(target)){
							// Disintegrate if can't move to target location and no trees nearby
							// -stuck and useless
							if (rc.senseNearbyTrees(3).length==0) {
								int num = rc.readBroadcast(GARDENER_CHANNEL);
								rc.broadcast(GARDENER_CHANNEL, num-1);
								rc.disintegrate();
							}
							// Stuck, but not useless, so settles down.
							else settled = true;
						}
					}
					// Can move to target
					else rc.move(target);
				}

				// Settle into the spot if close enough to target
				if (target != null && (rc.getLocation().equals(target) || 
						rc.getLocation().isWithinDistance(target, (float) 0.5))) {
					settled = true;
				}

				// Start planting if settled
				if (settled && canPlant){
					//See if there's enough space to plant
					if (rc.senseNearbyRobots(2).length + rc.senseNearbyTrees(2).length >= 5){
						canPlant = false;
					}
					tryPlantTree(dir.opposite(), 60, 2);
				}

				// Always try to water
				tryWaterTree();

				// Randomly attempt to build a soldier or lumberjack in this direction
				if (!canPlant){
					Direction buildDirection = new Direction((float) Math.PI);
					if (rc.canBuildRobot(RobotType.SOLDIER, buildDirection) && (birthDay%8==0|birthDay%8==1|birthDay%8==2) && rc.isBuildReady()) {
						rc.buildRobot(RobotType.SOLDIER, buildDirection);
					} else if (rc.canBuildRobot(RobotType.LUMBERJACK, buildDirection) && (birthDay%8==3|birthDay%8==4) && rc.isBuildReady()) {
						rc.buildRobot(RobotType.LUMBERJACK, buildDirection);
					} else if (rc.canBuildRobot(RobotType.SCOUT, buildDirection) && (birthDay%8==5|birthDay%8==6) && rc.isBuildReady()){
						rc.buildRobot(RobotType.SCOUT,buildDirection);
					} else if (rc.canBuildRobot(RobotType.TANK, buildDirection) && (birthDay%8==7) && rc.isBuildReady()) {
						rc.buildRobot(RobotType.TANK, buildDirection);
					}
				}
				birthDay++;
				// Broadcast if near death
				checkBroadcastDeath(GARDENER_CHANNEL, GARDENER_MAX_HP);

				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();

			} catch (Exception e) {
				System.out.println("Gardener Exception");
				e.printStackTrace();
			}
		}
	}

	static void runSoldier() throws GameActionException {
		System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();
        Direction dir = new Direction(0);
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	System.out.println(rc.readBroadcast(5));
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                MapLocation broadcastLocation = null;
                // If there are some...
                if (robots.length > 0) {
                	//Broadcast your location for other soldiers to go to
                	rc.broadcast(XCOORDENEMY, (int)robots[0].getLocation().x);
                	rc.broadcast(YCOORDENEMY, (int)robots[0].getLocation().y);
                	rc.broadcast(5, COME);
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        System.out.println("lol");
                    	rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }else {
                   	 rc.broadcast(5, DONTCOME);
                }
                broadcastLocation = new MapLocation(rc.readBroadcast(XCOORDENEMY),rc.readBroadcast(YCOORDENEMY));
             // Try to move to a preexisting broadcast location, otherwise move randomly
                if (rc.readBroadcast(5)==DONTCOME){
                	if(!tryMove(dir)) {
                		dir = dir.rotateRightDegrees((float)(Math.random()*180));
                		tryMove(dir);
                	}
                } else {
                	goToDirectAvoidCollision(broadcastLocation);
                }
                
                System.out.println(rc.readBroadcast(XCOORDENEMY));
                
               
                

                // Try to move to a broadcast location, otherwise move randomly


                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
	}

	static void runLumberjack() throws GameActionException {
		System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();
        Direction dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);
                TreeInfo[] trees = rc.senseNearbyTrees();
                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);
                    
                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);
                        rc.broadcast(XCOORDENEMY, (int)robots[0].getLocation().x);
                    	rc.broadcast(YCOORDENEMY, (int)robots[0].getLocation().y);
                    	rc.broadcast(5, COME);
                    	
                    } else {
                    	rc.broadcast(5, DONTCOME);           	
                    }
                    MapLocation broadcastLocation = new MapLocation(rc.readBroadcast(XCOORDENEMY),rc.readBroadcast(YCOORDENEMY));
                    if(rc.readBroadcast(5)==COME) {
                    	goToDirectAvoidCollision(broadcastLocation);
                    }
                    else {
                    	if (!tryMove(dir)) {
                    		dir.rotateRightDegrees((float)Math.random()*180);
                    		tryMove(dir);
                    	}
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();
                }
            }
             catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
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
		return tryMove(dir,20,4);
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

		// Planting never happened, so return false.
		return false;

	}

	/**
	 * Try to find a good spot to start a patch of trees.
	 * A good spot is one that is not occupied and allows for the gardener to plant a minimum of 3 trees.
	 * 
	 */
	static MapLocation tryFindSpot() throws GameActionException{
		float circleRadius = 3 + GameConstants.GENERAL_SPAWN_OFFSET;
		Direction dir = new Direction(0);
		int checks = 1;
		while (checks <= 8){
			MapLocation endLoc = rc.getLocation().add(dir, 1 + rc.getType().sensorRadius - circleRadius);
			if (rc.onTheMap(endLoc) && !rc.isLocationOccupied(endLoc)){
				if (!rc.isCircleOccupied(endLoc, 2)) {
					return endLoc;
				}
			}
			dir = dir.rotateLeftDegrees(45);
			checks++;
		}
		return null;
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

	/**
	 * Checks the need to broadcast death, and does so if needed.
	 * 
	 * Precondition: points to a channel that records the active number of a certain type of robot.
	 * @throws GameActionException 
	 */
	static void checkBroadcastDeath(int channel, float maxHP) throws GameActionException{
		if (rc.getHealth() <= maxHP * (0.05) ) {
			int prevNumBots = rc.readBroadcast(channel);
			rc.broadcast(channel, prevNumBots - 1);
		}
	}
    static void goToDirectAvoidCollision(MapLocation dest) throws GameActionException{
    	tryMove(rc.getLocation().directionTo(dest),10,90);
    }
    static void goToDirectAvoidCollision(float x, float y) throws GameActionException {
    	MapLocation dest = new MapLocation(x,y);
    	goToDirectAvoidCollision(dest);
    }
    static boolean tryBuildRobot(RobotType robo) throws GameActionException {
    	// First, try intended direction
    	Direction dir = new Direction(0);
    			if (rc.canBuildRobot(robo, dir)) {
    				rc.buildRobot(robo, dir);
    				return true;
    			}

    			// Now try a bunch of similar angles
    			int currentCheck = 1;
    			int checksPerSide = 12;
    			int degreeOffset = 30;
    			while(currentCheck<=checksPerSide) {
    				// Try the offset of the left side
    				if(rc.canBuildRobot(robo,dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
    					rc.buildRobot(robo,dir.rotateLeftDegrees(degreeOffset*currentCheck));
    					return true;
    				}
    				// Try the offset on the right side
    				if(rc.canBuildRobot(robo,dir.rotateRightDegrees(degreeOffset*currentCheck))) {
    					rc.buildRobot(robo,dir.rotateRightDegrees(degreeOffset*currentCheck));
    					return true;
    				}
    				// No move performed, try slightly further
    				currentCheck++;
    			}
				return false;
				
    }
    static void cutPathToDestination(MapLocation mapLocations) throws GameActionException{
		TreeInfo [] nearby =rc.senseNearbyTrees(50, Team.NEUTRAL);
		
		if(rc.canMove(mapLocations)){
			rc.move(mapLocations);
		} else {
			tryMoveToLoc(mapLocations);
		}
		for(TreeInfo ti:nearby){
			if(ti.getContainedBullets()>0)
				rc.shake(ti.getLocation());
			else {
				rc.chop(ti.getLocation());
			}
		}
		if(!rc.hasAttacked()&&!rc.hasMoved()&&rc.canStrike()){
			rc.strike();
		}
	}

}


