package com.csit5930.searchengine.utils;

import org.rocksdb.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * RocksDBUtil
 * Persistent index files
 */
public class RocksDBUtil {
    private RocksDB db;
    static {
        RocksDB.loadLibrary();
    }

    private List<ColumnFamilyHandle> cfHandles = new ArrayList<>();

    public RocksDBUtil(String dbPath, List<String> columnFamilies) {
        Options options = new Options().setCreateIfMissing(true);
        ColumnFamilyOptions cfOptions = new ColumnFamilyOptions().optimizeUniversalStyleCompaction();
        List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>();
        DBOptions dbOptions = new DBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true);
        try {
            List<byte[]> cfNames = RocksDB.listColumnFamilies(options, dbPath);
            if (cfNames.isEmpty()) {
                cfDescriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOptions));
            } else {
                for (byte[] cfName : cfNames) {
                    cfDescriptors.add(new ColumnFamilyDescriptor(cfName, cfOptions));
                }
            }

            db = RocksDB.open(dbOptions, dbPath, cfDescriptors, cfHandles);

            for (String cf : columnFamilies) {
                boolean exists = false;
                for (byte[] cfName : cfNames) {
                    if (new String(cfName).equals(cf)) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    ColumnFamilyHandle cfHandle = db.createColumnFamily(new ColumnFamilyDescriptor(cf.getBytes(), cfOptions));
                    cfHandles.add(cfHandle);
                }
            }

        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public void put(String columnFamily, byte[] key, byte[] value) {
        try {
            ColumnFamilyHandle cfHandle = getColumnFamilyHandle(columnFamily);
            if (cfHandle != null) {
                db.put(cfHandle, key, value);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
    }

    public byte[] get(String columnFamily, byte[] key) {
        try {
            ColumnFamilyHandle cfHandle = getColumnFamilyHandle(columnFamily);
            if (cfHandle != null) {
                return db.get(cfHandle, key);
            }
        } catch (RocksDBException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        for (ColumnFamilyHandle cfHandle : cfHandles) {
            cfHandle.close();
        }
        db.close();
    }

    private ColumnFamilyHandle getColumnFamilyHandle(String columnFamily) throws RocksDBException {
        for (ColumnFamilyHandle cfHandle : cfHandles) {
            if (new String(cfHandle.getName()).equals(columnFamily)) {
                return cfHandle;
            }
        }
        return null;
    }
}

