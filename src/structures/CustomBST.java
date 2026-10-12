// CustomBST.java
// Part of: John Benedict Jesalva  (Binary Search Tree)
// Presented in: Slides 5-8 (John Benedict Jesalva) - range query, traversals.
// What it does: BST of equipment keyed by usage count. Used for range
//               queries and the three traversals on the Reports page.
// Used by: EquipmentStore (usage index), ReportsPanel
package structures;

import model.Equipment;

// Our own binary search tree, KEYED BY USAGE COUNT.
// Rule: everything in the left subtree has a smaller key, everything in the right subtree
// has a bigger key.
//
// Why we need it when we already have a hash table: the hash table is fast for an exact
// ID, but it has no order. It can't answer "which machines were used 3,000 to 3,050
// times?" or "list from least used to most used". The BST can, because of its ordering.
//
// Many machines have the same usage count (5,000 records but only 2,868 different counts),
// so each node keeps a small linked list called a "bucket" with every equipment that has
// that exact count. The tree has one node per distinct count.
//
// Important: since usage is the key, if a machine's usage changes it must be deleted
// BEFORE the change and inserted again AFTER. EquipmentStore.updateUsage() does this.
public class CustomBST {

    private class Node {


        int key;                                  // usage count of this node
        CustomLinkedList<Equipment> bucket;      // all equipment with exactly this usage count
        Node left, right;

        Node(int key, Equipment first) {
            this.key = key;
            this.bucket = new CustomLinkedList<>();
            this.bucket.add(first);
        }
    }

    private Node root;
    private int nodeCount;   // number of nodes (distinct usage counts), not number of equipment

    // ---------- Insert: O(log n) average, O(n) worst ----------
    // Worst case is when keys come in sorted order: every node goes to the right and the
    // tree turns into a straight line (basically a linked list).
    public void insert(Equipment e) {
        root = insert(root, e);
    }

    // Recursive: go left if smaller, right if bigger, until we hit an empty spot.
    // Returning the node and re-assigning it (node.left = insert(...)) is how the
    // new node gets attached to its parent.
    private Node insert(Node node, Equipment e) {
        if (node == null) {                       // empty spot: create the node here
            nodeCount++;
            return new Node(e.getUsageCount(), e);
        }
        int key = e.getUsageCount();
        if (key < node.key) {
            {
            node.left = insert(node.left, e);  
        }}else if (key > node.key) {
            {
            node.right = insert(node.right, e);  
        }}else {
            {
            node.bucket.add(e);                  // same usage count: join the existing node's bucket

    
                }    }return node;
    }

    // ---------- Search: O(log n) average ----------
    // Each comparison lets us ignore one whole side of the tree.
    // Done with a loop instead of recursion since we only follow one path.
    // Never returns null: an empty list means "nothing has this usage count".
    public CustomLinkedList<Equipment> search(int usageCount) {
        Node current = root;
        while (current != null) {
            if (usageCount < current.key) {
                {
                current = current.left;  
            }}else if (usageCount > current.key) {
                {
                current = current.right;  
            }}else {
                {
                return current.bucket;
            }
            }
        }
        return new CustomLinkedList<>();
    }

    // ---------- Delete ----------
    // Removes ONE equipment. It first leaves its node's bucket. The node itself is only
    // removed from the tree when its bucket becomes empty.
    public boolean delete(Equipment e) {
        CustomLinkedList<Equipment> bucket = search(e.getUsageCount());
        if (!bucket.remove(e)) {
            {
            return false;              // not in the tree

    
                }    }if (bucket.isEmpty()) {
            {
            root = deleteNode(root, e.getUsageCount());
        }
        }
        return true;
    }

    // Removes the whole root node (live change request: "delete the BST root").
    // Returns the key that was removed, or -1 if the tree is empty.
    public int deleteRoot() {
        if (root == null) {
            {
            return -1;
        }
        }
        int removedKey = root.key;
        root = deleteNode(root, removedKey);
        return removedKey;
    }

    // The standard 3 cases of BST deletion:
    //   Case 1 - leaf (no children):  just remove it (return null to the parent)
    //   Case 2 - one child:           the child takes its place
    //   Case 3 - two children:        find the in-order SUCCESSOR (smallest key in the right
    //                                 subtree), copy it into this node, then delete the
    //                                 successor from the right subtree. The successor has
    //                                 no left child, so deleting it is case 1 or 2.
    // Example from our trace: deleting 557 (two children) -> successor 590 replaces it.
    private Node deleteNode(Node node, int key) {
        if (node == null) {
            {
            return null;
        }
        }
        if (key < node.key) {
            node.left = deleteNode(node.left, key);
        } else if (key > node.key) {
            node.right = deleteNode(node.right, key);
        } else {
            if (node.left == null) {
               
                nodeCount--;
               
                return node.right;
           
            }   // case 1 (right is null too) or case 2
            if (node.right == null) {
               
                nodeCount--;
               
                return node.left;
           
            }   // case 2
            Node successor = node.right;                                  // case 3
            while (successor.left != null) {
                {
                successor = successor.left;   // go right once, then all the way left

      
                        }      }node.key = successor.key;
            node.bucket = successor.bucket;
            node.right = deleteNode(node.right, successor.key);          // nodeCount goes down inside this call
        }
        return node;
    }

    // ---------- Traversals: all O(n), every node visited once ----------
    // They return the keys (usage counts) in the order they were visited.
    // All three are recursive: the only difference is WHEN we record the current node.
    // Inorder = Left, Node, Right. On a BST this gives the keys in ascending order.
    public CustomLinkedList<Integer> inorder() {
        CustomLinkedList<Integer> out = new CustomLinkedList<>();
        inorder(root, out);
        return out;
    }

    private void inorder(Node node, CustomLinkedList<Integer> out) {
        if (node == null) {
            {
            return;                 // base case

    
                }    }inorder(node.left, out);
        out.add(node.key);
        inorder(node.right, out);
    }

    // Preorder = Node, Left, Right. Root comes first. Inserting the keys in this order
    // into an empty BST rebuilds the exact same tree shape.
    public CustomLinkedList<Integer> preorder() {
        CustomLinkedList<Integer> out = new CustomLinkedList<>();
        preorder(root, out);
        return out;
    }

    private void preorder(Node node, CustomLinkedList<Integer> out) {
        if (node == null) {
            {
            return;
        }
        }
        out.add(node.key);
        preorder(node.left, out);
        preorder(node.right, out);
    }

    // Postorder = Left, Right, Node. Root comes last; children are always visited
    // before their parent (the order you'd use to delete a whole tree).
    public CustomLinkedList<Integer> postorder() {
        CustomLinkedList<Integer> out = new CustomLinkedList<>();
        postorder(root, out);
        return out;
    }

    private void postorder(Node node, CustomLinkedList<Integer> out) {
        if (node == null) {
            {
            return;
        }
        }
        postorder(node.left, out);
        postorder(node.right, out);
        out.add(node.key);
    }

    // ---------- Range query: O(log n + k), k = number of results ----------
    // An inorder walk that skips subtrees that can't contain an answer:
    //   only go left if this key is bigger than min (smaller keys might still be in range)
    //   only go right if this key is smaller than max (bigger keys might still be in range)
    // Results come out in ascending order because it is still inorder.
    public CustomLinkedList<Equipment> getInRange(int min, int max) {
        CustomLinkedList<Equipment> result = new CustomLinkedList<>();
        collectInRange(root, min, max, result);
        return result;
    }

    private void collectInRange(Node node, int min, int max, CustomLinkedList<Equipment> result) {
        if (node == null) {
            {
            return;
        }
        }
        if (node.key > min) {
            {
            collectInRange(node.left, min, max, result);
        }
        }
        if (node.key >= min && node.key <= max) {
            for (Equipment e : node.bucket) {
                {
                result.add(e);
            }
            }
        }
        if (node.key < max) {
            {
            collectInRange(node.right, min, max, result);
        }
        }
    }

    // ---------- Info for the Reports page ----------
    public int getRootKey() {
       
        return root == null ? -1 : root.key;
   
    }


    public int getNodeCount() {
       
        return nodeCount;
   
    }


    public boolean isEmpty() {
       
        return root == null;
   
    }

    // ---------- Read-only helpers for the tree drawing on the Reports page ----------
    // These only LOOK at the tree; they never change it.
    // Keys of the top `levels` levels, laid out like a heap array:
    // root at 0, children of index i at 2i+1 (left) and 2i+2 (right). -1 = no node there.
    public int[] topLevels(int levels) {
        int[] out = new int[(1 << levels) - 1];
        for (int i = 0; i < out.length; i++) {
            {
            out[i] = -1;
        }
        }
        fillLevels(root, 0, out);
        return out;
    }

    private void fillLevels(Node node, int i, int[] out) {
        if (node == null || i >= out.length) {
            {
            return;
        }
        }
        out[i] = node.key;
        fillLevels(node.left, 2 * i + 1, out);
        fillLevels(node.right, 2 * i + 2, out);
    }

    // The keys a search for `key` visits, root first. Same walk as search(): O(height).
    public CustomLinkedList<Integer> searchPath(int key) {
        CustomLinkedList<Integer> path = new CustomLinkedList<>();
        Node current = root;
        while (current != null) {
            path.add(current.key);
            if (key == current.key) {
                {
                break;
            }
            }
            current = key < current.key ? current.left : current.right;
        }
        return path;
    }

    // Height = number of nodes on the longest root-to-leaf path.
    // Perfectly balanced would be about log2(n); a straight line would be n.
    // Ours: 26 for 2,868 nodes, so searches stay short even without balancing.
    public int height() {
       
        return height(root);
   
    }

    private int height(Node node) {
        if (node == null) {
            {
            return 0;
        }
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }
}
