package expression.select;

import types.ExprTypes;
import types.QualifyEleTypes;
import types.QualifyTypes;

import java.util.ArrayList;

public class QualifierExpr extends WhereExpr {
    private QualifyTypes qualifyTypes;
    private QualifyEleExpr lhs;
    private QualifyEleExpr rhs;

    public QualifierExpr() {
        setExprType(ExprTypes.EXPR_QUALIFIER);
    }

    public QualifierExpr(QualifyTypes qualifyTypes, QualifyEleExpr lhs, QualifyEleExpr rhs) {
        setExprType(ExprTypes.EXPR_QUALIFIER);
        this.qualifyTypes = qualifyTypes;
        this.lhs = lhs;
        this.rhs = rhs;
    }

    public QualifyTypes getQualifyTypes() {
        return qualifyTypes;
    }

    public void setQualifyTypes(QualifyTypes qualifyTypes) {
        this.qualifyTypes = qualifyTypes;
    }

    public QualifyEleExpr getLhs() {
        return lhs;
    }

    public void setLhs(QualifyEleExpr lhs) {
        this.lhs = lhs;
    }

    public QualifyEleExpr getRhs() {
        return rhs;
    }

    public void setRhs(QualifyEleExpr rhs) {
        this.rhs = rhs;
    }

    public ArrayList<QualifyEleExpr> getAttrELes() {
        ArrayList<QualifyEleExpr> result = new ArrayList<>();
        if (lhs.getEleTypes() == QualifyEleTypes.QUA_ELE_ATTR)
            result.add(lhs);
        if (rhs.getEleTypes() == QualifyEleTypes.QUA_ELE_ATTR)
            result.add(rhs);
        return result;
    }
}
