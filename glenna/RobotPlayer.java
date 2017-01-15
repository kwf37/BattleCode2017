package glennabott;
import battlecode.common.*;

public strictfp class RobotPlayer {
	static RobotController rc;

	//Channels
		static int GARDENER_CHANNEL = 3;
		static int XCOORDENEMY = 2;
		static int YCOORDENEMY = 4;
		static int SHOULDCOME = 5;
		static int PRODUCTIONCHANNEL = 6;
		//Max number of bots
		static int GARDENER_MAX = 30;

		//Max HP of bots
		static float GARDENER_MAX_HP = 100;
		
		static int ROUND = 0;
		
		static int COME = 1;
		static int DONTCOME = 0;
		static int SHOULDPRODUCE = 1;
		static int DONTPRODUCE = 0;
		static int SETTLERADIUS = 4;

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
		case TANK:
			runTank();
			break;
		}
	}
	static void runScout() throws GameActionException {
		System.out.println("I'm a scout!");
		Direction dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
    	while (true) {
    		try {
    			broadcastIfSenseEnemy();
    			if(rc.canMove(dir)) {
    				rc.move(dir);
    			} else {
    				dir = dir.rotateRightDegrees((float)(Math.random()*180));
    				tryMove(dir);
    			}
    			if(rc.senseNearbyTrees().length>0) {
    				shakeNearbyTrees();
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
	static void runTank() throws GameActionException {
		System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();
        Direction dir = new Direction(0);
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
            	broadcastIfSenseEnemy();
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
	static void runArchon() throws GameActionException {
		System.out.println("I'm an archon!");

		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				broadcastIfSenseEnemy();
				int numGard = rc.readBroadcast(3);
				rc.broadcast(3, 0);
				// Generate a random direction
				Direction dir = randomDirection();

				// Hire a gardener if enough slots in a random direction
				int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
				if (numGard > 15 && numGard < GARDENER_MAX && rc.getTeamBullets() > 500 && rc.canHireGardener(dir)) {
					rc.hireGardener(dir);
					rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
				}
				if (numGard <= 10 && numGard < GARDENER_MAX && rc.canHireGardener(dir)) {
					rc.hireGardener(dir);
					rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
				}

				//Move randomly
				//wander();

				// Broadcast archon's location for other robots on the team to know
				MapLocation myLocation = rc.getLocation();
				rc.broadcast(0,(int)myLocation.x);
				rc.broadcast(1,(int)myLocation.y);

				//Donate bullets, see if can win
				if (rc.getTeamBullets() >= 500) rc.donate(100);
				canWin();
				
				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
				

			} catch (Exception e) {
				System.out.println("Archon Exception");
				e.printStackTrace();
				
			}
		}
	}

	static void runGardener() throws GameActionException {
		int birthDay = 0;
		System.out.println("I'm a gardener!");
		MapLocation target = tryFindSpot();
		boolean settled = false;
		MapLocation[] enemy= rc.getInitialArchonLocations(rc.getTeam().opponent());
		Direction dir = rc.getLocation().directionTo(rc.getInitialArchonLocations(rc.getTeam().opponent())[0]);
		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				Direction directionToArchon = rc.getLocation().directionTo(enemy[0]);
				rc.broadcast(3, rc.readBroadcast(3)+1);
				// Listen for home archon's location
				int xPos = rc.readBroadcast(0);
				int yPos = rc.readBroadcast(1);
				MapLocation archonLoc = new MapLocation(xPos,yPos);   
				
				if (settled == false) {
					if (!tryMove(dir)) {
						dir = dir.rotateRightDegrees((float)(Math.random()*180));
						tryMove(dir);
					}
					settled = settleDown(directionToArchon);
				} 
				

				/*// Try finding a target by wandering around.
				if (target == null){
					wander();
					target = tryFindSpot();
				}

				// Found a spot, try moving to it if needed.
				if (target != null && !rc.hasMoved() && !rc.getLocation().equals(target) && !settled){
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
				}*/

				// Settle into the spot if close enough to target
/*				if (target != null && (rc.getLocation().equals(target) || 
						rc.getLocation().isWithinDistance(target, (float) 0.5))) {
					settled = true;
				}*/

				// Start planting if settled by using inital enemy archon loc
				if (settled){
					tryPlantTree(directionToArchon.opposite(), 60, 2);
				}

				// Always try to water
				tryWaterTree();
				if (birthDay > 5 && !settled) {
					hireLumberjacks(directionToArchon);
				}
				// Attempt to build a soldier or lumberjack in this direction
				if (settled){
					hireLumberjacks(directionToArchon);
					if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length>0) {
						tryBuildRobot(RobotType.SOLDIER, directionToArchon);
					} else if (rc.readBroadcast(PRODUCTIONCHANNEL)>0) {
						produceRandom(directionToArchon);
					}
				}
				if (rc.senseNearbyRobots(-1, rc.getTeam().opponent()).length>0) {
					tryBuildRobot(RobotType.SOLDIER, directionToArchon);
				}
				rc.broadcast(PRODUCTIONCHANNEL, DONTPRODUCE);
				// Broadcast if near death
				//checkBroadcastDeath(GARDENER_CHANNEL, GARDENER_MAX_HP);
				birthDay++;
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
            	broadcastIfSenseEnemy();
            	System.out.println(rc.readBroadcast(5));
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                MapLocation broadcastLocation = null;
                // If there are some...
                if (rc.readBroadcast(5) == DONTCOME) {
	                if (robots.length > 0) {
	                	//Broadcast your location for other soldiers to go to
	                	rc.broadcast(XCOORDENEMY, (int)robots[0].getLocation().x);
	                	rc.broadcast(YCOORDENEMY, (int)robots[0].getLocation().y);
	                	rc.broadcast(5, COME);
	                    // And we have enough bullets, and haven't attacked yet this turn...
	                	strafeAndShoot(robots[0]);
	                }
                } else {
                	if(robots.length == 0) {
                		rc.broadcast(5, DONTCOME);
                	}
                }
                
                broadcastLocation = new MapLocation(rc.readBroadcast(XCOORDENEMY),rc.readBroadcast(YCOORDENEMY));
             // Try to move to a preexisting broadcast location, otherwise move randomly
	         if (!rc.hasMoved()){
                	if (rc.readBroadcast(5)==DONTCOME){
	                	if(!tryMove(dir)) {
	                		dir = dir.rotateRightDegrees((float)(Math.random()*180));
	                		tryMove(dir);
	                	}
	                } else {
	                	goToDirectAvoidCollision(broadcastLocation);
	                }
	         }    
               
                

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
		Direction dir = new Direction(0);
		// The code you want your robot to perform every round should be in this loop
		while (true) {

			// Try/catch blocks stop unhandled exceptions, which cause your robot to explode
			try {
				broadcastIfSenseEnemy();
				// See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
				RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

				if(robots.length > 0 && !rc.hasAttacked()) {
					// Use strike() to hit all nearby robots!
					rc.strike();
				} else {
					// No close robots, so search for robots within sight radius
					robots = rc.senseNearbyRobots(-1,enemy);

					// If there is a robot, move towards it
					if (robots.length > 0) {
						MapLocation myLocation = rc.getLocation();
						MapLocation enemyLocation = robots[0].getLocation();
						Direction toEnemy = myLocation.directionTo(enemyLocation);

						tryMove(toEnemy);
					}else if (rc.senseNearbyTrees(-1, rc.getTeam().NEUTRAL).length>0) {
						TreeInfo[] trees = rc.senseNearbyTrees();
						Direction toTree = rc.getLocation().directionTo(trees[0].getLocation());
						tryMove(toTree);
						for (TreeInfo t: trees){
							if (t.getTeam() == Team.NEUTRAL && rc.canChop(t.getID())) 
								rc.chop(t.getID());
					} 
				}
				
				if(!rc.hasAttacked() && !rc.hasMoved())
					// Move Randomly
					if(!tryMove(dir)) {
                		dir = dir.rotateRightDegrees((float)(Math.random()*180));
                		tryMove(dir);
                	}




				// Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
				Clock.yield();
				}
			} catch (Exception e) {
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
				if (!rc.isCircleOccupiedExceptByThisRobot(endLoc, 2)) {
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
			if (rc.canWater(t.getID()) && t.getHealth() < 45){
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
		if (rc.getHealth() <= maxHP * (0.01) ) {
			int prevNumBots = rc.readBroadcast(channel);
			rc.broadcast(channel, prevNumBots - 1);
		}
	}
	
	/**
	 * Hires lumberjacks when there are nearby neutral trees
	 * 
	 * @throws GameActionException
	 */
	static void hireLumberjacks(Direction dir) throws GameActionException {
		TreeInfo[] nearby = rc.senseNearbyTrees(-1, Team.NEUTRAL);
		if (nearby.length > 0) {
			if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && rc.isBuildReady())
				rc.buildRobot(RobotType.LUMBERJACK, dir);
		}
	}
	
	/**
	 * Returns true and immediately donates all bullets if we can win in that
	 * turn. Returns false otherwise
	 * 
	 * @return
	 * @throws GameActionException
	 */
	static boolean canWin() throws GameActionException {
		float difference = 1000 - rc.getTeamVictoryPoints();
		if ((rc.getTeamBullets() / 10) >= difference) {
			rc.donate(rc.getTeamBullets());
			return true;
		} else
			return false;
	}
    static void goToDirectAvoidCollision(MapLocation dest) throws GameActionException{
    	tryMove(rc.getLocation().directionTo(dest),10,90);
    }
    static void goToDirectAvoidCollision(float x, float y) throws GameActionException {
    	MapLocation dest = new MapLocation(x,y);
    	goToDirectAvoidCollision(dest);
    }
    static boolean tryBuildRobot(RobotType robo, Direction dir) throws GameActionException {
    	// First, try intended direction
    			if (rc.canBuildRobot(robo, dir)) {
    				rc.buildRobot(robo, dir);
    				return true;
    			}

    			// Now try a bunch of similar angles
    			int currentCheck = 1;
    			int checksPerSide = 15 ;
    			int degreeOffset = 12;
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
    static void produceRandom(Direction dir) throws GameActionException {
		int choose = (int) (Math.random() * 99);
		if (choose < 10 && rc.canBuildRobot(RobotType.SCOUT, dir))
			tryBuildRobot(RobotType.SCOUT, dir);
		else if (choose < 50 && rc.canBuildRobot(RobotType.SOLDIER, dir))
			tryBuildRobot(RobotType.SOLDIER, dir);
		else if (choose < 75 && rc.canBuildRobot(RobotType.LUMBERJACK, dir))
			tryBuildRobot(RobotType.LUMBERJACK, dir);
		else if (rc.canBuildRobot(RobotType.TANK, dir)) {
			tryBuildRobot(RobotType.TANK ,dir);
		}
    }
    static boolean shakeNearbyTrees() throws GameActionException{
    	TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
    	
    	int randomIndex = (int)(Math.random()*nearbyTrees.length);
    	System.out.println(randomIndex);
    	for (TreeInfo trees: nearbyTrees) {
    		if(rc.canShake(trees.getLocation())) {
    			System.out.println("shaking");
    			rc.shake(trees.getLocation());
    			return true;
    		}
    	}
    	return false;
    }
    static boolean settleDown(Direction dir) throws GameActionException {
    	
    	float howfaraway = 5 + GameConstants.GENERAL_SPAWN_OFFSET;
    	System.out.println(howfaraway);
    	if (rc.senseNearbyRobots(howfaraway, rc.getTeam()).length > 0) {
    		return false;
    	}
    	/*for (int i = 0; i<7; i++) {
    		if (!rc.canPlantTree((dir.rotateRightDegrees(i*60)))) {
    			return false;
    		}
    	}*/
    	return !rc.isCircleOccupiedExceptByThisRobot(rc.getLocation(), SETTLERADIUS);
    }
    static boolean broadcastIfSenseEnemy() throws GameActionException {
    	if(rc.senseNearbyRobots().length>0) {
    		rc.broadcast(PRODUCTIONCHANNEL, rc.readBroadcast(PRODUCTIONCHANNEL)+1);
    		return true;
    	} 
    	return false;
    }
    static void strafeAndShoot(RobotInfo enemy) throws GameActionException {
		Direction dir = rc.getLocation().directionTo(enemy.getLocation());
		if (!tryMove(dir.rotateLeftDegrees(90)))
			tryMove(dir.rotateRightDegrees(90));
		if(rc.canFirePentadShot()) {
			rc.firePentadShot(dir);
		} else if (rc.canFireTriadShot()) {
			rc.fireTriadShot(dir);
		} else if (rc.canFireSingleShot()) {
			rc.fireSingleShot(dir);
		}
	}

}
