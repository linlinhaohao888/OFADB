package index;

import io.IndexFileIO;
import disk.Table;
import disk.Type;

import java.io.File;
import java.util.HashMap;

/**
 * The base class of index
 **/

public class IndexBase extends BPlusTree {

    public Type[] types;
    public IndexFileIO fileIO;
    public IndexChange indexChange;
    public HashMap<Long, NodeIndex> loadedNodes;
    public Table table;
    public int[] columnIndex;
    public boolean isUnique;

    public IndexBase(Table table, int order, int[] index, File file, Type[] types1) {
        super(order, null);
        this.types = types1;
        this.table = table;
        this.columnIndex = index;
        this.loadedNodes = new HashMap<Long, NodeIndex>();

        this.fileIO = new IndexFileIO(file, order, this);
        this.indexChange = new IndexChange(this);

    }


    public IndexKey getIndexAccessor(Object[] data) {
        Object[] keyData = new Object[types.length];
        for (int i = 0; i < columnIndex.length; i++) {
            keyData[i] = data[columnIndex[i]];
        }
        return new IndexKey(types, keyData);
    }
}
