package com.pso.bigquery.optimization;

import com.google.zetasql.*;
import com.pso.bigquery.optimization.analysis.QueryAnalyzer;

import java.text.ParseException;
import java.util.List;

import static com.pso.bigquery.optimization.util.ZetaSQLHelperConstants.*;
import org.apache.commons.cli.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static String getInputQuery(String[] args)throws ParseException, IOException{

        String query_str = "\n" +
                "SELECT " +
                "   t1.*, " +
                "   t2.col2 " +
                "FROM \n" +
                "  (SELECT * FROM `project.dataset.table1`) t1\n" +
                "LEFT JOIN\n" +
                "  `project.dataset.table2` t2\n ON t1.col1 = t2.col2";;

        Options options = new Options();

        Option query = Option.builder("query")
                .argName("query")
                .hasArg()
                .required(false)
                .desc("set query").build();
        options.addOption(query);

        Option file_path = Option.builder("file_path")
                .argName("file_path")
                .hasArg()
                .required(false)
                .desc("set file path").build();
        options.addOption(file_path);

        CommandLine cmd;
        CommandLineParser parser = new BasicParser();
        HelpFormatter helper = new HelpFormatter();

        try {
            cmd = parser.parse(options, args);
            if(cmd.hasOption("query")){
                query_str = cmd.getOptionValue("query");
                System.out.println("query detected as " + query_str);
            }
            else if(cmd.hasOption("file_path")){
                String file_path_str = cmd.getOptionValue("file_path");
                Path file_name = Path.of(file_path_str);
                query_str = Files.readString(file_name);
                System.out.println("file path detected as " + file_path_str);
            }
        } catch (org.apache.commons.cli.ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(0);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(0);
        }
        return query_str;
    }


    public static void main(String[] args) throws ParseException, IOException{
        String query_str = getInputQuery(args);

        String billing_project = MY_PROJET;
        SimpleCatalog catalog =  new SimpleCatalog(CATALOG_NAME);

        catalog.addZetaSQLFunctions(new ZetaSQLBuiltinFunctionOptions());
        SimpleColumn column1 = new SimpleColumn(
                TABLE_NAME,
                COL_1,
                TypeFactory.createSimpleType(ZetaSQLType.TypeKind.TYPE_STRING)
        );
        SimpleColumn column2 = new SimpleColumn(
                TABLE_NAME,
                COL_2,
                TypeFactory.createSimpleType(ZetaSQLType.TypeKind.TYPE_STRING)
        );
        SimpleTable table1 = new SimpleTable(TABLE_1_NAME, List.of(column1, column2));
        SimpleTable table2 = new SimpleTable(TABLE_2_NAME, List.of(column1, column2));
        SimpleTable table3 = new SimpleTable(TABLE_3_NAME, List.of(column1, column2));
        catalog.addSimpleTable(table1);
        catalog.addSimpleTable(table2);
        catalog.addSimpleTable(table3);



        System.out.println(new IdentidySelectedColumns().run(query_str, billing_project, catalog, QueryAnalyzer.CatalogScope.MANUAL));
    }

}
