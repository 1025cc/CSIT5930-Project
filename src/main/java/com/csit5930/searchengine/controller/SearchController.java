package com.csit5930.searchengine.controller;

import com.csit5930.searchengine.service.SearchService;
import com.csit5930.searchengine.model.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SearchController {

    @Autowired
    private SearchService searchService;


    @GetMapping("/search")
    public ResponseEntity<List<SearchResult>> search(@RequestParam("query") String query) {
        List<SearchResult> searchResults = searchService.search(query);
        return ResponseEntity.status(HttpStatus.OK).body(searchResults);
    }
}

