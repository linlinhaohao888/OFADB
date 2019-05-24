package disk;

import index.IndexKey;
import meta.IndexInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class System {
    private static Database System;
    private static Database curDB;

    private static HashMap<String,Database> dbMap;


    public System()throws IOException{
        dbMap = new HashMap<>();
        disk.System.loadSystem();

    }

    /**
     * get the system database
     */
    public static Database getSystem()throws IOException{
        checkSystemLoaded();
        return System;
    }


    /**
     * get the table which stores metadata of database in the system database
     */
    public static Table getDatabaseTable()throws IOException{
        checkSystemLoaded();
        return System.tables.get(Logger.databaseTableName);
    }

    /**
     * get the table which stores metadata of table in the system database
     */
    public static Table getTablesTable()throws IOException{
        checkSystemLoaded();
        return System.tables.get(Logger.tablesTableName);
    }


    /**
     * get the table which stores metadata of index in the system database
     */
    public static Table getIndexesTable()throws IOException{
        checkSystemLoaded();
        return System.tables.get(Logger.indexesTableName);
    }

    /**
     * load the system database
     */
    public static void loadSystem() throws IOException {
        System = new Database(Logger.systemDatabaseName, true);
        Table databases = new Table(System, Logger.datebasesTableType(), Logger.databaseTableName, new ArrayList<IndexInfo>(){{add(new IndexInfo("0",true));}}, System.cache, 0);
        Table tables = new Table(System, Logger.tablesTableType(), Logger.tablesTableName, new ArrayList<IndexInfo>(){{add(new IndexInfo("1,0",true));}}, System.cache, 0);
        Table indexes = new Table(System,Logger.indexesTableType(),Logger.indexesTableName,new ArrayList<IndexInfo>(){{add(new IndexInfo("1,0",false));}},System.cache,-1);
        System.tables.put(databases.info.tableName, databases);
        System.tables.put(tables.info.tableName, tables);
        System.tables.put(indexes.info.tableName,indexes);
        dbMap.put("System",System);
    }


    /**
     * check if the database with input name exists
     * @param name name of the database
     */
    public static boolean databaseExistence(String name)throws IOException{
        checkSystemLoaded();

        Table databases = System.tables.get(Logger.databaseTableName);
        List<Row> result = databases.equivalenceFind(0,new IndexKey(Logger.columnTypesOfdatabaseTable,databaseData(name)));
        return result.size() > 0;
    }


    /**
     * load a database to the system and set it as the current database
     * @param name name of the database
     */
    public static void loadDataBase(String name)throws IOException{
        curDB = getDataBase(name);
    }


    /**
     * get a database
     * @param name name of the database
     */
    public static Database getDataBase(String name)throws IOException{
        checkSystemLoaded();

        if(dbMap.containsKey(name))
            return dbMap.get(name);

        if(!databaseExistence(name))
            return null;
        Database database = new Database(name,false);
        database.loadTables();
        dbMap.put(name,database);
        return database;
    }


    /**
     * check if the system database is loaded
     */
    public static void checkSystemLoaded() throws IOException {
        if (System == null)
            disk.System.loadSystem();
    }


    /**
     * create a new database
     * @param name name of the database
     */
    public static Database createNewDatabase(String name) throws IOException {
        checkSystemLoaded();

        Object[] databaseData = databaseData(name);

        if(System.tables.get(Logger.databaseTableName).insert(databaseData) != null) {
            Database ndb =  new Database(name, false);
            dbMap.put(ndb.dataBaseName,ndb);
            return ndb;
        }
        return null;
    }


    /**
     * insert a  newindex metadata into the table of index in system database
     * @param databaseName name of database
     * @param tableName name of table
     * @param info info of the index to be inserted
     */
    public static IndexInfo createNewIndex(String databaseName,String tableName,IndexInfo info)throws IOException{
        Object[] indexData =indexData(databaseName,tableName,info);
        if(getIndexesTable().insert(indexData)!=null)
            return info;
        else
            return null;
    }


    /**
     * delete a database from the  database system
     * @param database the database to be deleted
     */
    public static void removeDatabase(Database database)throws IOException{
        if(database == null)
            return;
        if(database == curDB)
            curDB = null;

        Table databases = getDatabaseTable();
        for(Table table : database.tables.values())
            database.removeTable(table);
        databases.delete(0,databasePK(database.dataBaseName));
        Logger.deleteDir(new File(Logger.databaseDirectoryPath(database)));
        dbMap.remove(database.dataBaseName);
    }

    /**
     * create a row data for the table of database in system database
     * @param name name of database
     */
    public static Object[] databaseData(String name) {
        Object[] data = new Object[1];
        data[0] = name;
        return data;
    }


    /**
     * create a row data for the table of index in system database
     * @param databaseName name of the database
     * @param tableName name of the table
     * @param info info of the index
     */
    public static Object[] indexData(String databaseName,String tableName,IndexInfo info)
    {
        Object[] data = new Object[4];
        data[0] = tableName;
        data[1] = databaseName;
        data[2] = info.columnIndexString();
        data[3] = info.isUnique;
        return data;
    }


    /**
     * create a indexkey for a searching in table table of index table in system databse
     * @param dataBaseName name of the database
     * @param tableName name of the table
     */
    public static IndexKey tablePK(String dataBaseName,String tableName){
        Object[] data = new Object[2];
        data[0] = dataBaseName;
        data[1] = tableName;
        Type[] types = new Type[2];
        types[0] = Logger.columnTypesOftableTable[1];
        types[1] = Logger.columnTypesOftableTable[0];
        return new IndexKey(types,data);
    }

    /**
     * get a indexkey in the table of database in system database
     * @param dataBaseName name of the database
     */
    public static IndexKey databasePK(String dataBaseName){
        Object[] data = new Object[1];
        data[0] = dataBaseName;
        Type[] types = new Type[1];
        types[0] = Logger.columnTypesOfdatabaseTable[0];
        return new IndexKey(types,data);
    }

    /**
     * get the current database
     */
    public static Database getCurDB(){
        return curDB;
    }


    /**
     * remove the current database from memory
     */
    public static void resetCurDB(){
        curDB = null;
    }

    /**
     * switch the current database to the input database
     * @param name name of the database
     */
    public static void switchDatabase(String name)throws IOException{
        resetCurDB();
        loadDataBase(name);
    }


    /**
     * save all the file system to disk
     */
    public static void saveSystem()throws IOException{
        for(Database ele : dbMap.values())
            ele.save();
    }

}
