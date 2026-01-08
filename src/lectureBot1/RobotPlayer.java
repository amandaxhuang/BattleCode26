package lectureBot1;

import java.util.Random;

import battlecode.common.*;


public class RobotPlayer {

    public static enum State{
            INITIALIZE,
            FIND_CHEESE,
            RETURN_TO_KING,
            BUILD_TRAPS,
            EXPLORE_AND_ATTACK,
    }

    public static Direction[] directions = Direction.values();
    public static Random rand = new Random(1092);
    public static State currentState = State.INITIALIZE;

    public static int turnsSinceCarry = 1000;

    public static void run(RobotController rc) throws GameActionException {
        while (true){
            try{
                if (rc.getType().isRatKingType()){
                    runRatKing(rc);
                } else {
                    turnsSinceCarry ++;
                    switch (currentState) {
                        case INITIALIZE:
                            if (rc.getRoundNum() < 30 || rc.getCurrentRatCost() <= 10){
                                currentState = State.FIND_CHEESE;
                            } else {
                                currentState = State.EXPLORE_AND_ATTACK;
                            }
                            break;
                        case FIND_CHEESE:
                            runFindCheese(rc);
                            break;
                        case RETURN_TO_KING:
                            runReturnToKing(rc);
                            break;
                        case BUILD_TRAPS:
                            runBuildTraps(rc);
                            break;
                        case EXPLORE_AND_ATTACK:
                            runExploreAndAttack(rc);
                            break;

                    }
                }
            } catch (GameActionException e) {
                System.out.println("GameActionException");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }

    public static void moveRandom(RobotController rc) throws GameActionException {
        MapLocation forwardLoc = rc.adjacentLocation(rc.getDirection());

        if (rc.canRemoveDirt(forwardLoc)){
            rc.removeDirt(forwardLoc);
        }

        if (rc.canMoveForward()) {
                rc.moveForward();
        } else {
            Direction random = directions[rand.nextInt(directions.length)];
            rc.turn(random);
        }
    }

    public static void runRatKing(RobotController rc) throws GameActionException {
        int currentCost = rc.getCurrentRatCost();

        if (currentCost <= 10 || rc.getAllCheese() > currentCost + 2500){
            MapLocation[] potentialSpawLocations = rc.getAllLocationsWithinRadiusSquared(rc.getLocation(), 8);

            for (MapLocation loc : potentialSpawLocations) {
                if (rc.canBuildRat(loc)) {
                    rc.buildRat(loc);
                    break;
                }
            }
        }

        moveRandom(rc);

        rc.writeSharedArray(0, rc.getLocation().x);
        rc.writeSharedArray(1, rc.getLocation().x);

    }

    public static void runFindCheese(RobotController rc) throws GameActionException {
        MapInfo[] nearbyInfos = rc.senseNearbyMapInfos();

        for (MapInfo info: nearbyInfos){
            if (info.getCheeseAmount() > 0){
                Direction toCheese = rc.getLocation().directionTo(info.getMapLocation());
                if (rc.canTurn()) {
                    rc.turn(toCheese);
                    break;
                }
            }
        }

        for (Direction dir:directions) {
            MapLocation loc = rc.getLocation().add(dir);
            if (rc.canPickUpCheese(loc)) {
                rc.pickUpCheese(loc);
                if (rc.getRawCheese() >= 10) {
                    currentState = State.RETURN_TO_KING;
                }
            }
        }
    }

    public static void runReturnToKing(RobotController rc) throws GameActionException {
        MapLocation kingLoc = new MapLocation(rc.readSharedArray(0), rc.readSharedArray(1));
        Direction toKing = rc.getLocation().directionTo(kingLoc);
        MapLocation nextLoc = rc.getLocation().add(toKing);

        if (rc.canTurn()){
            rc.turn(toKing);
        }

        if (rc.canRemoveDirt(nextLoc)){
            rc.removeDirt(nextLoc);
        }

        if (rc.canMove(toKing)){
            rc.move(toKing);
        }

        int rawCheese = rc.getRawCheese();

        if (rawCheese == 0){
            currentState = State.FIND_CHEESE;
        }

        if (rc.canSenseLocation(kingLoc)){
            RobotInfo[] kingLocations = rc.senseNearbyRobots(kingLoc,8, rc.getTeam());
            for (RobotInfo robotInfo: kingLocations){
                if (robotInfo.getType().isRatKingType()){
                    MapLocation actualKingLoc = robotInfo.getLocation();

                    if (rc.canTransferCheese(actualKingLoc, rawCheese)){
                        rc.transferCheese(actualKingLoc, rawCheese);
                        currentState = State.FIND_CHEESE;
                    }

                    break;
                }
            }
        }
    }

    public static void runBuildTraps(RobotController rc) throws GameActionException{

        for (Direction dir: directions){
            MapLocation loc = rc.getLocation().add(dir);
            boolean catTraps = rand.nextBoolean();

            if (catTraps && rc.canPlaceCatTrap(loc)){
                rc.placeCatTrap(loc);
            } else if (rc.canPlaceRatTrap(loc)){
                rc.placeRatTrap(loc);
            }

        }

        if (rand.nextDouble() < 0.1) {
            currentState = State.EXPLORE_AND_ATTACK;
        }
    }

    public static void runExploreAndAttack(RobotController rc) throws GameActionException{

        moveRandom(rc);
        if (rc.canThrowRat() && turnsSinceCarry >= 3){
            rc.throwRat();
        }

        for (Direction dir: directions) {
            MapLocation loc = rc.getLocation().add(dir);

            if (rc.canCarryRat(loc)) {
                rc.carryRat(loc);
                turnsSinceCarry = 0;
            }

            if (rc.canAttack(loc)){
                rc.attack(loc);
            }
        }

        if (rand.nextDouble() < 0.1) {
            currentState = State.BUILD_TRAPS;
        }
    }

}
