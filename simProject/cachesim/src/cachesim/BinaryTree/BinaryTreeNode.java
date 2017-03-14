package cachesim.BinaryTree;

public class BinaryTreeNode<T> {
    public T Value;
    public BinaryTreeNode LeftNode;
    public BinaryTreeNode RightNode;
    public BinaryTreeNode ParentNode;

    public BinaryTreeNode(T value, BinaryTreeNode leftNode, BinaryTreeNode rightNode, BinaryTreeNode parentNode) {
        this.Value = value;
        this.LeftNode = leftNode;
        this.RightNode = rightNode;
        this.ParentNode = parentNode;

        if (LeftNode != null) {
            LeftNode.ParentNode = this;
        }

        if (RightNode != null) {
            RightNode.ParentNode = this;
        }
    }
}
