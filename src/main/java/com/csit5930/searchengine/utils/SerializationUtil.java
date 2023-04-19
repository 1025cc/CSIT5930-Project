package com.csit5930.searchengine.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;


public class SerializationUtil {
    private static final Logger logger = LoggerFactory.getLogger(SerializationUtil.class);
    public static byte[] serialize(Object object){
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(object);
            return bos.toByteArray();
        }catch (IOException e) {
            logger.error("Error during serialization: {}", e.getMessage(), e);
            return null;
        }
    }

    public static Object deserialize(byte[] data){
        if(data == null){
            return null;
        }
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return ois.readObject();
        }catch (IOException | ClassNotFoundException e) {
            logger.error("Error during deserialization: {}", e.getMessage(), e);
            return null;
        }
    }
}

