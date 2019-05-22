package disk;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import Meta.IndexInfo;
import Meta.TableInfo;
import io.*;
import index.*;
import block.*;

/**
 * A table in database
 **/

public class Table {
    public DataFileManager dataFileManager;                         // the io manager of the data file
    public IndexBase primaryIndex;                                  // the primary index of this table
    public List<IndexBase> indexes;                                  // the index array of this table
    public TableInfo info;

    public Table(Database database, Column[] columns, String name, List<IndexInfo> indexInfos, Cache cache, int pkIndexNum) throws IOException {
        this.info = new TableInfo(database,this,columns,name,indexInfos);
        dataFileManager = new DataFileManager(this, new File(Logger.dataFilePath(this)), cache);
        loadIndex(indexInfos);
        if (pkIndexNum >= 0)
            setPKIndex(pkIndexNum);
    }

    public void setPKIndex(int indexNum) throws IOException {
        primaryIndex = indexes.get(indexNum);
        if (!primaryIndex.info.isUnique) {
            primaryIndex.info.isUnique = true;
            primaryIndex.fileIO.saveUnique();
        }

    }



    /**
     * Load the indexes
     **/
    public void loadIndex(List<IndexInfo> indexInfos) throws IOException {
        indexes = new ArrayList<>();
        for (int i = 0;i<indexInfos.size();i++) {
            indexInfos.get(i).setTable(this);
            File file = new File(Logger.indexFilePath(this, getIndexFileName(indexInfos.get(i).columnIndex)));
            IndexBase index = new IndexBase(this, Logger.defaultIndexOrder, indexInfos.get(i).columnIndex, file, indexInfos.get(i).types,indexInfos.get(i).isUnique);
            indexes.add(index);
        }
    }

    public String getIndexFileName(int[] info) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < info.length; i++) {
            name.append(this.info.columns[info[i]].columnName);
            if (i != info.length - 1)
                name.append('_');
        }
        return name.toString();
    }


    /**
     * Check the column value of unique key in the data doesn't exist in the unique index
     **/
    public boolean uniqueKeyUnUsed(Object[] data) {
        for (IndexBase index : indexes)
            if (index.info.isUnique && index.root.contains(index.getIndexAccessor(data)) >= 0)
                return false;
        return true;
    }

    /**
     * Get the rows having the input key value
     **/
    public List<Row> equivalenceFind(int indexNum, Comparable key) throws IOException {
        IndexBase index = indexes.get(indexNum);
        NodeLeaf leaf = (NodeLeaf) index.get(key);
        List<Row> rowList = new ArrayList<>();

        if(leaf != null) {
            for (Row ele : leaf.rowInfos) {
                rowList.add(dataFileManager.get(ele));
            }
        }

        return rowList;
    }


    /**
     * get the row by its primary key
     * @param pk primary key
     */
    public Row getByPrimaryKey(IndexKey pk)throws IOException{
        NodeLeaf leaf = (NodeLeaf)primaryIndex.get(pk);
        if(leaf == null)
            return null;

        return this.dataFileManager.get(leaf.rowInfos.get(0));
    }


    /**
     * Check the unique key, then insert the data into data file and all index trees
     **/
    public Row insert(Object[] data) throws IOException {
        //check the unique keys are unused before
        if (uniqueKeyUnUsed(data)) {

            // insert the data into data file
            Row row = dataFileManager.insert(RowIO.writeRowData(info.columnTypes, data));
            Row rowInfo = new Row(this, row.blockIndex, row.rowIndex);
            //insert the row into all index trees
            for (IndexBase index : indexes) {
                index.insert(index.getIndexAccessor(row.rowData), rowInfo);
                index.indexChange.saveChange();
            }
            info.rowNum++;
            return row;
        } else
            return null;
    }

    /**
     * Delete all the row with input key in input index
     **/
    public List<Row> delete(int indexNum, Comparable key) throws IOException {
        //to do
        IndexBase index = indexes.get(indexNum);
        if (index.root.contains(key) < 0)
            return null;
        NodeLeaf rowPos = (NodeLeaf) index.get(key);

        List<Row> deletedRow = new ArrayList<>();
        for (int i = 0; i < rowPos.rowInfos.size(); i++) {
            Row row = dataFileManager.get(rowPos.rowInfos.get(i).blockIndex, rowPos.rowInfos.get(i).rowIndex);
            for (IndexBase indexBase : indexes) {
                indexBase.remove(indexBase.getIndexAccessor(row.rowData), rowPos.rowInfos.get(i));
            }
            deletedRow.add(dataFileManager.delete(rowPos.rowInfos.get(i).blockIndex, rowPos.rowInfos.get(i).rowIndex));
        }
        info.rowNum -= deletedRow.size();
        return deletedRow;
    }


    public Row update(Row oldRow, Object[] data) throws IOException {
        Row old = dataFileManager.get(oldRow);
        Row updated = dataFileManager.update(oldRow.blockIndex, oldRow.rowIndex, RowIO.writeRowData(info.columnTypes, data));
        for (IndexBase index : indexes) {
            index.update(index.getIndexAccessor(old.rowData),
                    index.getIndexAccessor(data),
                    new Row(old),
                    new Row(updated));
        }
        return updated;
    }

    public void save() throws IOException {
        saveIndex();
        saveData();
    }

    public void saveIndex() throws IOException {
        for (IndexBase index : indexes) {
            index.indexChange.saveChange();
        }
    }

    public void saveData() throws IOException {
        dataFileManager.saveAll();
    }

    public static Object[] tableData(String dataBaseName,String tableName, Column[] columns, int pkIndexNum) {
        Object[] data = new Object[Logger.columnNamesOftableTable.length];
        data[0] = tableName;
        data[1] = dataBaseName;
        data[2] = Column.columnsToString(columns);
        data[4] = pkIndexNum;
        return data;
    }

    public Object[] tableData(){
        return Table.tableData(info.database.dataBaseName,info.tableName,info.columns,indexes.indexOf(primaryIndex));
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Table))
            return false;
        return info.tableName.equals(((Table) obj).info.tableName) && info.database.equals(((Table) obj).info.database);
    }

}
