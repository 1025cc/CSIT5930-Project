package com.csit5930.searchengine.utils;

import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RocksDBUtil
 * Persistent index files
 */
public class RocksDBUtil {
    private static final Logger logger = LoggerFactory.getLogger(RocksDBUtil.class);
    private RocksDB db;
    static {
        RocksDB.loadLibrary();
    }

    private Map<String, ColumnFamilyHandle> columnFamilyHandleMap = new HashMap<>();


    public RocksDBUtil(String dbPath, List<String> columnFamilies) {
        try {
            List<ColumnFamilyDescriptor> columnFamilyDescriptors = new ArrayList<>();

            // Add default column family
            columnFamilyDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));

            // Add user-defined column families
            for (String columnFamilyName : columnFamilies) {
                columnFamilyDescriptors.add(new ColumnFamilyDescriptor(columnFamilyName.getBytes()));
            }
            DBOptions dbOptions = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
            List<ColumnFamilyHandle> columnFamilyHandles = new ArrayList<>();
            db = RocksDB.open(dbOptions, dbPath, columnFamilyDescriptors, columnFamilyHandles);

            for (int i = 0; i < columnFamilies.size(); i++) {
                columnFamilyHandleMap.put(columnFamilies.get(i), columnFamilyHandles.get(i + 1));
            }

        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void put(String columnFamily, byte[] key, byte[] value) {
        try {
            ColumnFamilyHandle cfHandle = columnFamilyHandleMap.get(columnFamily);
            if (cfHandle != null) {
                db.put(cfHandle, key, value);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }
    public void put(String columnFamily, Object key, Object value) {
        put(columnFamily,SerializationUtil.serialize(key),SerializationUtil.serialize(value));
    }


    public byte[] get(String columnFamily, byte[] key) {
        try {
            ColumnFamilyHandle cfHandle = columnFamilyHandleMap.get(columnFamily);
            if (cfHandle != null) {
                return db.get(cfHandle, key);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }
    public Object get(String columnFamily, Object key) {
        return SerializationUtil.deserialize(get(columnFamily,SerializationUtil.serialize(key)));
    }

    public byte[] get(byte[] key){
        byte[] value;
        try {
            value = db.get(key);
            return value;
        } catch (RocksDBException e) {
            logger.error("Error while getting key from default column family: {}", e.getMessage(), e);
        }
        return null;
    }

    public void close() {
        for (ColumnFamilyHandle columnFamilyHandle : columnFamilyHandleMap.values()) {
            columnFamilyHandle.close();
        }
        db.close();
    }

    public static long countKeysInColumnFamily(RocksDB db, ColumnFamilyHandle columnFamilyHandle) {
        long count = 0;
        try (RocksIterator iterator = db.newIterator(columnFamilyHandle)) {
            for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                count++;
            }
        }
        return count;
    }
}

