package com.google.zetasql.toolkit.antipattern;

import static com.google.zetasql.toolkit.antipattern.util.ZetaSQLHelperConstants.COL_1;
import static com.google.zetasql.toolkit.antipattern.util.ZetaSQLHelperConstants.COL_2;
import static com.google.zetasql.toolkit.antipattern.util.ZetaSQLHelperConstants.TABLE_1_NAME;
import static com.google.zetasql.toolkit.antipattern.util.ZetaSQLHelperConstants.TABLE_2_NAME;
import static com.google.zetasql.toolkit.antipattern.util.ZetaSQLHelperConstants.TABLE_3_NAME;
import static com.google.zetasql.toolkit.antipattern.util.ZetaSQLHelperConstants.TABLE_NAME;

import com.google.api.client.util.DateTime;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.TableId;
import com.google.zetasql.AnalyzerOptions;
import com.google.zetasql.LanguageOptions;
import com.google.zetasql.SimpleCatalog;
import com.google.zetasql.SimpleColumn;
import com.google.zetasql.SimpleTable;
import com.google.zetasql.TypeFactory;
import com.google.zetasql.ZetaSQLType;
import com.google.zetasql.toolkit.antipattern.cmd.BQAntiPatternCMDParser;
import com.google.zetasql.toolkit.antipattern.cmd.InputQuery;
import com.google.zetasql.toolkit.antipattern.util.BigQueryHelper;
import com.google.zetasql.toolkit.ZetaSQLToolkitAnalyzer;
import com.google.zetasql.toolkit.catalog.bigquery.BigQueryCatalog;
import com.google.zetasql.toolkit.options.BigQueryLanguageOptions;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.ParseException;

public class Main {
  private static List<BasePatternDetector> antiPatterList = new ArrayList<>();

  public static void main(String[] args) throws ParseException {
    populateAntiPatterList();
    String processingProject = "pso-dev-whaite";

    // setup analyzer
    AnalyzerOptions options = new AnalyzerOptions();
    LanguageOptions languageOptions = BigQueryLanguageOptions.get().enableMaximumLanguageFeatures();
    languageOptions.setSupportsAllStatementKinds();
    options.setLanguageOptions(languageOptions);
    options.setCreateNewColumnForEachProjectedOutput(true);
    ZetaSQLToolkitAnalyzer analyzer = new ZetaSQLToolkitAnalyzer(options);

    // create BQ catalog
    BigQueryCatalog catalog = new BigQueryCatalog(processingProject);


    BQAntiPatternCMDParser bqAntiPatternCMDParser = new BQAntiPatternCMDParser(args);
    Iterator<InputQuery> inputQueryIterator =  bqAntiPatternCMDParser.getInputQueries();

    processQueries(catalog, options, analyzer, inputQueryIterator, bqAntiPatternCMDParser.getOutputTableProjectId());

  }

  private static void populateAntiPatterList() {
    antiPatterList.add(new IdentidySelectedColumns());
    antiPatterList.add(new IdentifyCrossJoin());
    antiPatterList.add(new IdentifySubqueryInWhere());
  }

  private static void processQueries(
      BigQueryCatalog catalog,
      AnalyzerOptions options,
      ZetaSQLToolkitAnalyzer analyzer,
      Iterator<InputQuery> inputQueryIterator,
      String outputProjectId) {
    List<String> recommendationsList = new ArrayList<>();

    InputQuery inputQuery;
    while (inputQueryIterator.hasNext()) {
      inputQuery = inputQueryIterator.next();
      String query = inputQuery.getQuery();

      catalog.addAllTablesUsedInQuery(query, options);


      antiPatterList.forEach(antiPatter ->{
        String rec = antiPatter.run(query, catalog, analyzer);
        if(rec.length()>0){
          recommendationsList.add(rec);
        }
      });


      String recommendations = String.join("\n", recommendationsList);
      recommendationsList.clear();
      System.out.println(recommendations);

      // Create a map of rows to insert
      Map<String, Object> rowContent = new HashMap<>();
      rowContent.put("job_id", inputQuery.getJobId());
      rowContent.put("query", query);
      rowContent.put("slot_hours", inputQuery.getSlotHours());
      rowContent.put("recommendation", recommendations);
      rowContent.put("process_timestamp", new DateTime(new Date()));

      BigQueryHelper.writeResults(outputProjectId, rowContent);
    }
  }
}
