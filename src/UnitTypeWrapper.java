import bwapi.Unit;
import bwapi.UnitType;


public class UnitTypeWrapper implements Comparable<UnitTypeWrapper>
{
	private UnitType t;
	private Queue<Unit> q;
	
	public int count() { return q.size(); }
	
	public UnitTypeWrapper(Unit unit)
	{
		t = unit.getType();
		q = new Queue<Unit>();
		q.add(unit);
	}
	
	public UnitTypeWrapper(UnitType type)
	{
		t = type;
	}
	
	public void add(Unit unit)
	{
		q.add(unit);
	}
	
	public Unit pop()
	{
		return q.pop();
	}
	
	public Unit cycle()
	{
		Unit unit = q.pop();
		while (unit != null && !unit.exists()) unit = q.pop();
		if (unit != null) q.add(unit);
		return unit;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof UnitTypeWrapper)
			return t.toString().equals(((UnitTypeWrapper)o).t.toString());
		else
			return super.equals(o);
	}
	
	@Override
	public int compareTo(UnitTypeWrapper type)
	{
		return t.toString().compareTo(type.t.toString());
	}
}
