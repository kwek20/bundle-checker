package org.iota.theft_leftover;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ExchangeLists {
    
    public static final List<String> addresses = new LinkedList<String>() {{
        add("999999999999999999999999999999999999999999999999999999999999999999999999999999999");
    }};

    public static final List<String> bundleHashes = new LinkedList<String>() {{
        add("999999999999999999999999999999999999999999999999999999999999999999999999999999999");
    }};
    
    public static final List<String> otherAddresses = loadFromDisk("otherAddresses.txt");
    
    private static List<String> loadFromDisk(String name) {
        List<String> hashes = new ArrayList<>();  
        try (BufferedReader br = new BufferedReader(new FileReader(name))) {
            String line;
            while ((line = br.readLine()) != null) {
                hashes.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hashes;
    }
}
