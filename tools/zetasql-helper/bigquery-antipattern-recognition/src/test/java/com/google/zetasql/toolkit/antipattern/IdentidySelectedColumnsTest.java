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


public class IdentidySelectedColumnsTest {

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
  public void selectStarSingleTableTest() {
    String query = "SELECT * FROM `project.dataset.table1`";
    String recommendations =
        new IdentidySelectedColumns()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(
        "All columns on table: project.dataset.table1 are being selected. Please be sure that all"
            + " columns are needed",
        recommendations);
  }

  @Test
  public void selectStarTwoTablesTest() {
    String expected =
        "All columns on table: project.dataset.table1 are being selected. Please be sure that all"
            + " columns are needed\n"
            + "All columns on table: project.dataset.table2 are being selected. Please be sure that"
            + " all columns are needed";
    String query =
        "SELECT "
            + "   t1.*, t2.* "
            + "FROM "
            + "   `project.dataset.table1` t1 "
            + "cross JOIN "
            + "    `project.dataset.table2` t2 "
            + "WHERE "
            + "   t1.col1 = t2.col1 ";
    String recommendations =
        new IdentidySelectedColumns()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(expected, recommendations);
  }

  @Test
  public void noRecommendationTest() {
    String query = "SELECT col1 FROM `project.dataset.table1`";
    String recommendations =
        new IdentidySelectedColumns()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals("", recommendations);
  }

  @Test
  public void selectEachColSingleTableTest() {
    String query = "SELECT col1, col2 FROM `project.dataset.table1`";
    String recommendations =
        new IdentidySelectedColumns()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(
        "All columns on table: project.dataset.table1 are being selected. Please be sure that all"
            + " columns are needed",
        recommendations);
  }

  @Test
  public void selectStarWithJoinTest() {
    String query =
        "\n"
            + "SELECT "
            + "   t1.*, "
            + "   t2.col2 "
            + "FROM \n"
            + "  `project.dataset.table1` t1\n"
            + "LEFT JOIN\n"
            + "  `project.dataset.table2` t2\n ON t1.col1 = t2.col2";
    String recommendations =
        new IdentidySelectedColumns()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(
        "All columns on table: project.dataset.table1 are being selected. Please be sure that all"
            + " columns are needed",
        recommendations);
  }

  @Test
  public void selectStarBothTablesWithJoinTest() {
    String query =
        "\n"
            + "SELECT "
            + "   t1.*, "
            + "   t2.col1, t2.col2, "
            + "FROM \n"
            + "  `project.dataset.table1` t1\n"
            + "LEFT JOIN\n"
            + "  `project.dataset.table2` t2\n ON t1.col1 = t2.col2";
    String recommendations =
        new IdentidySelectedColumns()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(
        "All columns on table: project.dataset.table1 are being selected. Please be sure that all"
            + " columns are needed\n"
            + "All columns on table: project.dataset.table2 are being selected. Please be sure that"
            + " all columns are needed",
        recommendations);
  }

  @Test
  public void selectStarWithJoinAndSubSelectTest() {
    // failing
    String query =
        "\n"
            + "SELECT "
            + "   t1.*, "
            + "   t2.col2 "
            + "FROM \n"
            + "  (SELECT * FROM `project.dataset.table1`) t1\n"
            + "LEFT JOIN\n"
            + "  `project.dataset.table2` t2\n ON t1.col1 = t2.col2";
    String recommendations =
        new IdentidySelectedColumns()
            .run(query, bigQueryCatalog, analyzer);
    assertEquals(
        "All columns on table: project.dataset.table1 are being selected. Please be sure that all"
            + " columns are needed",
        recommendations);
  }

}
