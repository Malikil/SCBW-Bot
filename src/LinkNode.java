public class LinkNode<T>
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
}