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
package com.pso.bigquery.optimization;

import com.google.zetasql.SimpleCatalog;
import com.pso.bigquery.optimization.analysis.QueryAnalyzer;
import com.pso.bigquery.optimization.analysis.visitors.subqueryinwhere.SubqueryInWhereVisitor;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;

public class IdentifySubqueryInWhere implements BasePatternDetector {

  private final String SUBQUERY_IN_WHERE_SUGGESTION_MESSAGE =
      "You are using an IN filter with a subquery without a DISTINCT on the following columns: ";
  ArrayList<String> result = new ArrayList<String>();

  public String run(
      String query,
      String billingProjectId,
      SimpleCatalog catalog,
      QueryAnalyzer.CatalogScope catalogScope) {

    SubqueryInWhereVisitor visitor = new SubqueryInWhereVisitor(billingProjectId, catalog);
    parser.visitQuery(billingProjectId, query, catalog, catalogScope, visitor);
    visitor.getResult().forEach(subqueryInWhereColumns -> parseResults(subqueryInWhereColumns));

    return StringUtils.join(result, "\n");
  }

  public void parseResults(String subqueryInWhereColumns) {
    result.add(SUBQUERY_IN_WHERE_SUGGESTION_MESSAGE + subqueryInWhereColumns);
  }
}
