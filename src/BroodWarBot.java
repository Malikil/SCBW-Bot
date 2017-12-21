import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class BroodWarBot extends DefaultBWListener
{
    private Mirror mirror = new Mirror();
    private Game game;
    private Player self;
    
    private AVLTree<SeenBuilding> enemyBuildings;
    private Stack<Thread> threads;
    private Unit activeUnit;
    private AVLTree<UnitTypeWrapper> unitTree;
    private Queue<Unit> oldActives;
    /**
     * <pre>
     * ---------------1(1)   - Scout Sent
     * --------------1-(2)   - Building Queued
     * -------------1--(4)   - Supply
     * ------------1---(8)   - Barracks
     * -----------1----(16)  - Engineering
     * ----------1-----(32)  - Missile Turret
     * ---------1------(64)  - Academy
     * --------1-------(128) - Bunker
     * -------1--------(256) - Factory
     * ------1---------(512) - Starport
     * -----1----------(1024)- Science
     * ----1-----------(2048)- Armory
     * ---1------------(4096)- Queued Command Centre
     * --1-------------(8192)- Queued Refinery
     * </pre>
     */
    private short flags;
    
    public void run()
    {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

	@Override
	public void onStart()
	{
	    game = mirror.getGame();
	    self = game.self();
	
	    //Use BWTA to analyze map
	    //This may take a few minutes if the map is processed first time!
	    
	    System.out.println(game.mapName());
	    System.out.println("Analyzing map...");
	    BWTA.readMap();
	    BWTA.analyze();
	    System.out.println("Map data ready");
	    
	    // "Constructor" things
	    game.enableFlag(1);
	    flags = 0;
		enemyBuildings = new AVLTree<>();
		threads = new Stack<>();
		unitTree = new AVLTree<>();
		oldActives = new Queue<>();
//		game.sendText("black sheep wall");
		
		for (Unit unit : self.getUnits())
			if (unit.getType().isWorker())
				activeUnit = unit;
	}
	

    @Override
    public void onUnitCreate(Unit unit)
    {
    	/*
		 * Called when allied unit is queued, when enemy unit is created in vision,
		 * or on first discovery of neutral unit. If the unit moves out of vision then re-enters, this event is not called again 
		 */
		if (unit.getType().isBuilding())
		{
			if ((flags / 2) % 2 == 1)
				flags -= 2;
			
			game.sendTextEx(true, "Build unit is #" + unit.getBuildUnit().getID());
			
			/*if (unit.getBuildUnit().equals(activeUnit))
				activeUnit = unitTree.get(new UnitTypeWrapper(activeUnit)).cycle();*/
		}
    }
	
	@Override
	public void onUnitComplete(Unit unit)
	{
		if (unit.getPlayer().equals(self))
		{
			if (unit.getType().isBuilding() || unit.getType().isOrganic() || unit.getType().isMechanical())
			{
				UnitTypeWrapper newUnitType = new UnitTypeWrapper(unit);
				if (unitTree.contains(newUnitType))
					unitTree.get(newUnitType).add(unit);
				else
					unitTree.add(newUnitType);
			}
			
			if (unit.getType().isWorker())
				unit.gather(getClosestMineral(unit));
		}
	}
	
	@Override
	public void onFrame()
	{
		game.drawTextScreen(400, 25, unitTree.get(new UnitTypeWrapper(UnitType.Terran_SCV)).count() + " on tree, " + self.completedUnitCount(UnitType.Terran_SCV) + " total");
	    game.drawTextScreen(10, 10, game.mapName() + " - " + game.mapFileName() + "\tPlaying as " + self.getName() + " - " + self.getRace());
	    
		// Update frame flags
		if ((self.supplyUsed() * 100) / self.supplyTotal() >= 70 &&
			self.incompleteUnitCount(UnitType.Terran_Supply_Depot) == 0)
		{
			if ((flags / 4) % 2 == 0)
				flags += 4;
		}
		else if ((flags / 4) % 2 == 1)
			flags -= 4;
		
		// Make sure the build queued flag is correct
		if ((flags / 2) % 2 == 1)
		{
			if (oldActives.size() > 0)
			{
				Queue<Unit> temp = new Queue<>();
				while (oldActives.size() > 0)
				{
					Unit active = oldActives.pop();
					if (active.isIdle())
						active.gather(getClosestMineral(active));
					else
						temp.add(active);
				}
				oldActives = temp;
				
				if (oldActives.size() == 0)
				{
					game.sendTextEx(true, "All active units are idle, resetting flag");
					flags -= 2;
				}
			}
			else
				flags -= 2;
		}

		// Show active flags
		String str = "";
		for (Unit u : oldActives)
		{
			str += "\n#" + u.getID();
		}
		game.drawTextScreen(10, 25,
				"Scout Sent - " + (flags % 2 == 1) +
				"\nBuilding Queued - " + ((flags / 2) % 2 == 1) +
				"\nBuild Supply - " + ((flags / 4) % 2 == 1) +
				"\nIncomplete Building - " + ((flags / 8) % 2 == 1) +
				"\n\n" + oldActives.size() + " old actives:" + str);
		
		// Act on each flag
		// Scout if needed
		if (flags % 2 == 0 && self.getUnits().size() > 9 &&
				!(activeUnit.isCarryingMinerals() || activeUnit.isCarryingGas()))
		{
			// Move to current base location
			BaseLocation home = BWTA.getNearestBaseLocation(activeUnit.getPosition());
			System.out.println("1");
			activeUnit.move(home.getRegion().getChokepoints().get(0).getCenter());
			System.out.println("2");
			for (BaseLocation b : BWTA.getStartLocations())
				if (!b.equals(home))
					activeUnit.move(b.getPosition(), true);
			System.out.println("3");
			activeUnit.move(home.getPosition(), true);
			System.out.println("4");
			activeUnit.gather(getClosestMineral(activeUnit), true);
			game.sendTextEx(true, "Sent #" + activeUnit.getID() + " to scout");
			oldActives.add(activeUnit);
			activeUnit = unitTree.get(new UnitTypeWrapper(activeUnit)).cycle();
			game.sendTextEx(true, "Active unit changed to #" + activeUnit.getID());
			flags += 1;
		}
		
		// Build buildings if needed
		if (!activeUnit.exists())
		{
			activeUnit = unitTree.get(new UnitTypeWrapper(activeUnit)).cycle();
		}
		
		if ((flags / 2) % 2 == 0 && !activeUnit.isCarryingGas() && !activeUnit.isCarryingMinerals())
			if ((flags / 4) % 2 == 1 && self.minerals() > 100)
			{
				TilePosition buildLocation = getBuildLocation(activeUnit, UnitType.Terran_Supply_Depot);
	    		if (buildLocation != null)
	    		{
	    			game.sendTextEx(true, "Trying to build Supply Depot at " + buildLocation.toString());
	    			activeUnit.build(UnitType.Terran_Supply_Depot, buildLocation);
	    			oldActives.add(activeUnit);
	    			activeUnit = unitTree.get(new UnitTypeWrapper(activeUnit)).cycle();
	    			game.sendTextEx(true, "Active unit changed to #" + activeUnit.getID());
	    			flags += 2;
	    		}
	    		else
	    		{
	    			game.sendTextEx(true, "Build location was null, sending to minerals and cycling active");
	    			activeUnit.gather(getClosestMineral(activeUnit));
	    			activeUnit = unitTree.get(new UnitTypeWrapper(activeUnit)).cycle();
	    		}
			}
			else if ((flags / 8) % 2 == 1 && self.minerals() > 150)
			{
				TilePosition buildLocation = getBuildLocation(activeUnit, UnitType.Terran_Barracks);
				if (buildLocation != null)
				{
					game.sendTextEx(true, "Trying to build Barracks at " + buildLocation.toString());
					activeUnit.build(UnitType.Terran_Barracks, buildLocation);
					oldActives.add(activeUnit);
					activeUnit = unitTree.get(new UnitTypeWrapper(activeUnit)).cycle();
	    			game.sendTextEx(true, "Active unit changed to #" + activeUnit.getID());
	    			flags += 2;
				}
				else
				{
					game.sendTextEx(true, "Build location was null, sending to minerals and cycling active");
	    			activeUnit.gather(getClosestMineral(activeUnit));
	    			activeUnit = unitTree.get(new UnitTypeWrapper(activeUnit)).cycle();
				}
			}
		
		
		// Make sure the old active units mine after they're done
		if (oldActives.size() > 0)
		{
			Queue<Unit> temp = new Queue<>();
			while (oldActives.size() > 0)
			{
				Unit unit = oldActives.pop();
				if (unit.isIdle())
					unit.gather(getClosestMineral(unit));
				else if (unit.isGatheringMinerals() || unit.isGatheringGas() ||
						unit.isCarryingMinerals() || unit.isCarryingGas())
					continue;
				else
					temp.add(unit);
			}
			oldActives = temp;
		}
		
		// ===================================================================================================
		// ===================================================================================================
		
	    //iterate through my units
	    for (Unit myUnit : self.getUnits())
	    {
	        if (!myUnit.isCompleted())
	        {
	        	if (myUnit.getType().isBuilding())
	        	{
	        		if (!myUnit.getBuildUnit().exists())
	        		{
	        			unitTree.get(new UnitTypeWrapper(UnitType.Terran_SCV)).cycle().rightClick(myUnit);
	        		}
	        	}
	        	else
	        		continue;
	        }
	        
	        StringBuilder debugString = new StringBuilder("#" + myUnit.getID());
	        
	        if (!(myUnit.getOrderTargetPosition().getX() == 0) || !(myUnit.getOrderTargetPosition().getY() == 0))
	        	game.drawLineMap(myUnit.getPosition(), myUnit.getOrderTargetPosition(), Color.Cyan);
	        if (!myUnit.getType().isBuilding())
	        	debugString.append(" " + myUnit.getOrder().toString());
	
			//if there's enough nearby minerals and we're not waiting for a supply depot, train an SCV
			if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50 && (flags / 4) % 2 == 0)
			{
				int minerals = 0;
				int scvs = 0;
				for (Unit nearby : myUnit.getUnitsInRadius(320))
				{
					if (nearby.getType().isMineralField()) minerals++;
					else if (nearby.getType().isWorker()) scvs++;
				}
				game.drawCircleMap(myUnit.getPosition(), 320, Color.Black);
				debugString.append("\nMinerals: " + minerals + "\nSCVs: " + scvs);
				if ((double)scvs / minerals < 1.7 && myUnit.getTrainingQueue().size() < 1)
					myUnit.train(UnitType.Terran_SCV);
			}
	
	        if (myUnit.getType().isWorker())
	        {
	        	if (myUnit.equals(activeUnit))
	        	{
	        		debugString.append("\nActive Unit");
	        		game.drawCircleMap(myUnit.getPosition(), 20, Color.Orange);
	        	}
	        }
	        game.drawTextMap(myUnit.getPosition(), debugString.toString());
	    }
	    
	    String s = "Known enemy buildings:\n";
		for (SeenBuilding u : enemyBuildings)
		{
			// May as well check here for if the space is visible, and the building isn't there
			if (game.isVisible(u.getPosition().toTilePosition()))
				if (!u.getUnit().exists())
				{
					game.sendTextEx(true, "Couldn't find " + u.getUnit().getType() + " at " + u.getPosition().toTilePosition());
					enemyBuildings.delete(u);
				}
			s += u.getType() + "#" + u.getID() + " " + u.getPosition().toTilePosition() + "\n";
			game.drawTextMap(u.getPosition(), u.getType() + "#" + u.getID());
		}
		game.drawTextScreen(150, 25, s);
		
		for (BaseLocation b : BWTA.getBaseLocations())
	    {
	    	Position last = null;
	    	Position first = null;
	    	for (Position p : b.getRegion().getPolygon().getPoints())
	    	{
	    		if (first == null)
	    			first = p;
	    		if (last != null)
	    			game.drawLineMap(last, p, Color.Green);
	    		last = p;
	    	}
	    	game.drawLineMap(last, first, Color.Green);
	    	s = "";
	    	s += b.getMinerals().size() + " Minerals: " + b.minerals() + "\n";
	    	s += b.getGeysers().size() + " Gas: " + b.gas();
	    	game.drawTextMap(b.getRegion().getCenter(), s);
	    }
	}
	
	private Unit getClosestMineral(Unit unit)
	{
		Unit closestMineral = null;
		for (Unit neutral : game.neutral().getUnits())
		{
			if (neutral.getType().isMineralField())
			{
				if (closestMineral == null)
					closestMineral = neutral;
				else if (unit.getDistance(closestMineral) > unit.getDistance(neutral))
					closestMineral = neutral;
			}
		}
		return closestMineral;
	}
	
	private TilePosition getBuildLocation(Unit unit, UnitType building)
	{
		BaseLocation base = BWTA.getNearestBaseLocation(unit.getPosition());
		if (building.isRefinery())
			return base.getGeysers().get(0).getTilePosition();
		else
		{
			boolean insideBase = true;
			TilePosition unitTile = unit.getTilePosition();
			for (int radius = 1; insideBase; radius++)
			{
				insideBase = false;
				TilePosition current;
				int x = -radius;
				int y = -radius;
				for (;x < radius; x++)
				{
					current = new TilePosition(unitTile.getX() + x, unitTile.getY() + y);
					if (base.getRegion().getPolygon().isInside(current.toPosition()))
						insideBase = true;
					else
						continue;
					
					if (game.canBuildHere(current, building))
					{
						if (game.canBuildHere(new TilePosition(current.getX() - 1, current.getY() - 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() + 1, current.getY() - 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() - 1, current.getY() + 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() + 1, current.getY() + 1), building))
						{
							return current;
						}
					}
				}
				for (;y < radius; y++)
				{
					current = new TilePosition(unitTile.getX() + x, unitTile.getY() + y);
					if (base.getRegion().getPolygon().isInside(current.toPosition()))
						insideBase = true;
					else
						continue;
					
					if (game.canBuildHere(current, building))
					{
						if (game.canBuildHere(new TilePosition(current.getX() - 1, current.getY() - 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() + 1, current.getY() - 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() - 1, current.getY() + 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() + 1, current.getY() + 1), building))
						{
							return current;
						}
					}
				}
				for (;x > -radius; x--)
				{
					current = new TilePosition(unitTile.getX() + x, unitTile.getY() + y);
					if (base.getRegion().getPolygon().isInside(current.toPosition()))
						insideBase = true;
					else
						continue;
					
					if (game.canBuildHere(current, building))
					{
						if (game.canBuildHere(new TilePosition(current.getX() - 1, current.getY() - 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() + 1, current.getY() - 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() - 1, current.getY() + 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() + 1, current.getY() + 1), building))
						{
							return current;
						}
					}
				}
				for (;y > -radius; y--)
				{
					current = new TilePosition(unitTile.getX() + x, unitTile.getY() + y);
					if (base.getRegion().getPolygon().isInside(current.toPosition()))
						insideBase = true;
					else
						continue;
					
					if (game.canBuildHere(current, building))
					{
						if (game.canBuildHere(new TilePosition(current.getX() - 1, current.getY() - 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() + 1, current.getY() - 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() - 1, current.getY() + 1), building) &&
							game.canBuildHere(new TilePosition(current.getX() + 1, current.getY() + 1), building))
						{
							return current;
						}
					}
				}
			}
		}
		
		return null;
	}
	
	@Override
	public void onUnitDiscover(Unit unit)
	{
		/*
		 * Called whenever any unit enters vision
		 */
		if (unit.getType().isBuilding() && unit.getPlayer().isEnemy(self))
		{
			// Maintain a list of enemy buildings so we can come back to them
			SeenBuilding foundBuilding = new SeenBuilding(unit);
			if (!enemyBuildings.contains(foundBuilding))
			{
				enemyBuildings.add(foundBuilding);
				game.sendTextEx(true, "Found new " + foundBuilding.getType() + " at " + foundBuilding.getPosition().toTilePosition());
				game.sendTextEx(true, "Now know of " + enemyBuildings.size() + " enemy buildings");
    		}
    	}
    }
	
	@Override
	public void onEnd(boolean isWinner)
	{
		while (threads.size() > 0)
		{
			Thread current = threads.pop();
			if (current.isAlive())
				current.interrupt();
		}
	}

    public static void main(String[] args) {
        new BroodWarBot().run();
    }
}