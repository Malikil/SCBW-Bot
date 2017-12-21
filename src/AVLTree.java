import java.util.Iterator;

public class AVLTree<T extends Comparable<T>> implements Iterable<T>
{
	private AVLNode<T> base;
	private int count;
	
	public int size() { return count; }
	
	/**
	 * The default constructor, the base will be set to null and no items will be in the tree 
	 */
	public AVLTree()
	{
		base = null;
		count = 0;
	}
	
	/**
	 * A constructor to set the base of the tree to an initial value
	 * @param value The initial base value
	 */
	public AVLTree(T value)
	{
		base = new AVLNode<T>(value);
		count = 1;
	}
	
	/**
	 * Adds a value to the tree, then makes sure the tree is balanced
	 * @param value The value to add to the tree
	 */
	public void add(T value)
	{
		if (base == null)
			base = new AVLNode<>(value);
		else
		{
			AVLNode<T> current = base;
			while (true)
			{
				if (value.compareTo(current.getValue()) < 0)
				{
					if (current.getLeft() != null)
						current = current.getLeft();
					else
					{
						current.setLeft(new AVLNode<T>(value, current));
						break;
					}
				}
				else
				{
					if (current.getRight() != null)
						current = current.getRight();
					else
					{
						current.setRight(new AVLNode<T>(value, current));
						break;
					}
				}
			}
			rebalance(current);
		}
		count++;
	}
	
	/**
	 * Deletes the first node found which matches the passed value
	 * @param value The value to delete
	 * @return Returns true if the value was found, false if the value wasn't on the tree
	 */
	public boolean delete(T value)
	{
		AVLNode<T> current = base;
		while (current != null)
		{
			if (current.compareTo(value) < 0)
				current = current.getRight();
			else if (current.compareTo(value) > 0)
				current = current.getLeft();
			else
			{
				// delete current value
				if (current.getLayers() == 1)
				{
					if (current.getParent() == null)
					{
						base = null;
						count = 0;
						return true;
					}
					else if (current.getParent().getLeft() == current)
						current.getParent().setLeft(null);
					else
						current.getParent().setRight(null);
					rebalance(current.getParent());
				}
				else
				{
					AVLNode<T> deleter = current;
					
					if (current.getRight() != null)
					{
						current = current.getRight();
						while (current.getLeft() != null)
							current = current.getLeft();
					}
					else
						current = current.getLeft();
					// replace deleter with current
					if (current.getParent().getLeft() == current)
						current.getParent().setLeft(null);
					else
						current.getParent().setRight(null);
					
					AVLNode<T> rebalancer = current.getParent();
					current.setLeft(deleter.getLeft());
					current.setRight(deleter.getRight());
					current.setParent(deleter.getParent());
					// set parent child
					if (deleter.getLeft() != null)
						deleter.getLeft().setParent(current);
					if (deleter.getRight() != null)
						deleter.getRight().setParent(current);
					if (deleter.getParent() != null)
						if (deleter.getParent().getRight() == deleter)
							deleter.getParent().setRight(current);
						else
							deleter.getParent().setLeft(current);
					if (count > 2)
						rebalance(rebalancer);
					else
						base = current;
				}
				
				count--;
				return true;
			}
		}
		return false;
	}
	
	private void rebalance(AVLNode<T> current)
	{
		if (current.getLeft() != null)
			current = current.getLeft();
		else if (current.getRight() != null)
			current = current.getRight();
		
		while (current.getParent() != null)
		{
			current = current.getParent();
			current.updateLayers();
			if (current.getOffset() < -1)
			{
				if (current.getRight().getOffset() > 0)
					current.getRight().rotateRight();
				current.rotateLeft();
			}
			else if (current.getOffset() > 1)
			{
				if (current.getLeft().getOffset() < 0)
					current.getLeft().rotateLeft();
				current.rotateRight();
			}
		}
		base = current;
	}
	
	/**
	 * Checks if the tree contains a certain value
	 * @param value The value to check
	 * @return Returns true if the value is on the tree, otherwise false
	 */
	public boolean contains(T value)
	{
		if (base == null)
			return false;
		else
		{
			AVLNode<T> current = base;
			while (!current.equals(value))
				if (current.getValue().compareTo(value) > 0)
					if (current.getLeft() == null)
						return false;
					else
						current = current.getLeft();
				else
					if (current.getRight() == null)
						return false;
					else
						current = current.getRight();
			return true;
		}
	}
	
	/**
	 * Searches the tree for a value, and returns that value
	 * @param value The value to search for
	 * @return Returns the first instance of the value if found, null otherwise
	 */
	public T get(T value)
	{
		AVLNode<T> current = base;
		while (current != null)
		{
			if (current.compareTo(value) < 0)
				current = current.getRight();
			else if (current.compareTo(value) > 0)
				current = current.getLeft();
			else
				return current.getValue();
		}
		return null;
	}
	
	public T[] toArray(T[] arr)
	{
		// Don't do this at home, kids
		count = 0;
		if (base != null)
			copyNode(base, arr);
		if (arr.length > count)
			arr[count] = null;
		return arr;
	}
	
	private void copyNode(AVLNode<T> node, T[] arr)
	{
		if (node.getLeft() != null)
			copyNode(node.getLeft(), arr);
		arr[count++] = node.getValue();
		if (node.getRight() != null)
			copyNode(node.getRight(), arr);
	}
	
	public AVLTree<T> reconstructTree()
	{
		AVLTree<T> temp = new AVLTree<>();
		if (base != null)
			reconstructNode(base, temp);
		return temp;
	}
	
	private void reconstructNode(AVLNode<T> node, AVLTree<T> newTree)
	{
		newTree.add(node.getValue());
		if (node.getLeft() != null)
			reconstructNode(node.getLeft(), newTree);
		if (node.getRight() != null)
			reconstructNode(node.getRight(), newTree);
	}
	
	public T popMin()
	{
		// Not sure whether to return null for an empty tree, or to throw exception...
		if (count < 1)
			return null; //throw new IndexOutOfBoundsException("Cannot pop. Tree contains no objects");
		
		AVLNode<T> current = base;
		while (current.getLeft() != null)
			current = current.getLeft();
		
		// Three cases:
		//   - The farthest left node has a right child
		//   - The farthest left node has no children
		//   - The base of the tree is the only value
		if (current.getRight() != null)
		{
			if (current.getParent() != null)
				current.getParent().setLeft(current.getRight());
			current.getRight().setParent(current.getParent());
			rebalance(current.getRight());
			count--;
		}
		else if (current.getParent() != null)
		{
			current.getParent().setLeft(null);
			rebalance(current.getParent());
			count--;
		}
		else
		{
			base = null;
			count = 0;
		}
		
		return current.getValue();
	}
	
	public void printTree()
	{
		printNode(base, "B");
	}
	
	private void printNode(AVLNode<T> node, String pre)
	{
		System.out.println(pre + "\t" + node.getValue().toString());
		if (node.getLeft() != null)
			printNode(node.getLeft(), pre + ".L");
		if (node.getRight() != null)
			printNode(node.getRight(), pre + ".R");
	}

	@Override
	public Iterator<T> iterator()
	{
		return new Iterator<T>() {
			private Stack<AVLNode<T>> st = new Stack<>();
			AVLNode<T> current = base;
			
			@Override
			public boolean hasNext()
			{
				return current != null || st.size() > 0;
			}
	
			@Override
			public T next()
			{
				while (current != null)
				{
					st.add(current);
					current = current.getLeft();
				}
				current = st.pop();
				T value = current.getValue();
				current = current.getRight();
				return value;
			}
		};
	}
}
