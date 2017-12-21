import java.util.Iterator;

public class Queue<T> implements Iterable<T>
{
	private LinkNode<T> base;
	private LinkNode<T> last;
	private int count;
	
	public int size() { return count; }
	
	public Queue()
	{
		base = last = null;
		count = 0;
	}
	
	public void add(T value)
	{
		if (last == null)
			base = last = new LinkNode<T>(value);
		else
		{
			last.setNext(new LinkNode<T>(value));
			last = last.getNext();
		}
		count++;
	}
	
	public T pop()
	{
		if (base == null)
			return null;
		else
		{
			T value = base.getValue();
			base = base.getNext();
			count--;
			return value;
		}
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private LinkNode<T> current = base;
			
			@Override
			public boolean hasNext()
			{
				return current != null;
			}
			
			@Override
			public T next()
			{
				T value = current.getValue();
				current = current.getNext();
				return value;
			}
		};
	}
}
