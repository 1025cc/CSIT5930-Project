package com.csit5930.searchengine.utils;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {

    @Test
    void tokenize() {
        String query = "\"data science\" world";
        List<String> tokens = Tokenizer.tokenize(query);
    }

    @Test
    void tokenizeSingle() {
    }
}