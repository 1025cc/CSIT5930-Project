package com.csit5930.searchengine.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SerializationUtilTest {
    @Test
    void testSerialize(){
        byte[] data = new byte[]{-84, -19, 0, 5, 112};
        int o = (int)SerializationUtil.deserialize(data);
    }
}