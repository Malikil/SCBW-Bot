import bwapi.Position;
import bwapi.Unit;
import bwapi.UnitType;

public class SeenBuilding implements Comparable<SeenBuilding>
{
	private int id;
	private Unit unit;
	private UnitType type;
	private Position position;
	
	public int getID() { return id; }
	public Unit getUnit() { return unit; }
	public UnitType getType() { return type; }
	public Position getPosition() { return position; }
	
	public SeenBuilding(Unit unit)
	{
		this.unit = unit;
		id = unit.getID();
		type = unit.getType();
		position = unit.getPosition();
	}
	
	@Override
	public int compareTo(SeenBuilding u)
	{
		return id - u.id;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof SeenBuilding)
			return id == ((SeenBuilding)o).id;
		else if (o instanceof Unit)
			return id == ((Unit)o).getID();
		else
			return super.equals(o);
	}
}
