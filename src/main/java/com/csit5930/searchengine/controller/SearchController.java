package com.csit5930.searchengine.controller;

import com.csit5930.searchengine.service.SearchService;
import com.csit5930.searchengine.model.SearchResult;
import org.rocksdb.RocksDBException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class SearchController {

    @Autowired
    private SearchService searchService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam("query") String query, Model model) throws RocksDBException {
        List<SearchResult> searchResults = searchService.search(query);
        model.addAttribute("searchResults", searchResults);
        return "results";
    }
}

