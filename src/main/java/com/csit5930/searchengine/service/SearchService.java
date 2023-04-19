package com.csit5930.searchengine.service;

import com.csit5930.searchengine.model.SearchResult;
import org.rocksdb.RocksDBException;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface SearchService {
    List<SearchResult> search(String query) throws RocksDBException;
}
