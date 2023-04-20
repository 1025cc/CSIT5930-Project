package com.csit5930.searchengine;

import com.csit5930.searchengine.indexer.Indexer;
import com.csit5930.searchengine.model.PageInfo;
import com.csit5930.searchengine.model.SearchResult;
import com.csit5930.searchengine.service.PageRank;
import com.csit5930.searchengine.service.SearchService;
import com.csit5930.searchengine.model.Posting;
import com.csit5930.searchengine.model.WebPage;
import com.csit5930.searchengine.utils.RocksDBUtil;
import com.csit5930.searchengine.utils.SerializationUtil;
import com.csit5930.searchengine.utils.Tokenizer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class SearchEngineApplicationTests {
    @Autowired
    private Indexer indexer;

    @Autowired
    private SearchService searchService;
    @Test
    void testDB() {
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add("word_id_to_posting");
        RocksDBUtil rocksDBUtil = new RocksDBUtil("./db", columnFamilies);
        Posting posting1 = new Posting(1,3,new ArrayList<>());
        Posting posting3 = new Posting(3,2,new ArrayList<>());
        List<Posting> list1 = new ArrayList<>();
        list1.add(posting1);
        list1.add(posting3);
        rocksDBUtil.put("word_id_to_posting", SerializationUtil.serialize(1),SerializationUtil.serialize(list1));
        byte[] tmp = rocksDBUtil.get("word_id_to_posting",SerializationUtil.serialize(1));
        List<Posting> postings = (List<Posting>) SerializationUtil.deserialize(tmp);
        rocksDBUtil.close();
    }
    @Test
    void testClose() {
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add("word_id_to_posting");
        RocksDBUtil rocksDBUtil = new RocksDBUtil("./db", columnFamilies);

    }

    @Test
    void testGet(){
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add("word_id_to_posting");
        RocksDBUtil rocksDBUtil = new RocksDBUtil("./db", columnFamilies);
        Integer tmp = (Integer) rocksDBUtil.get("word_id_to_posting",2);
        //List<Posting> postings = (List<Posting>) SerializationUtil.deserialize(tmp);
    }
    @Test
    void testIndexer(){
        WebPage webPage1 = new WebPage("This is an example page about artificial intelligence.", "AI Introduction", "https://example.com/ai-introduction", "2022-11-12", 300);
        WebPage webPage2 = new WebPage("Learn about the history of programming languages.", "Programming Languages History", "https://example.com/programming-history", "2022-11-11", 350);
        WebPage webPage3 = new WebPage("Explore the world of software engineering practices and principles.", "Software Engineering Basics", "https://example.com/software-engineering", "2022-11-10", 400);
        WebPage webPage4 = new WebPage("Discover the importance of cybersecurity in today's digital world.", "Cybersecurity Essentials", "https://example.com/cybersecurity", "2022-11-09", 450);
        WebPage webPage5 = new WebPage("Dive into the fascinating world of data science and its applications.", "Data Science Overview", "https://example.com/data-science", "2022-11-08", 500);
        indexer.indexPage(webPage1);
        indexer.indexPage(webPage2);
        indexer.indexPage(webPage3);
        indexer.indexPage(webPage4);
        indexer.indexPage(webPage5);
        int n = indexer.getTotalPageNum();
        //searchService.search("history");
        indexer.close();
    }
    @Test
    void testTTT(){
        System.out.println("==============before==============");
        indexer.displayAllIndex();
        System.out.println("==============after==============");
        WebPage webPage1 = new WebPage("update update update i 324nf hong kong.", "AI Introduction new", "https://example.com/ai-introduction", "2022-11-12", 300);
        indexer.updatePage(webPage1);
        indexer.displayAllIndex();
        indexer.close();
    }

    @Test
    void testIndexer1(){
        //WebPage webPage1 = new WebPage("update update update i 324nf \"hong kong\".", "AI Introduction new", "https://example.com/ai-introduction", "2022-11-12", 300);
        //indexer.indexPage(webPage1);
        //indexer.displayAllIndex();
        PageInfo pageInfo= indexer.getPageInfoById(111);
        indexer.close();
    }

    @Test
    void testResult(){
        List<SearchResult> results = searchService.search("AI");
        System.out.println(results);
    }
}
