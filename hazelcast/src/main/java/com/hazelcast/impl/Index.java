package com.hazelcast.impl;

import com.hazelcast.query.Expression;

import java.util.*;

public class Index<T> {
    final Map<Long, Set<T>> mapIndex;
    final String indexName;
    final Expression expression;
    final boolean ordered;

    public Index(String indexName, Expression expression, boolean ordered) {
        this.indexName = indexName;
        this.ordered = ordered;
        this.expression = expression;
        this.mapIndex = (ordered) ? new TreeMap<Long, Set<T>>() : new HashMap<Long, Set<T>>(1000);
    }

    public long extractLongValue(Object value) {
        return QueryService.getLongValue(expression.getValue(value));
    }

    void addNewIndex(long value, T record) {
        Set<T> lsRecords = mapIndex.get(value);
        if (lsRecords == null) {
            lsRecords = new LinkedHashSet<T>();
            mapIndex.put(value, lsRecords);
        }
        lsRecords.add(record);
    }

    void updateIndex(long oldValue, long newValue, T record) {
        if (oldValue == Long.MIN_VALUE) {
            addNewIndex(newValue, record);
        } else {
            if (oldValue != newValue) {
                removeIndex(oldValue, record);
                addNewIndex(newValue, record);
            }
        }
    }

    void removeIndex(long value, T record) {
        Set<T> lsRecords = mapIndex.get(value);
        if (lsRecords != null && lsRecords.size() > 0) {
            lsRecords.remove(record);
            if (lsRecords.size() == 0) {
                mapIndex.remove(value);
            }
        }
    }

    Set<T> getRecords(long value) {
        return mapIndex.get(value);
    }

    Set<T> getSubRecords(boolean equal, boolean lessThan, long value) {
        TreeMap<Long, Set<T>> treeMap = (TreeMap<Long, Set<T>>) mapIndex;
        Set<T> results = new HashSet<T>();
        Map<Long, Set<T>> sub = (lessThan) ? treeMap.headMap(value) : treeMap.tailMap(value);
        Set<Map.Entry<Long, Set<T>>> entries = sub.entrySet();
        for (Map.Entry<Long, Set<T>> entry : entries) {
            if (equal || entry.getKey() != value) {
                results.addAll(entry.getValue());

            }
        }
        return results;
    }

    Set<T> getSubRecords(long from, long to) {
        TreeMap<Long, Set<T>> treeMap = (TreeMap<Long, Set<T>>) mapIndex;
        Set<T> results = new HashSet<T>();
        Collection<Set<T>> sub = treeMap.subMap(from, to).values();
        for (Set<T> records : sub) {
            results.addAll(records);
        }
        return results;
    }


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append("Index");
        sb.append("{indexName='").append(indexName).append('\'');
        sb.append(", size=").append(mapIndex.size());
        sb.append(", ordered=").append(ordered);
        sb.append(", expression=").append(expression.getClass());
        sb.append('}');
        return sb.toString();
    }
}