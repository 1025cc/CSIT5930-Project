package com.csit5930.searchengine;

import com.csit5930.searchengine.model.Posting;
import com.csit5930.searchengine.utils.RocksDBUtil;
import com.csit5930.searchengine.utils.SerializationUtil;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class SearchEngineApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    void testDB() throws IOException, ClassNotFoundException {
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add("word_id_to_posting");
        RocksDBUtil rocksDBUtil = new RocksDBUtil("./db", columnFamilies);
        Posting posting1 = new Posting(1,3);
        Posting posting3 = new Posting(3,2);
        List<Posting> list1 = new ArrayList<>();
        list1.add(posting1);
        list1.add(posting3);

        rocksDBUtil.put("word_id_to_posting", SerializationUtil.serialize(1),SerializationUtil.serialize(list1));
        byte[] tmp = rocksDBUtil.get("word_id_to_posting",SerializationUtil.serialize(1));
        List<Posting> postings = (List<Posting>) SerializationUtil.deserialize(tmp);
    }
    @Test
    void testGet() throws IOException, ClassNotFoundException {
        List<String> columnFamilies = new ArrayList<>();
        columnFamilies.add("word_id_to_posting");
        RocksDBUtil rocksDBUtil = new RocksDBUtil("./db", columnFamilies);
        byte[] tmp = rocksDBUtil.get("word_id_to_posting",SerializationUtil.serialize(1));
        List<Posting> postings = (List<Posting>) SerializationUtil.deserialize(tmp);
    }
}
