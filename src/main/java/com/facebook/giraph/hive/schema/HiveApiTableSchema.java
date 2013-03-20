/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.facebook.giraph.hive.schema;

import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;

import com.facebook.giraph.hive.common.Writables;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.facebook.giraph.hive.common.HiveUtils.FIELD_SCHEMA_NAME_GETTER;
import static com.google.common.collect.Lists.transform;

/**
 * Schema for a Hive table
 */
public class HiveApiTableSchema implements HiveTableSchema {
  /** Partition keys */
  private final List<String> partitionKeys;
  /** Positions of columns in the row */
  private final Map<String, Integer> columnPositions;

  /** Number of columns. Not serialized */
  private int numColumns;

  /**
   * Constructor
   */
  public HiveApiTableSchema() {
    partitionKeys = Lists.newArrayList();
    columnPositions = Maps.newHashMap();
    numColumns = 0;
  }

  /**
   * Constructor
   *
   * @param partitionKeys Partition keys
   * @param columnPositions Positions of columns in row
   */
  public HiveApiTableSchema(List<String> partitionKeys,
                            Map<String, Integer> columnPositions) {
    this.partitionKeys = partitionKeys;
    this.columnPositions = columnPositions;
    this.numColumns = computeNumColumns(columnPositions);
  }

  /**
   * Create from a Hive table
   *
   * @param table Hive table
   * @return Schema
   */
  public static HiveApiTableSchema fromTable(Table table) {
    StorageDescriptor storageDescriptor = table.getSd();

    List<String> partitionKeys = transform(table.getPartitionKeys(),
        FIELD_SCHEMA_NAME_GETTER);

    ImmutableMap.Builder<String, Integer> columnPositions =
        ImmutableMap.<String, Integer>builder();
    List<FieldSchema> cols = storageDescriptor.getCols();
    for (int i = 0; i < cols.size(); ++i) {
      columnPositions.put(cols.get(i).getName(), i);
    }

    return new HiveApiTableSchema(partitionKeys, columnPositions.build());
  }

  /**
   * Compute number of columns from the data.
   */
  private static int computeNumColumns(Map<String, Integer> columnPositions) {
    return Ordering.natural().max(columnPositions.values()) + 1;
  }

  @Override
  public int numColumns() {
    return numColumns;
  }

  @Override
  public List<String> partitionKeys() {
    return partitionKeys;
  }

  @Override
  public int positionOf(String columnName) {
    Integer index = columnPositions.get(columnName);
    if (index == null) {
      throw new IllegalArgumentException("Column " + columnName +
          " not found in schema " + this);
    }
    return index;
  }

  @Override
  public void write(DataOutput out) throws IOException {
    Writables.writeStringList(out, partitionKeys);
    Writables.writeStrIntMap(out, columnPositions);
  }

  @Override
  public void readFields(DataInput in) throws IOException {
    Writables.readStringList(in, partitionKeys);
    Writables.readStrIntMap(in, columnPositions);
    numColumns = computeNumColumns(columnPositions);
  }

  @Override
  public String toString() {
    return Objects.toStringHelper(this)
        .add("partitionKeys", partitionKeys)
        .add("columnPositions", columnPositions)
        .toString();
  }
}
