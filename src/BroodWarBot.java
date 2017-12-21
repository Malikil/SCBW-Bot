import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class BroodWarBot extends DefaultBWListener
{
    private Mirror mirror = new Mirror();
    private Game game;
    private Player self;
    
    private AVLTree<SeenBuilding> enemyBuildings;
    private short flags;
    private Stack<Thread> threads;
    
    public void run()
    {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit)
    {
    	/*
		 * Called when allied unit is queued, when enemy unit is created in vision,
		 * or on first discovery of neutral unit. If the unit moves out of vision then re-enters, this event is not called again 
		 */
		
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
	    
	    game.enableFlag(1);
	    flags = 0;
		enemyBuildings = new AVLTree<>();
		threads = new Stack<>();
	}
	@Override
	public void onFrame()
	{
	    //game.setTextSize(10);
	    game.drawTextScreen(10, 10, game.mapName() + " - " + game.mapFileName() + "\tPlaying as " + self.getName() + " - " + self.getRace());
	
	    StringBuilder units = new StringBuilder("My units:\n");
	
	    //iterate through my units
	    for (Unit myUnit : self.getUnits())
	    {
	        units.append(myUnit.getType()).append("#").append(myUnit.getID()).append(" ").append(myUnit.getTilePosition()).append("\n");
	        
	        if (!myUnit.isCompleted())
	        	continue;
	        
	        if (!(myUnit.getOrderTargetPosition().getX() == 0) || !(myUnit.getOrderTargetPosition().getY() == 0))
	        	game.drawLineMap(myUnit.getPosition(), myUnit.getOrderTargetPosition(), Color.Cyan);
	        if (!myUnit.getType().isBuilding())
	        	game.drawTextMap(myUnit.getPosition(), myUnit.getOrder().toString());
	
			//if there's enough nearby minerals, train an SCV
			if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50)
			{
				int minerals = 0;
				int scvs = 0;
				for (Unit nearby : myUnit.getUnitsInRadius(320))
				{
					if (nearby.getType().isMineralField()) minerals++;
					else if (nearby.getType().isWorker()) scvs++;
				}
				game.drawCircleMap(myUnit.getPosition(), 320, Color.Black);
				game.drawTextMap(myUnit.getPosition(), "Minerals: " + minerals + "\nSCVs: " + scvs);
				if (scvs / minerals < 2 && myUnit.getTrainingQueue().size() < 2)
					myUnit.train(UnitType.Terran_SCV);
			}
	
	        //if it's a worker and it's idle, send it to the closest mineral patch
	        if (myUnit.getType().isWorker())
	        	if (myUnit.isIdle())
	        	{
		            Unit closestMineral = null;
		
					//find the closest mineral
					for (Unit neutralUnit : game.neutral().getUnits()) {
					    if (neutralUnit.getType().isMineralField()) {
					        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
					            closestMineral = neutralUnit;
					        }
					    }
					}
		
		            //if a mineral patch was found, send the worker to gather it
		            if (closestMineral != null)
		            {
		                myUnit.gather(closestMineral, false);
		            }
	        	}
	        	else if (self.getUnits().size() > 6)
	        	{
					if (myUnit.isCompleted() && flags % 2 == 0) // 1 if scouting has happened
					{
						flags += 1;
						// Move to current base location
						BaseLocation home = BWTA.getNearestBaseLocation(myUnit.getPosition());
						myUnit.move(home.getPosition());
						for (BaseLocation b : BWTA.getStartLocations())
							myUnit.move(b.getPosition(), true);
						myUnit.move(home.getPosition(), true);
						System.out.println("Sent scout");
					}
	        	}
	    }
	
	    // Display debug text and lines
	    game.drawTextScreen(10, 25, units.toString());
	    
	    String s = "Known enemy buildings:\n";
		for (SeenBuilding u : enemyBuildings)
		{
			// May as well check here for if the space is visible, and the building isn't there
			if (game.isVisible(u.getPosition().toTilePosition()))
				if (!u.getUnit().exists())
				{
					System.out.println("Couldn't find " + u.getUnit().getType() + " at " + u.getPosition().toTilePosition());
					enemyBuildings.delete(u);
				}
			s += u.getType() + "#" + u.getID() + " " + u.getPosition().toTilePosition() + "\n";
			game.drawTextMap(u.getPosition(), u.getType() + "#" + u.getID());
		}
		game.drawTextScreen(250, 25, s);
		
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
				System.out.println("Found new " + foundBuilding.getType() + " at " + foundBuilding.getPosition().toTilePosition());
				System.out.println("Now know of " + enemyBuildings.size() + " enemy buildings");
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