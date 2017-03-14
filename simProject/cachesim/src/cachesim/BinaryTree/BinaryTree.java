package cachesim.BinaryTree;

import java.util.ArrayList;
import java.util.List;

public class BinaryTree<T> {

    public final int NumberOfLevels;
    public BinaryTreeNode FirstNode;

    // Create balanced binary-tree of numberOfLevels levels.
    public BinaryTree(T initialValueToAllNodes, int numberOfLevels) {
        this.NumberOfLevels = numberOfLevels;
        this.FirstNode = getBalancedTree(initialValueToAllNodes, numberOfLevels);
    }

    private BinaryTreeNode getBalancedTree(T initialValueToAllNodes, int numberOfLevels) {
        if (numberOfLevels == 0) {
            return null;
        } else {
            BinaryTreeNode leftTree = getBalancedTree(initialValueToAllNodes, numberOfLevels - 1);
            BinaryTreeNode RightTree = getBalancedTree(initialValueToAllNodes, numberOfLevels - 1);
            BinaryTreeNode newNode = new BinaryTreeNode(initialValueToAllNodes, leftTree, RightTree, null);
            return newNode;
        }
    }

    public List<BinaryTreeNode> getNodesOfOneLevel(final int indexOfLevel) {
        List<BinaryTreeNode> result = new ArrayList<>();

        getNodesOfOneLevel(indexOfLevel, result, FirstNode);
        return result;
    }

    public void getNodesOfOneLevel(int indexOfLevel, List<BinaryTreeNode> acc, BinaryTreeNode currNode) {
        if (indexOfLevel == 1) {
            acc.add(currNode);
        } else {
            getNodesOfOneLevel(indexOfLevel - 1, acc, currNode.LeftNode);
            getNodesOfOneLevel(indexOfLevel - 1, acc, currNode.RightNode);
        }
    }

}

