package com.google.zetasql.toolkit.antipattern.visitors.subqueryinwhere;

import com.google.zetasql.resolvedast.ResolvedNodes;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedAggregateScanBase;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedFunctionCall;
import com.google.zetasql.resolvedast.ResolvedNodes.ResolvedSubqueryExpr;
import java.util.ArrayList;
import java.util.List;

public class SubqueryInWhereVisitor extends ResolvedNodes.Visitor {


  private static final String SUBQUERY_TYPE_IN = "IN";
  private List<String> subQueriesInWhereWithoutAgg = new ArrayList<>();

  public void visit(ResolvedFunctionCall resolvedFunctionCall) {
    resolvedFunctionCall
        .getArgumentList()
        .forEach(
            resolvedExpr -> {
              if (resolvedExpr instanceof ResolvedSubqueryExpr) {
                ResolvedSubqueryExpr resolvedSubqueryExpr = (ResolvedSubqueryExpr) resolvedExpr;
                if (resolvedSubqueryExpr.getSubqueryType().toString().equals(SUBQUERY_TYPE_IN)
                    && !(resolvedSubqueryExpr.getSubquery() instanceof ResolvedAggregateScanBase)) {
                  ColumnsOfInExprVisitor columnsOfInExprVisitor =
                      new ColumnsOfInExprVisitor();
                  resolvedSubqueryExpr.getInExpr().accept(columnsOfInExprVisitor);
                  subQueriesInWhereWithoutAgg.add(columnsOfInExprVisitor.getResult());
                }
              }
            });
    super.visit(resolvedFunctionCall);
  }

  public List<String> getResult() {
    return subQueriesInWhereWithoutAgg;
  }

}
