package com.google.zetasql.toolkit.antipattern.cmd;

import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.TableResult;
import com.google.zetasql.toolkit.antipattern.util.BigQueryHelper;
import java.nio.file.Files;
import java.util.Iterator;

public class InformationSchemaQueryIterable implements Iterator<InputQuery> {

  Iterator<FieldValueList> fieldValueListIterator;

  public InformationSchemaQueryIterable(String project_id) throws InterruptedException {
    TableResult tableResult = BigQueryHelper.getQueries(project_id);
    fieldValueListIterator = tableResult.iterateAll().iterator();
  }

  @Override
  public boolean hasNext() {
    return fieldValueListIterator.hasNext();
  }

  @Override
  public InputQuery next() {
    FieldValueList row = fieldValueListIterator.next();
    String job_id = row.get("job_id").getStringValue();
    String query = row.get("query").getStringValue();
    String slot_hours = row.get("slot_hours").getStringValue();
    return new InputQuery(query, job_id, Float.parseFloat(slot_hours));
  }
}
