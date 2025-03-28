/*
 * Copyright 2023 Google LLC All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pso.bigquery.optimization.analysis.visitors.crossjoin;

import com.google.zetasql.SimpleCatalog;
import com.google.zetasql.resolvedast.ResolvedNodes.*;
import com.pso.bigquery.optimization.analysis.visitors.BaseAnalyzerVisitor;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class CrossJoinVisitor extends BaseAnalyzerVisitor {

  private final String INNER_JOIN_TYPE = "INNER";
  private List<CrossJoin> crossJoinList = new ArrayList<>();
  private Stack<ResolvedExpr> filterStack = new Stack<ResolvedExpr>();
  private FindFilterForCrossJoinVisitor findFilterForCrossJoinVisitor = null;

  public CrossJoinVisitor(String projectId, SimpleCatalog catalog) {
    super(projectId, catalog);
  }

  public void visit(ResolvedJoinScan resolvedJoinScan) {
    checkForCrossJoin(resolvedJoinScan);
    super.visit(resolvedJoinScan);
  }

  public void visit(ResolvedFilterScan resolvedFilterScan) {
    filterStack.push(resolvedFilterScan.getFilterExpr());
    super.visit(resolvedFilterScan);
    filterStack.pop();
  }

  public void checkForCrossJoin(ResolvedJoinScan resolvedJoinScan) {
    ResolvedExpr joinsExpr = resolvedJoinScan.getJoinExpr();
    if (joinsExpr == null
        && resolvedJoinScan.getJoinType().toString().equals(INNER_JOIN_TYPE)
        && filterStack.size() > 0) {

      CrossJoinChildNode leftNode = new CrossJoinChildNode(resolvedJoinScan.getLeftScan());
      CrossJoinChildNode rightNode = new CrossJoinChildNode(resolvedJoinScan.getRightScan());
      CrossJoin crossJoin = new CrossJoin(leftNode, rightNode);

      findFilterForCrossJoinVisitor =
          new FindFilterForCrossJoinVisitor(super.getProjectId(), super.getCatalog());
      findFilterForCrossJoinVisitor.setCrossJoin(crossJoin);

      filterStack.peek().accept(findFilterForCrossJoinVisitor);
      if (findFilterForCrossJoinVisitor.result()) {
        crossJoinList.add(crossJoin);
      }
    }
  }

  public List<CrossJoin> getResult() {
    return crossJoinList;
  }
}
