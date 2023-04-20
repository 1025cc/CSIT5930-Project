package com.csit5930.searchengine.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {

    @Test
    void tokenize() {
        String pageBody = "update update update i 324nf hong kong.";
        List<String> pageBodyWords = Tokenizer.tokenize(pageBody);
    }

    @Test
    void tokenizeSingle() {
    }
}