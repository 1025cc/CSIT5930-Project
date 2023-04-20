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
            logger.error("Error while openning RocksDBUtil");
        }
    }

    public void put(String columnFamily, byte[] key, byte[] value) {
        try {
            ColumnFamilyHandle cfHandle = columnFamilyHandleMap.get(columnFamily);
            if (cfHandle != null) {
                db.put(cfHandle, key, value);
            }
        } catch (RocksDBException e) {
            logger.error("Error while putting key into column family {}: {}", columnFamily,e.getMessage());
        }
    }
    public void put(String columnFamily, Object key, Object value) {
        put(columnFamily,SerializationUtil.serialize(key),SerializationUtil.serialize(value));
    }
    public byte[] put(byte[] key, byte[] value) {
        try {
            db.put(key, value);
            return key;
        } catch (RocksDBException e) {
            logger.error("Error putting key-value pair to RocksDB database", e);
            return null;
        }
    }

    public byte[] get(String columnFamily, byte[] key) {
        if(key == null){
            return null;
        }
        try {
            ColumnFamilyHandle cfHandle = columnFamilyHandleMap.get(columnFamily);
            if (cfHandle != null) {
                return db.get(cfHandle, key);
            }
        } catch (RocksDBException e) {
            logger.error("Error while getting key from column family {}: {}", columnFamily,e.getMessage());
        }
        return null;
    }
    public Object get(String columnFamily, Object key) {
        return SerializationUtil.deserialize(get(columnFamily,SerializationUtil.serialize(key)));
    }

    public byte[] get(byte[] key){
        if(key == null){
            return null;
        }
        byte[] value;
        try {
            value = db.get(key);
            return value;
        } catch (RocksDBException e) {
            logger.error("Error while getting key from default column family: {}", e.getMessage());
        }
        return null;
    }

    public void close() {
        for (ColumnFamilyHandle columnFamilyHandle : columnFamilyHandleMap.values()) {
            columnFamilyHandle.close();
        }
        db.close();
    }
    public void displayAllIndexes() {
        for (String columnFamily : columnFamilyHandleMap.keySet()) {
            System.out.println("Column Family: " + columnFamily);
            ColumnFamilyHandle columnFamilyHandle = columnFamilyHandleMap.get(columnFamily);

            try (RocksIterator iterator = db.newIterator(columnFamilyHandle)) {
                for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                    Object key = SerializationUtil.deserialize(iterator.key());
                    Object value = SerializationUtil.deserialize(iterator.value());
                    System.out.println("Key: " + key + ", Value: " + value);
                }
            } catch (Exception e) {
                logger.error("Error during displaying all indexes: {}", e.getMessage());
            }
        }
    }
    public void delete(String columnFamily,byte[] key) {
        try {
            ColumnFamilyHandle cfHandle = columnFamilyHandleMap.get(columnFamily);
            if (cfHandle != null) {
                db.delete(cfHandle, key);
            }
        } catch (RocksDBException e) {
            logger.error("Error deleting key from RocksDB: {}", e.getMessage());
        }
    }

    public void delete(String columnFamily, Object key) {
        byte[] tmp = SerializationUtil.serialize(key);
        delete(columnFamily,tmp);
    }
}

