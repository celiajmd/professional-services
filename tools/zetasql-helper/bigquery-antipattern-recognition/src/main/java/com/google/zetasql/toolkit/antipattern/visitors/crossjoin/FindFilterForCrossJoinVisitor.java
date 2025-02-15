package com.google.zetasql.toolkit.antipattern.visitors.crossjoin;

import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFilterScan;
import com.google.common.collect.ImmutableList;
import com.google.zetasql.resolvedast.ResolvedNodes.*;


public class FindFilterForCrossJoinVisitor extends ResolvedNodes.Visitor {

  private int countColumnRefs = 0;
  private Boolean foundFilterWithLeftTable = false;
  private Boolean foundFilterWithRightTable = false;
  private Boolean foundFilterForCrossJoin = false;
  private final String ZETASQL_EQUAL_FUN_TYPE = "ZetaSQL:$equal";

  private CrossJoin crossJoin;

  public void setCrossJoin(CrossJoin crossJoin) {
    this.crossJoin = crossJoin;
  }

  public void visit(ResolvedFilterScan resolvedFilterScan) {
    super.visit(resolvedFilterScan);
  }

  public void visit(ResolvedFunctionCall resolvedFunctionCall) {
    if (!foundFilterForCrossJoin) {
      if (resolvedFunctionCall.getFunction().toString().equals(ZETASQL_EQUAL_FUN_TYPE)) {
        checkArgumentList(resolvedFunctionCall.getArgumentList());
      }
      super.visit(resolvedFunctionCall);
    }
  }

  private void checkArgumentList(ImmutableList<ResolvedExpr> argumentList) {
    foundFilterForCrossJoin = false;
    String tempLeftTable = null;
    String tempRightTable = null;
    if (argumentList.size() == 2) {
      for (ResolvedExpr resolvedExpr : argumentList) {
        if (resolvedExpr instanceof ResolvedColumnRef) {
          String tableName = ((ResolvedColumnRef) resolvedExpr).getColumn().getTableName();
          if (crossJoin.getLeft().getTableNameList().contains(tableName)) {
            foundFilterWithLeftTable = true;
            tempLeftTable = tableName;
          } else if (crossJoin.getRight().getTableNameList().contains(tableName)) {
            foundFilterWithRightTable = true;
            tempRightTable = tableName;
          }
        }
      }
      if (foundFilterWithLeftTable && foundFilterWithRightTable) {
        foundFilterForCrossJoin = true;
        crossJoin.addTableName(tempLeftTable);
        crossJoin.addTableName(tempRightTable);
      }
    } else {
      foundFilterForCrossJoin = false;
    }
  }

  public boolean result() {
    return foundFilterWithLeftTable;
  }
}
