package types;

/**
 * The types of expression.
 *
 * @author Hao Lin
 * @version 1.0
 * @since 1.0
 */

public enum ExprTypes {
    EXPR_COLUMN,
    EXPR_COLUMN_DEF,
    EXPR_CREATE_TABLE,
    EXPR_CREATE_DB,
    EXPR_DELETE,
    EXPR_DROP_TABLE,
    EXPR_DROP_DB,
    EXPR_FORMULA,
    EXPR_INSERT,
    EXPR_SELECT,
    EXPR_TABLE_CONSTRAINT,
    EXPR_QUALIFIER,
    EXPR_QUALIFY_ELE,
    EXPR_RANGE_TABLE,
    EXPR_SHOW_DB,
    EXPR_SHOW_DBS,
    EXPR_SHOW_TABLE,
    EXPR_UPDATE,
    EXPR_USE_DB,
    EXPR_WHERE
}
