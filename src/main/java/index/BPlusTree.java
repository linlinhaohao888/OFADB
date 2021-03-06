package index;

/**
 * The base class of the b plus tree
 **/

public abstract class BPlusTree {
    public int order;
    public NodeBPlus root;
    public NodeBPlus headLeaf;


    public int getOrder() {
        return order;
    }

    public NodeBPlus getRoot() {
        return root;
    }

    public void setRoot(NodeBPlus root) {
        this.root = root;
    }

    public NodeBPlus getHeadLeaf() {
        return headLeaf;
    }

    public void setHeadLeaf(NodeBPlus headLeaf) {
        this.headLeaf = headLeaf;
    }

    public BPlusTree(int order, NodeBPlus treeroot) {
        if (order < 3)
            throw new RuntimeException("order of B+ Tree should > 3");
        root = treeroot;
        this.order = order;
    }

    public void insert(Comparable key, Object data) {
        root.insert(key, data, this);
    }

    public void remove(Comparable key, Object data) {
        root.remove(key, data, this);
    }

    public void update(Comparable oldKey, Comparable newKey, Object oldData, Object data) {
        root.remove(oldKey, oldData, this);
        root.insert(newKey, data, this);
    }

    public Object get(Comparable key) {
        return root.get(key);
    }

}
