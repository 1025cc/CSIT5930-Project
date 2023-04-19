package com.csit5930.searchengine.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RocksDBUtilTest {

    @Test
    void displayAllIndexes() {
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add("word_to_word_id");
        columnFamilies.add("url_to_page_id");
        columnFamilies.add("word_id_to_posting");
        columnFamilies.add("page_id_to_page_info");
        columnFamilies.add("page_id_to_child_pages") ;
        columnFamilies.add("page_id_to_parent_pages") ;
        //forward index, storing the keywords and frequencies for each page
        columnFamilies.add("page_id_to_keywords");
        RocksDBUtil rocksDBUtil = new RocksDBUtil("./db", columnFamilies);
        rocksDBUtil.displayAllIndexes();
    }
}