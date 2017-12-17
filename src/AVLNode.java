public class AVLNode<T extends Comparable<T>> implements Comparable<AVLNode<T>>
{
	private T value;
	private AVLNode<T> parent;
	private AVLNode<T> left, right;
	private int layers;
	
	public T getValue() { return value; }
	public AVLNode<T> getParent() { return parent; }
	public AVLNode<T> getLeft() { return left; }
	public AVLNode<T> getRight() { return right; }
	public int getLayers() { return layers; }
	
	public void setParent(AVLNode<T> parent) { this.parent = parent; }
	
	/**
	 * Initializes this node with a value, but no connections
	 * @param value The value of this node
	 */
	public AVLNode(T value)
	{
		this.value = value;
		parent = left = right = null;
		layers = 1;
	}
	
	/**
	 * Initializes this node with a value, and links it to a parent node
	 * @param value The value of this node
	 * @param parent A reference to the parent of this node
	 */
	public AVLNode(T value, AVLNode<T> parent)
	{
		this.value = value;
		this.parent = parent;
		left = right = null;
		layers = 1;
	}
	
	/**
	 * Gets the difference in maximum branch length on either side of this node
	 * @return Returns the number of levels difference between the left and right sides of this node,
	 * a negative value means the right side of this node is longer
	 */
	public int getOffset()
	{
		if (left == null)
			if (right == null)
				return 0;
			else
				return -right.getLayers();
		else if (right == null)
			return left.getLayers();
		else
			return left.getLayers() - right.getLayers();
	}
	
	/**
	 * Sets the left child of this node and updates the layer value
	 * @param left The node to put on the left side
	 */
	public void setLeft(AVLNode<T> left)
	{
		this.left = left;
		updateLayers();
	}
	
	/**
	 * Sets the right child of this node and updates the layer value
	 * @param right The node to put on the right side
	 */
	public void setRight(AVLNode<T> right)
	{
		this.right = right;
		updateLayers();
	}
	
	/**
	 * Updates which layer of the tree this node is on based on the layer values of its children
	 */
	public void updateLayers()
	{
		if (left != null)
			if (right == null || right.getLayers() < left.getLayers())
				layers = left.getLayers() + 1;
			else
				layers = right.getLayers() + 1;
		else
			if (right != null)
				layers = right.getLayers() + 1;
			else
				layers = 1;
	}
	
	/**
	 * Shifts the right child of this node into this node's spot, and updates other references accordingly
	 */
	public void rotateLeft()
	{
		AVLNode<T> oldRight = right;
		AVLNode<T> oldParent = parent;
		
		setRight(oldRight.getLeft());
		setParent(oldRight);
		
		oldRight.setParent(oldParent);
		if (oldParent != null)
			if (oldParent.getLeft() == this)
				oldParent.setLeft(oldRight);
			else
				oldParent.setRight(oldRight);
		if (oldRight.getLeft() != null)
			oldRight.getLeft().setParent(this);
		oldRight.setLeft(this);
	}
	
	/**
	 * Shift the left child of this node into this node's spot, and updates other references accordingly
	 */
	public void rotateRight()
	{
		AVLNode<T> oldLeft = left;
		AVLNode<T> oldParent = parent;
		
		setLeft(oldLeft.getRight());
		setParent(oldLeft);
		
		oldLeft.setParent(oldParent);
		if (oldParent != null)
			if (oldParent.getRight() == this)
				oldParent.setRight(oldLeft);
			else
				oldParent.setLeft(oldLeft);
		
		if (oldLeft.getRight() != null)
			oldLeft.getRight().setParent(this);
		oldLeft.setRight(this);
	}
	
	@Override
	public int compareTo(AVLNode<T> t)
	{
		return value.compareTo(t.value);
	}
	
	public int compareTo(T t)
	{
		return value.compareTo(t);
	}
	
	public boolean equals(T value)
	{
		return this.value.equals(value);
	}
	
	@Override
	public String toString()
	{
		return value.toString();
	}
}
