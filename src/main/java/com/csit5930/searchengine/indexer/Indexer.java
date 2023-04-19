package com.csit5930.searchengine.indexer;

import com.csit5930.searchengine.model.Posting;
import com.csit5930.searchengine.utils.RocksDBUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class Indexer {
    private RocksDBUtil rocksDBUtil;
    private String dbPath = "";

    public Indexer() {
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add("word_to_word_id");
        columnFamilies.add("url_to_page_id");
        columnFamilies.add("word_id_to_posting");
        columnFamilies.add("page_id_to_page_info");
        rocksDBUtil = new RocksDBUtil(dbPath, columnFamilies);
    }


    public List<Posting> getPostingListByWord(String word) {
        return null;
    }

    public List<Integer> getWordPositions(String word,int pageId) {
        return null;
    }

    public int getTfMax(int pageId) {
        return 0;
    }

    public int getTotalDocumentNum() {
        return 0;
    }

    public List<Integer> getTitleMatchedPages(String token) {
        return null;
    }
}
