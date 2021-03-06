package disk;

import meta.ColumnInfo;

import java.io.File;
import java.io.IOException;

public class Logger {
    public final static int defaultBlockSize = 4096;                                        // default size of one block of data file
    public final static int cacheSizeLimit = defaultBlockSize * 256;                                    // size of the block cache
    public final static String dataFileSuffix = ".data";                                    // suffix of data file
    public final static String indexFileSuffix = ".ndx";                                    // suffix of index file
    public final static String FilePrefix = "./data/";
    public final static String indexFilePrefix = "/index/";
    public final static int defaultIndexOrder = 3;                                          // order of bplus tree in index
    public final static String columnIndexStringSegment = ",";                              // segment of column index in index column string
    public final static String indexStringSegment = "\\|";                                  // segment of different index string

    public final static String systemDatabaseName = "system";                               // name of system database storing the meta data

    public final static String databaseTableName = "databases";
    public final static String[] columnNamesOfdatabaseTable = {"name"};
    public final static Type[] columnTypesOfdatabaseTable = {Type.stringType(20)};

    public final static String tablesTableName = "tables";
    public final static String[] columnNamesOftableTable = {"tableName", "databaseName", "fields", "indexesInfo", "pkInfo"};
    public final static Type[] columnTypesOftableTable = {Type.stringType(20), Type.stringType(20), Type.stringType(400), Type.stringType(200), Type.intType()};

    public final static String indexesTableName = "indexes";
    public final static String[] columnNamesOfindexTable = {"tableName", "databaseName", "columnsInfo", "isUnique"};
    public final static Type[] getColumnTypesOfindexTable = {Type.stringType(20), Type.stringType(20), Type.stringType(200), Type.boolType()};

    public final static String tableColumnNameSegment = "\1";

    /**
     * Return the path of data file from a table
     **/
    public static String dataFilePath(Table table) throws IOException {
        String filePath = FilePrefix + table.info.database.dataBaseName + '/' + table.info.tableName + '/' + table.info.tableName + dataFileSuffix;
        preFile(filePath);
        return filePath;
    }

    /**
     * Return the path of index file from a table and column
     **/
    public static String indexFilePath(Table table, String columns) throws IOException {
        String filePath = FilePrefix + table.info.database.dataBaseName + '/' + table.info.tableName + indexFilePrefix + columns + indexFileSuffix;
        preFile(filePath);
        return filePath;
    }


    /**
     * Return the path of a table directory
     **/
    public static String tableDirectoryPath(Table table) throws IOException {
        String filePath = FilePrefix + table.info.database.dataBaseName + '/' + table.info.tableName;
        preFile(filePath);
        return filePath;
    }

    /**
     * Return the path of a database directory
     **/
    public static String databaseDirectoryPath(Database dataBase) throws IOException {
        String filePath = FilePrefix + dataBase.dataBaseName;
        preFile(filePath);
        return filePath;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void preFile(String path) throws IOException {
        File file = new File(path);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
        }
    }

    public static boolean deleteDir(File someFile) {
        if (!someFile.exists()) {
            java.lang.System.out.println("[deleteDir]File " + someFile.getAbsolutePath()
                    + " does not exist.");
            return false;
        }
        if (someFile.isDirectory()) {// is a folder
            File[] files = someFile.listFiles();
            for (File subFile : files) {
                boolean isSuccess = deleteDir(subFile);
                if (!isSuccess) {
                    return isSuccess;
                }
            }
        } else {// is a regular file
            boolean isSuccess = someFile.delete();
            if (!isSuccess) {
                return isSuccess;
            }
        }
        if (someFile.isDirectory()) {
            return someFile.delete();
        } else {
            return true;
        }
    }


    public Logger() {
    }

    public static ColumnInfo[] datebasesTableType() {
        ColumnInfo[] columns = new ColumnInfo[columnNamesOfdatabaseTable.length];
        for (int i = 0; i < columns.length; i++)
            columns[i] = new ColumnInfo(columnTypesOfdatabaseTable[i], columnNamesOfdatabaseTable[i]);
        return columns;
    }

    public static ColumnInfo[] tablesTableType() {
        ColumnInfo[] columns = new ColumnInfo[columnNamesOftableTable.length];
        for (int i = 0; i < columns.length; i++)
            columns[i] = new ColumnInfo(columnTypesOftableTable[i], columnNamesOftableTable[i]);
        return columns;
    }

    public static ColumnInfo[] indexesTableType() {
        ColumnInfo[] columns = new ColumnInfo[columnNamesOfindexTable.length];
        for (int i = 0; i < columns.length; i++)
            columns[i] = new ColumnInfo(getColumnTypesOfindexTable[i], columnNamesOfindexTable[i]);
        return columns;
    }
}
