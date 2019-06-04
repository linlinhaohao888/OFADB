package utils;

import disk.System;
import disk.Type;
import expression.select.*;
import meta.ColumnInfo;
import meta.MetaData;
import meta.TableInfo;
import types.ColumnTypes;
import types.ExprTypes;
import types.RangeTableTypes;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

/**
 * Various utility functions.
 *
 * @author Hao Lin
 * @version 1.0
 * @since 1.0
 */

public class Utils {
    /**
     * Transform the tree structure of join expression to array form.
     * @param root {@code RangeTableExpr} variable
     * @return {@code ArrayList} of {@code RangeTableExpr}
     */
    public static ArrayList<RangeTableExpr> getFromTableList(RangeTableExpr root) {
        ArrayList<RangeTableExpr> result = new ArrayList<>();
        while (true) {
            if (root.getRtTypes() == RangeTableTypes.RT_RELATION ||
                    root.getRtTypes() == RangeTableTypes.RT_SUB_QUERY) {
                result.add(root);
                return result;
            } else if (root.getRtTypes() == RangeTableTypes.RT_JOIN) {
                result.add(((JoinExpr) root).getRhs());
                root = ((JoinExpr) root).getLhs();
            } else
                throw new RuntimeException("The from expression wasn't expanded correctly");
        }
    }

    /**
     * Acquire the map from table alias to table name.
     * For sub query, the alias is mapped to null.
     * For range table that has no alias, the table name is mapped to itself.
     * @param root {@code RangeTableExpr} variable.
     * @return {@code HashMap} of {@code String} to {@code String}.
     */
    public static HashMap<String, String> getTableNameList(RangeTableExpr root) {
        ArrayList<RangeTableExpr> fromTableList = getFromTableList(root);
        HashMap<String, String> tableNames = new HashMap<>();
        for (RangeTableExpr tableExpr : fromTableList) {
            if (tableExpr.getRtTypes() == RangeTableTypes.RT_SUB_QUERY) {
                SubSelectExpr subSelectExpr = (SubSelectExpr) tableExpr;
                if (subSelectExpr.getAlias() == null)
                    throw new RuntimeException("The sub-select statement needs an alias");
                tableNames.put(((SubSelectExpr) tableExpr).getAlias(), null);
            } else if (tableExpr.getRtTypes() == RangeTableTypes.RT_RELATION) {
                RelationExpr relationExpr = (RelationExpr) tableExpr;
                if (!relationExpr.getAlias().equals(""))
                    tableNames.put(relationExpr.getAlias(), relationExpr.getTableName());
                else {
                    tableNames.put(relationExpr.getTableName(), relationExpr.getTableName());
                }
            } else
                throw new RuntimeException("The from expression wasn't expanded correctly");
        }
        return tableNames;
    }

    /**
     * Verify where clause.
     * @param root {@code WhereExpr}.
     * @param table {@code RangeTableExpr} variable is the field of where clause.
     * @throws IOException IO exception.
     */
    public static void checkWhereClause(WhereExpr root, RangeTableExpr table) throws IOException {
        if (root == null)
            return;
        if (root.getExprType() == ExprTypes.EXPR_QUALIFIER) {
            QualifierExpr qualifierExpr = ((QualifierExpr) root);
            ArrayList<QualifyEleExpr> attrELes = qualifierExpr.getAttrELes();
            checkAttrEles(table, attrELes);
            qualifierExpr.checkValidity(table);
        } else {
            checkWhereClause(root.getRight(), table);
            checkWhereClause(root.getLeft(), table);
        }
    }

    public static void checkAttrEles(RangeTableExpr rangeTableExpr, ArrayList<QualifyEleExpr> attrELes) throws IOException {
        for (QualifyEleExpr eleExpr : attrELes) {
            ResultColumnExpr attr = (ResultColumnExpr) eleExpr.getValue();
            checkColumnExpr(attr, rangeTableExpr, true);
        }
    }

    @SuppressWarnings("ConstantConditions")
    public static ColumnTypes checkColumnExpr(ResultColumnExpr columnExpr, RangeTableExpr rangeTableExpr, boolean addTableName) throws IOException {
        ArrayList<RangeTableExpr> fromTableList = getFromTableList(rangeTableExpr);
        HashMap<String, String> tableNames = getTableNameList(rangeTableExpr);
        String dbName = System.getCurDB().dataBaseName;
        String tableName = tableNames.get(columnExpr.getTableName());
        SubSelectExpr subSelectExpr = null;

        if (!columnExpr.getDbName().equals(""))
            throw new RuntimeException("Syntax error of writing database name in query statement");

        // Verify column name that has no table name
        if (columnExpr.getTableName().equals("")) {
            int occurTimes = 0;
            for (RangeTableExpr fromExpr : fromTableList) {
                if (fromExpr.getRtTypes() == RangeTableTypes.RT_SUB_QUERY &&
                        ((SubSelectExpr) fromExpr).getSelectExpr().getColumn(columnExpr.getAttrName()) != null) {
                    occurTimes += 1;
                    subSelectExpr = (SubSelectExpr) fromExpr;
                    tableName = null;
                    if (addTableName)
                        columnExpr.setTableName(subSelectExpr.getAlias());
                } else {
                    if (fromExpr.getRtTypes() == RangeTableTypes.RT_RELATION &&
                            MetaData.isColumnExist(dbName, ((RelationExpr) fromExpr).getTableName(), columnExpr.getAttrName())) {
                        occurTimes += 1;
                        tableName = ((RelationExpr) fromExpr).getTableName();
                        if (addTableName)
                            columnExpr.setTableName(((RelationExpr) fromExpr).getName());
                    }
                }
            }

            if (occurTimes == 0)
                throw new RuntimeException("Column name " + columnExpr.getAttrName() + " does not exist");
            if (occurTimes > 1)
                throw new RuntimeException("Ambiguous column name " + columnExpr.getAttrName());
            else {
                if (tableName == null) {
                    SelectExpr selectExpr = subSelectExpr.getSelectExpr();
                    return checkColumnExpr(selectExpr.getColumn(columnExpr.getAttrName()), selectExpr.getFromExpr(), true);
                } else {
                    return MetaData.getColumnType(System.getCurDB().dataBaseName, tableName, columnExpr.getAttrName()).columnType.typeCode;
                }
            }
        }

        // Verify column name of sub query or not-exist table name
        if (tableName == null) {
            for (RangeTableExpr fromExpr : fromTableList) {
                if (fromExpr.getRtTypes() == RangeTableTypes.RT_SUB_QUERY &&
                        ((SubSelectExpr) fromExpr).getAlias().equals(columnExpr.getTableName())) {
                    if (((SubSelectExpr) fromExpr).getSelectExpr().getColumn(columnExpr.getAttrName()) == null)
                        throw new RuntimeException("Column name " + columnExpr.getAttrName() + " does not exist");
                    else {
                        SelectExpr selectExpr = ((SubSelectExpr) fromExpr).getSelectExpr();
                        return checkColumnExpr(selectExpr.getColumn(columnExpr.getAttrName()), selectExpr.getFromExpr(), true);
                    }
                }
            }
            throw new RuntimeException("Table name " + columnExpr.getTableName() + " does not exist");
        }

        // Verify column name of relation
        if (!MetaData.isColumnExist(dbName, tableName, columnExpr.getAttrName()))
            throw new RuntimeException("Column " + columnExpr.getAttrName() + " does not exist in table " + tableName);
        else {
            return MetaData.getColumnType(System.getCurDB().dataBaseName, tableName, columnExpr.getAttrName()).columnType.typeCode;
        }
    }

    private static boolean objectEqualsColumnType(Object object, Type columnType) {
        ColumnTypes type = columnType.typeCode;
        if (object == null)
            return true;
        if (object instanceof Long) {
            return type.equals(ColumnTypes.COL_SHORT) ||
                    type.equals(ColumnTypes.COL_INT) ||
                    type.equals(ColumnTypes.COL_LONG);
        }

        if (object instanceof Double) {
            return type.equals(ColumnTypes.COL_FLOAT) ||
                    type.equals(ColumnTypes.COL_DOUBLE);
        }

        if (object instanceof String) {
            if (type.equals(ColumnTypes.COL_STRING) ||
                    type.equals(ColumnTypes.COL_CHAR)) {
                if (((String) object).length() > columnType.maxLength)
                    throw new RuntimeException("The string is too long");
                return true;
            }

            return false;
        }

        if (object instanceof Boolean) {
            return type.equals(ColumnTypes.COL_BOOL);
        } else
            throw new RuntimeException("Unknown insert value type");
    }

    public static void checkColumnsValues(RelationExpr table, ArrayList<String> columns, ArrayList<ArrayList<Object>> values) throws IOException {
        table.checkValidity();
        TableInfo tableInfo = MetaData.getTableInfoByName(table.getDbName(), table.getTableName());
        if (columns.size() == 0) {
            ColumnInfo[] columnInfos = Objects.requireNonNull(tableInfo).columns;
            for (ColumnInfo columnInfo : columnInfos) {
                columns.add(columnInfo.columnName);
            }
        } else {
            for (String columnName : columns) {
                if (!MetaData.isColumnExist(table.getDbName(), table.getTableName(), columnName))
                    throw new RuntimeException("Column " + columnName + " does not exist");
            }
        }

        if (values.size() != columns.size())
            throw new RuntimeException("Inserted values is not enough for specified columns");

        for (int i = 0; i < values.size(); i++) {
            if (!objectEqualsColumnType(values, Objects.requireNonNull(MetaData.getColumnType(table.getDbName(), table.getTableName(), columns.get(i))).columnType))
                throw new RuntimeException("Value type does not match column type");
        }
    }

    public static SelectExpr getSelectExpr(WhereExpr whereExpr, RelationExpr table) throws IOException {
        SelectExpr selectExpr = new SelectExpr();
        selectExpr.setWhereExpr(whereExpr);
        selectExpr.setFromExpr(table);
        ResultColumnExpr resultColumnExpr = new ResultColumnExpr();
        resultColumnExpr.setAttrName("*");
        selectExpr.getResultColumnExprs().add(resultColumnExpr);
        selectExpr.checkValidity();
        return selectExpr;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream;
        objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(obj);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        objectOutputStream.close();
        byteArrayOutputStream.close();
        return bytes;
    }

    public static Object deserialize(byte[] str) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(str);
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        Object object = objectInputStream.readObject();
        objectInputStream.close();
        byteArrayInputStream.close();
        return object;
    }

    public static int[] getPosFromStr(String string){
        String[] strs = string.split(",");
        int[] result = new int[strs.length];
        for(int i = 0;i<strs.length;i++)
            result[i] = Integer.parseInt(strs[i]);
        return result;
    }
}