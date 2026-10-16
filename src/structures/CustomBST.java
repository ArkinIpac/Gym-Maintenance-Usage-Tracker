package structures;

import model.Equipment;

// Binary search tree keyed by usage count: smaller keys on the left, bigger on the right.
// The hash table finds an exact ID but has no order; the BST answers range questions like
// "used 3,000 to 3,050 times" and can list machines from least to most used.
// Many machines share a usage count (5,000 records, 2,868 distinct counts), so each node
// keeps a bucket (linked list) of every machine with that count.
// Because usage is the key, a machine must be removed before its usage changes and
// inserted again after. EquipmentStore.updateUsage() does this.
public class CustomBST {

    private class Node {

        int key;                                  // usage count
        CustomLinkedList<Equipment> bucket;       // every machine with this usage count
        Node left, right;

        Node(int key, Equipment first) {
            this.key = key;
            this.bucket = new CustomLinkedList<>();
            this.bucket.add(first);
        }
    }

    private Node root;
    private int nodeCount;   // distinct usage counts, not machines

    // O(log n) average, O(n) worst (keys arriving in sorted order make a straight line).
    public void insert(Equipment e) {
        root = insert(root, e);
    }

    private Node insert(Node node, Equipment e) {
        if (node == null) {
            nodeCount++;
            return new Node(e.getUsageCount(), e);
        }
        int key = e.getUsageCount();
        if (key < node.key) {
            node.left = insert(node.left, e);
        } else if (key > node.key) {
            node.right = insert(node.right, e);
        } else {
            node.bucket.add(e);                   // same count: join this node's bucket
        }
        return node;
    }

    // O(log n) average. Never returns null; an empty list means no match.
    public CustomLinkedList<Equipment> search(int usageCount) {
        Node current = root;
        while (current != null) {
            if (usageCount < current.key) {
                current = current.left;
            } else if (usageCount > current.key) {
                current = current.right;
            } else {
                return current.bucket;
            }
        }
        return new CustomLinkedList<>();
    }

    // Removes one machine from its bucket. The node only leaves the tree once its bucket is empty.
    public boolean delete(Equipment e) {
        CustomLinkedList<Equipment> bucket = search(e.getUsageCount());
        if (!bucket.remove(e)) {
            return false;
        }
        if (bucket.isEmpty()) {
            root = deleteNode(root, e.getUsageCount());
        }
        return true;
    }

    // Removes the root node. Returns its key, or -1 if the tree is empty.
    public int deleteRoot() {
        if (root == null) {
            return -1;
        }
        int removedKey = root.key;
        root = deleteNode(root, removedKey);
        return removedKey;
    }

    // The three deletion cases:
    //   no children  -> just remove it
    //   one child    -> the child takes its place
    //   two children -> copy in the in-order successor (smallest key on the right),
    //                   then delete the successor from the right subtree
    private Node deleteNode(Node node, int key) {
        if (node == null) {
            return null;
        }
        if (key < node.key) {
            node.left = deleteNode(node.left, key);
        } else if (key > node.key) {
            node.right = deleteNode(node.right, key);
        } else {
            if (node.left == null) {
                nodeCount--;
                return node.right;
            }
            if (node.right == null) {
                nodeCount--;
                return node.left;
            }
            Node successor = node.right;
            while (successor.left != null) {
                successor = successor.left;
            }
            node.key = successor.key;
            node.bucket = successor.bucket;
            node.right = deleteNode(node.right, successor.key);   // nodeCount drops in here
        }
        return node;
    }

    // Traversals are O(n). They differ only in when the current node is recorded.

    // Left, node, right: keys come out in ascending order.
    public CustomLinkedList<Integer> inorder() {
        CustomLinkedList<Integer> out = new CustomLinkedList<>();
        inorder(root, out);
        return out;
    }

    private void inorder(Node node, CustomLinkedList<Integer> out) {
        if (node == null) {
            return;
        }
        inorder(node.left, out);
        out.add(node.key);
        inorder(node.right, out);
    }

    // Node, left, right: root first. Inserting keys in this order rebuilds the same tree.
    public CustomLinkedList<Integer> preorder() {
        CustomLinkedList<Integer> out = new CustomLinkedList<>();
        preorder(root, out);
        return out;
    }

    private void preorder(Node node, CustomLinkedList<Integer> out) {
        if (node == null) {
            return;
        }
        out.add(node.key);
        preorder(node.left, out);
        preorder(node.right, out);
    }

    // Left, right, node: root last, children always before their parent.
    public CustomLinkedList<Integer> postorder() {
        CustomLinkedList<Integer> out = new CustomLinkedList<>();
        postorder(root, out);
        return out;
    }

    private void postorder(Node node, CustomLinkedList<Integer> out) {
        if (node == null) {
            return;
        }
        postorder(node.left, out);
        postorder(node.right, out);
        out.add(node.key);
    }

    // O(log n + k) for k results. An inorder walk that skips subtrees that can't match,
    // so results still come out in ascending order.
    public CustomLinkedList<Equipment> getInRange(int min, int max) {
        CustomLinkedList<Equipment> result = new CustomLinkedList<>();
        collectInRange(root, min, max, result);
        return result;
    }

    private void collectInRange(Node node, int min, int max, CustomLinkedList<Equipment> result) {
        if (node == null) {
            return;
        }
        if (node.key > min) {
            collectInRange(node.left, min, max, result);
        }
        if (node.key >= min && node.key <= max) {
            for (Equipment e : node.bucket) {
                result.add(e);
            }
        }
        if (node.key < max) {
            collectInRange(node.right, min, max, result);
        }
    }

    public int getRootKey() {
        return root == null ? -1 : root.key;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public boolean isEmpty() {
        return root == null;
    }

    // Keys of the top levels in heap layout (children of i at 2i+1 and 2i+2), -1 where
    // there's no node. Used to draw the tree on the Reports page.
    public int[] topLevels(int levels) {
        int[] out = new int[(1 << levels) - 1];
        for (int i = 0; i < out.length; i++) {
            out[i] = -1;
        }
        fillLevels(root, 0, out);
        return out;
    }

    private void fillLevels(Node node, int i, int[] out) {
        if (node == null || i >= out.length) {
            return;
        }
        out[i] = node.key;
        fillLevels(node.left, 2 * i + 1, out);
        fillLevels(node.right, 2 * i + 2, out);
    }

    // The keys a search visits, root first. O(height).
    public CustomLinkedList<Integer> searchPath(int key) {
        CustomLinkedList<Integer> path = new CustomLinkedList<>();
        Node current = root;
        while (current != null) {
            path.add(current.key);
            if (key == current.key) {
                break;
            }
            current = key < current.key ? current.left : current.right;
        }
        return path;
    }

    // Nodes on the longest root-to-leaf path. Ours is 26 for 2,868 nodes,
    // so searches stay short even without balancing.
    public int height() {
        return height(root);
    }

    private int height(Node node) {
        if (node == null) {
            return 0;
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }
}
