import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class BroodWarBot extends DefaultBWListener
{
    private Mirror mirror = new Mirror();
    private Game game;
    private Player self;
    
    private boolean timer;
    private AVLTree<SeenBuilding> enemyBuildings;
    
    public void run()
    {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit)
    {
    	/*
		 * Called when unit is created in vision, or on first discovery of neutral unit
		 * If the unit moves out of vision then re-enters, this event is not called again 
		 */
		System.out.println("New unit: " + unit.getType());
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
	    timer = false;
		enemyBuildings = new AVLTree<>();
	}
	
	@Override
	public void onFrame()
	{
		if (game.elapsedTime() % 15 == 0)
		{
			if (!timer)
			{
				game.sendText("black sheep wall"); // TODO Debug cheats
				timer = true;
			}
		}
		else if (timer)
			timer = false;
		
	    //game.setTextSize(10);
	    game.drawTextScreen(10, 10, game.mapName() + " - " + game.mapFileName() + "\tPlaying as " + self.getName() + " - " + self.getRace());
	
	    StringBuilder units = new StringBuilder("My units:\n");
	
	    //iterate through my units
	    for (Unit myUnit : self.getUnits()) {
	        units.append(myUnit.getType()).append("#").append(myUnit.getID()).append(" ").append(myUnit.getTilePosition()).append("\n");
	
	        //if there's enough minerals, train an SCV
	        if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50)
	            myUnit.train(UnitType.Terran_SCV);
	
	        //if it's a worker and it's idle, send it to the closest mineral patch
	        if (myUnit.getType().isWorker() && myUnit.isIdle()) {
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
	            if (closestMineral != null) {
	                myUnit.gather(closestMineral, false);
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
					enemyBuildings.delete(u);
			s += u.getType() + "#" + u.getID() + " " + u.getPosition().toTilePosition() + "\n";
			game.drawTextMap(u.getPosition(), u.getType() + "#" + u.getID());
		}
		game.drawTextScreen(200, 25, s);
		
	    showDebug();
	}
	
	private void showDebug()
	{
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
	    	String s = "";
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
		if (!unit.getPlayer().isNeutral())
		{
			System.out.println("Found unit: " + unit.getType() + "#" + unit.getID() + " owned by " + unit.getPlayer().getName());
			if (unit.getType().isBuilding())
				System.out.print(" Building");
			if (unit.getType().isOrganic())
				System.out.print(" Organic");
			if (unit.getType().isMechanical())
				System.out.print(" Mechanical");
			System.out.println();
		}
		
		if (unit.getType().isBuilding() && unit.getPlayer().isEnemy(self))
		{
			// Maintain a list of enemy buildings so we can come back to them
			SeenBuilding foundBuilding = new SeenBuilding(unit);
			if (!enemyBuildings.contains(foundBuilding))
			{
				enemyBuildings.add(foundBuilding);
				System.out.println("Added new building to list");
				System.out.println("Now know of " + enemyBuildings.size() + " enemy buildings");
    		}
    	}
    }

    public static void main(String[] args) {
        new BroodWarBot().run();
    }
}