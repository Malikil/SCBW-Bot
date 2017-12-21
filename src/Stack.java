public class Stack<T>
{
	private LinkNode<T> base;
	private int count;
	
	public int size() { return count; }
	public T peek() { return base.getValue(); }
	
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
