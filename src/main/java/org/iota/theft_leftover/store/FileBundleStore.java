package org.iota.theft_leftover.store;

import org.iota.jota.model.Bundle;
import org.iota.jota.model.Transaction;

import java.io.*;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class FileBundleStore implements BundleStore {
    

    Map<String, List<Bundle>> bundleStore = new HashMap<String, List<Bundle>>();
    private File file;
    
    
    public FileBundleStore(String fileName) throws IOException {
        this.file = new File(fileName);
        if (!file.exists()) {
            file.createNewFile();
        }
    }


    @Override
    public void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            int j=0;
            while ((line = br.readLine()) != null) {
                if (++j % 10 == 0) {
                    System.out.println("Processing... " + j);
                }
                
               String[] data = line.split(";");
               String address = data[0];
               
               List<Bundle> bundles = new LinkedList<Bundle>();
               for (int i = 1; i < data.length; i++) {
                   String addressData = data[i];
                   String[] bundlesTx = addressData.split(":");
                   
                   Bundle b = new Bundle();
                   for (String tx : bundlesTx) {
                       Transaction t = new Transaction(tx);
                       b.addTransaction(t);
                   }
                   b.setLength(b.getTransactions().size());
                   bundles.add(b);
               }
               bundleStore.put(address, bundles);
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public boolean contains(String address) {
        return bundleStore.containsKey(address);
    }


    @Override
    public List<Bundle> get(String address) {
        return bundleStore.get(address);
    }


    @Override
    public void put(String address, List<Bundle> bundles) {
        bundleStore.put(address, bundles);
        
        try {
            saveBundle(address, bundles);
        } catch (IOException e) {
            System.out.println("Failed to store bundles for " + address);
            e.printStackTrace();
        }
    }
    
    private void saveBundle(String address, List<Bundle> bundles) throws IOException {
        try (FileWriter fr = new FileWriter(file, true); BufferedWriter br = new BufferedWriter(fr)) {
            System.out.println("Stored " + address);
            String localStore = bundleToString(address, bundles);
            br.write(localStore);
            br.newLine();
        } catch (Exception e) {
            throw e;
        }
    }


    private String bundleToString(String address, List<Bundle> bundles) {
        StringBuilder builder = new StringBuilder(address);
        for (Bundle b : bundles) {
            builder.append(";");

            for (int i = 0; i < b.getTransactions().size(); i++) {
                builder.append(b.getTransactions().get(i).toTrytes());
                if (i != b.getTransactions().size()) {
                    builder.append(":");
                }
            }
        }
        return builder.toString();
    }
}
