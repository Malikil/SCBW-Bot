public class LinkNode<T extends Comparable<T>> implements Comparable<T>
{
	private LinkNode<T> next;
	private T value;
	
	public LinkNode<T> getNext() { return next; }
	public T getValue() { return value; }
	
	public void setNext(LinkNode<T> next) { this.next = next; }
	
	public LinkNode(T value)
	{
		this.value = value;
		next = null;
	}
	
	public LinkNode(T value, LinkNode<T> next)
	{
		this.value = value;
		this.next = next;
	}
	
	@Override
	public int compareTo(T v)
	{
		return value.compareTo(v);
	}
}