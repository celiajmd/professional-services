package com.google.zetasql.toolkit.antipattern;

import static com.google.zetasql.toolkit.antipattern.util.ZetaSQLHelperConstants.*;
import static org.junit.Assert.*;

import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.LanguageOptions;
import com.google.zetasql.SimpleCatalog;
import com.google.zetasql.SimpleColumn;
import com.google.zetasql.SimpleTable;
import com.google.zetasql.TypeFactory;
import com.google.zetasql.ZetaSQLType;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class IdentifySubqueryInWhereTest {
  BigQueryCatalog bigQueryCatalog= new BigQueryCatalog("test-project");
  SimpleCatalog catalog = bigQueryCatalog.getZetaSQLCatalog();
  ZetaSQLToolkitAnalyzer analyzer;

  @Before
  public void setUp() {
    AnalyzerOptions options = new AnalyzerOptions();
    LanguageOptions languageOptions = BigQueryLanguageOptions.get().enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
    options.setLanguageOptions(languageOptions);
    options.setCreateNewColumnForEachProjectedOutput(true);
    analyzer = new ZetaSQLToolkitAnalyzer(options);

    SimpleColumn column1 =
        new SimpleColumn(
            TABLE_NAME, COL_1, TypeFactory.createSimpleType(ZetaSQLType.TypeKind.TYPE_STRING));
    SimpleColumn column2 =
        new SimpleColumn(
            TABLE_NAME, COL_2, TypeFactory.createSimpleType(ZetaSQLType.TypeKind.TYPE_STRING));
    SimpleTable table1 = new SimpleTable(TABLE_1_NAME, List.of(column1, column2));
    SimpleTable table2 = new SimpleTable(TABLE_2_NAME, List.of(column1, column2));
    SimpleTable table3 = new SimpleTable(TABLE_3_NAME, List.of(column1, column2));
    catalog.addSimpleTable(table1);
    catalog.addSimpleTable(table2);
    catalog.addSimpleTable(table3);
  }


  @Test
  public void IdentifySubqueryInWhereTest() {
    String expected =
        "You are using an IN filter with a subquery without a DISTINCT on the following columns:"
            + " project.dataset.table1.col2";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "WHERE "
            + "    t1.col2 not in (select col2 from `project.dataset.table2`) ";
    String recommendation =
        new IdentifySubqueryInWhere()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(expected, recommendation);
  }

  @Test
  public void IdentifySubqueryInWhere2CasesTest() {
    String expected =
        "You are using an IN filter with a subquery without a DISTINCT on the following columns:"
            + " project.dataset.table1.col1\n"
            + "You are using an IN filter with a subquery without a DISTINCT on the following"
            + " columns: project.dataset.table1.col2";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "WHERE "
            + "    t1.col1 not in (select col1 from `project.dataset.table2`) "
            + "    AND t1.col2 not in (select col2 from `project.dataset.table2`) ";
    String recommendation =
        new IdentifySubqueryInWhere()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(expected, recommendation);
  }

  @Test
  public void IdentifySubqueryInWhereWithDistinct() {
    String expected = "";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "WHERE "
            + "    t1.col1 not in (select distinct col1 from `project.dataset.table2`) ";
    String recommendation =
        new IdentifySubqueryInWhere()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(expected, recommendation);
  }

  @Test
  public void IdentifySubqueryInWhereWithStruct() {
    String expected =
        "You are using an IN filter with a subquery without a DISTINCT on the "
            + "following columns: project.dataset.table1.col1, project.dataset.table1.col2";
    String query =
        "SELECT "
            + "   t1.col1 "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "WHERE "
            + "    (t1.col1, t1.col2) not in (select (col1, col2) from `project.dataset.table2`) ";
    String recommendation =
        new IdentifySubqueryInWhere()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(expected, recommendation);
  }
}
