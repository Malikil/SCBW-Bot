public class Stack<T extends Comparable<T>>
{
	private LinkNode<T> base;
	private int count;
	
	public int size() { return count; }
	
	public Stack() 
	{
		base = null;
		count = 0;
	}
	
	public T pop()
	{
		if (base == null)
			return null;
		T value = base.getValue();
		base = base.getNext();
		count--;
		return value;
	}
	
	public void add(T value)
	{
		base = new LinkNode<>(value, base);
		count++;
	}
}
