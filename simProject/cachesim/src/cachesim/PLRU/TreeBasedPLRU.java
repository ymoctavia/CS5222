package cachesim.PLRU;

import cachesim.BinaryTree.BinaryTree;
import cachesim.BinaryTree.BinaryTreeNode;

import java.util.LinkedHashMap;
import java.util.List;

public class TreeBasedPLRU extends AbstractPLRU {

    private LinkedHashMap<Integer, BinaryTree> sets;

    public TreeBasedPLRU(int targetCacheSize, int targetCacheBlockSize, int targetCacheWays, int wordSize) {

        int numberOfCacheBlocks = targetCacheSize / targetCacheBlockSize;
        int numberOfSets = numberOfCacheBlocks / targetCacheWays;

        this.sets = new LinkedHashMap<>();

        int numberOfWordsInBlock = targetCacheBlockSize / wordSize;
        int numberOfWordsInSet = targetCacheWays * numberOfWordsInBlock;
        // targetCacheBlockSize ==numberOfWordsInSet

        int numberOfBinaryTreeLevels = log2(numberOfWordsInSet) + 1;

        // Create balanced binary-tree of numberOfBinaryTreeLevels levels.
        for (int i = 0; i < numberOfSets; i++) {
            BinaryTree<Integer> newBinaryTreeForSet = new BinaryTree<Integer>(0, numberOfBinaryTreeLevels);

            List<BinaryTreeNode> allLastLevelNodes = newBinaryTreeForSet.getNodesOfOneLevel(numberOfBinaryTreeLevels);
            for (int j = 0; j < numberOfBinaryTreeLevels; j++) {
                allLastLevelNodes.get(j).Value = j;
            }

            this.sets.put(i, newBinaryTreeForSet);
        }
    }

    public int getVictimBlockOffset(int setIndex) {
        return getVictimBlockOffset(sets.get(setIndex).FirstNode);
    }

    private int getVictimBlockOffset(BinaryTreeNode<Integer> node) {
        if (node.LeftNode == null) {
            return node.Value;
        } else if (node.Value == 0) {
            return getVictimBlockOffset(node.LeftNode);
        } else {
            return getVictimBlockOffset(node.RightNode);
        }
    }

    private int log2(int n) {
        return (int) (Math.log(n) / Math.log(2));
    }

    public void onGetBlock(int setIndex, int offset) {
        traverseBlockAccess(setIndex, offset);
    }

    private void traverseBlockAccess(int setIndex, int offset) {

        BinaryTree currentTree = sets.get(setIndex);
        List<BinaryTreeNode> allLastLevelNodes = currentTree.getNodesOfOneLevel(currentTree.NumberOfLevels);
        BinaryTreeNode currentNode = allLastLevelNodes.get(offset);

        while (currentNode.ParentNode != null) {

            if (currentNode.ParentNode.LeftNode == currentNode) {
                currentNode.ParentNode.Value = 1;
            } else {
                currentNode.ParentNode.Value = 0;
            }

            currentNode = currentNode.ParentNode;

        }
    }
}
