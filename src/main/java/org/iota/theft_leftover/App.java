package org.iota.theft_leftover;

import org.iota.jota.IotaAPI;
import org.iota.jota.connection.HttpConnector;
import org.iota.jota.dto.response.FindTransactionResponse;
import org.iota.jota.error.ArgumentException;
import org.iota.jota.model.Bundle;
import org.iota.jota.model.Transaction;
import org.iota.jota.utils.Checksum;
import org.iota.theft_leftover.display.Graph;
import org.iota.theft_leftover.display.Graph.GraphPoint;
import org.iota.theft_leftover.display.GraphStream;
import org.iota.theft_leftover.store.BundleStore;
import org.iota.theft_leftover.store.FileBundleStore;

import java.io.*;
import java.net.URL;
import java.util.*;

public class App {

    public static final String node = "http://node02.iotatoken.nl:14265";

    // Bundle hashes we start searchign from
    public static final String startDoc = "involvedTxs.txt";
    // Place we store our cached tx data
    public static final String storeDoc = "storedResults.txt";
    // Places where theres funds in the address
    public static final String theftAddressesFile = "theftAddresses.txt";
    //Unknown inputs; to investigate
    public static final String newInputs = "newInputAddresses.txt";
    
    Graph graph;

    List<String> startingBundles = new LinkedList<String>();
    List<String> startingAddresses = new LinkedList<String>();
    List<String> transactionsAffected = new LinkedList<String>();

    Set<String> unknownInputs = new HashSet<>();

    BundleStore bundleStore = new FileBundleStore(storeDoc);

    private IotaAPI api;

    private List<String> theftAddresses = new LinkedList<String>();
    private long totalTheft = 0;

    App(IotaAPI api) throws FileNotFoundException, IOException {
        this.api = api;
        graph = new GraphStream();
        
        loadBundlesFromBundleHashes(startDoc);
        
        loadBundlesFromTransactions();
        
        startScan();

        writeThefts();

        logNewInputs();
    }

    private void startScan() {

        Deque<String> bundlesToExamine = new ArrayDeque<String>();
        for (String startingBundleAddress : startingAddresses) {
            bundlesToExamine.add(startingBundleAddress);
        }

        try {

            String currentAddress;
            Set<String> checkedAddresses = new HashSet<>();
            while ((currentAddress = bundlesToExamine.poll()) != null) {
                Deque<String> toProcess = new ArrayDeque<String>();
                toProcess.add(currentAddress);

                while ((currentAddress = toProcess.poll()) != null) {
                    if (!checkedAddresses.add(currentAddress)) {
                        continue;
                    }

                    List<Bundle> bundles = bundleStore.get(currentAddress);
                    if (bundles == null) {
                        System.out.println("### Found an unknown bundle at address " + currentAddress);
                        bundles = findAndStoreBundlesFrom(currentAddress);
                        if (bundles == null) {
                            System.out.println("!·&$!/·%$&!/·%$(& WERE IN  TROUBLE AT " + currentAddress);
                            continue;
                        }
                    }

                    boolean exchangeHit = checkExchange(bundles, currentAddress);

                    // Checks for an address with only inputs, which we didnt start from
                    // As for those only the spent bundle is added
                    if (noOutputs(bundles, currentAddress)
                            && !startingBundles.contains(bundles.get(0).getBundleHash())) {
                        if (!theftAddresses.contains(currentAddress)) {
                            long theftAmount = getTheft(bundles.get(0), currentAddress);
                            //System.out.println(currentAddress + " (" + theftAmount + ")");
                            
                            graph.setType(currentAddress, Graph.GraphPoint.FUNDS, bundles.get(0).getBundleHash());
                            theftAddresses.add(currentAddress);
                            totalTheft += theftAmount;

                            // Some bundles are detected from here, without knowing the address through below
                            for (String prevAddrs : getPreviousAddresses(bundles.get(0))) {
                                if (!graph.hasNode(prevAddrs)) {
                                    addNode(currentAddress, prevAddrs, Graph.GraphPoint.UNKNOWN_STUCK, bundles.get(0).getBundleHash());
                                }
                            }
                        }
                    } else {
                        for (Bundle bundle : bundles) {
                            if (spentBundle(bundle, currentAddress)) {
                                Collection<? extends String> addresses = getNextAddresses(bundle, currentAddress);
                                for (String addrToAdd : addresses) {
                                    if (!checkedAddresses.contains(addrToAdd) && !toProcess.contains(addrToAdd)) {
                                        graph.addNode(addrToAdd, Graph.GraphPoint.INTERMEDIATE, bundle.getBundleHash());
                                        if (!exchangeHit) {
                                            toProcess.add(addrToAdd);
                                        }
                                    }

                                    graph.addEdge(currentAddress, addrToAdd);
                                }
                            } else {
                                for (String prevAddrs : getPreviousAddresses(bundle)) {
                                    if (!graph.hasNode(prevAddrs)) {
                                        addNode(currentAddress, prevAddrs, Graph.GraphPoint.UNKNOWN, bundle.getBundleHash());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // hmm
            e.printStackTrace();
        }

        graph.display();
    }
    
    private boolean checkExchange(List<Bundle> bundles, String address) {
        boolean exchangeFound = false;
        for (Bundle b : bundles) {
            if (ExchangeLists.bundleHashes.contains(b.getBundleHash())) {
                for (Transaction t : b.getTransactions()) {
                    if (!graph.hasNode(t.getAddress())) {
                        graph.addNode(t.getAddress(), Graph.GraphPoint.BIG, b.getBundleHash());
                        graph.addEdge(t.getAddress(), address);
                    }
                    
                    graph.setType(t.getAddress(), Graph.GraphPoint.BIG, b.getBundleHash());
                    if (t.getValue() > 0) {
                    }
                }
                exchangeFound = true;
            }
            
            for (Transaction t : b.getTransactions()) {
                if (ExchangeLists.addresses.contains(t.getAddress())){
                    graph.addNode(t.getAddress(), Graph.GraphPoint.BIG, b.getBundleHash());
                    graph.setType(t.getAddress(), Graph.GraphPoint.BIG, b.getBundleHash());
                      return true;
                  }
            }
            
        }
        return exchangeFound;
    }

    private void writeThefts() {
        System.out.println("Total stolen: " + totalTheft);
        try (FileWriter fr = new FileWriter(new File(theftAddressesFile), true);
                BufferedWriter br = new BufferedWriter(fr)) {
            int i = 0;
            for (String addr : theftAddresses) {
                if (++i % 100 == 0) {
                    System.out.println("Processing... " + i);
                }
                br.write(addr);
                br.newLine();
            }
            br.close();
            fr.close();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
    
    private void logNewInputs() {
        try (FileWriter fr = new FileWriter(new File(newInputs), true); BufferedWriter br = new BufferedWriter(fr)) {

            for (String addr : unknownInputs) {
                if (!bundleStore.contains(addr)) {
                    br.write(addr);
                    br.newLine();
                }
            }
            br.close();
            fr.close();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }

    private void addNode(String currentAddress, String prevAddrs, GraphPoint type, String bundle) {
        graph.addNode(prevAddrs, type, bundle);
        graph.addEdge(prevAddrs, currentAddress);
    }

    private boolean noOutputs(List<Bundle> bundles, String address) {
        for (Bundle b : bundles) {
            if (spentBundle(b, address)) {
                return false;
            }
        }
        return true;
    }

    private List<Bundle> findAndStoreBundlesFrom(String address) throws IOException {
        try {
            FindTransactionResponse ftr = api
                    .findTransactionsByAddresses(new String[] { Checksum.addChecksum(address) });
            if (ftr == null || ftr.getHashes() == null) {
                return null;
            }

            if (ftr.getHashes().length == 0) {
                System.out.println("Could not find data on " + address);
                return null;
            }

            String[] whatWeDo = Arrays.copyOfRange(ftr.getHashes(), 0,
                    ftr.getHashes().length > 100 ? 100 : ftr.getHashes().length);
            List<Bundle> bundles = Arrays.asList(manualSort(api.findTransactionsObjectsByHashes(whatWeDo), address));
            for (Bundle b : bundles) {
                b.setLength(b.getTransactions().size());
            }

            bundleStore.put(address, bundles);
            return bundles;
        } catch (Exception e) {
            return null;
        }
    }
    
    private Bundle[] manualSort(List<Transaction> trxs, String address) {
        // set of tail transactions
        Map<String, Bundle> bundles = new HashMap<String, Bundle>();
        
        for (Transaction trx : trxs) {
            if (!bundles.containsKey(trx.getBundle())) {
                bundles.put(trx.getBundle(), new Bundle());
            }
        }
        
        for (String bundle : bundles.keySet()) {
            String[] txHashes = api.findTransactionsByBundles(bundle).getHashes();
            Transaction tail = null;
            for (String txHash : txHashes) {
                Transaction tx = new Transaction(api.getTrytes(txHash).getTrytes()[0]);
                if (tx.getCurrentIndex() == 0) {
                    tail = tx;
                    bundles.get(bundle).addTransaction(tx);
                    txHashes = null;
                    break;
                }
            }
            
            String next = tail.getTrunkTransaction();
            while (next != null) {
                Transaction tx = new Transaction(api.getTrytes(next).getTrytes()[0]);
                next = tx.getTrunkTransaction();
                bundles.get(bundle).addTransaction(tx);
                
                if (tx.getCurrentIndex() == tx.getLastIndex()) {
                    next = null;
                    break;
                }
            }
        }
        
        return bundles.values().toArray(new Bundle[0]);
    }

    private void loadBundlesFromBundleHashes(String bundleFile) throws FileNotFoundException, IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(bundleFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("https://thetangle.org/bundle/")) {
                    line = line.substring("https://thetangle.org/bundle/".length());
                }
                startingBundles.add(line);
                try {
                    List<Transaction> txs = api.findTransactionObjectsByBundle(line);
                    List<String> toAddress = new LinkedList<>();

                    for (Transaction t : txs) {
                        if (t.getValue() > 0) {
                            toAddress.add(t.getAddress());
                        }
                    }
                    for (Transaction t : txs) {
                        if (t.getValue() != 0) {
                            if (t.getValue() < 0) {
                                graph.addNode(t.getAddress(), Graph.GraphPoint.ORIGINAL, line);

                                toAddress.stream().forEach(addr -> {
                                    graph.addEdge(addr, t.getAddress());
                                });

                            } else if (t.getValue() > 0) {
                                graph.addNode(t.getAddress(), Graph.GraphPoint.THEFT, line);
                                if (!toAddress.contains(t.getAddress())) {
                                    // graph.addEdge(toAddress + "-" + t.getAddress(), t.getAddress(), toAddress);
                                }
                                transactionsAffected.add(t.getHash());
                                startingAddresses.add(t.getAddress());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Loads all starting transactions based on 
     * @throws ArgumentException
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void loadBundlesFromTransactions() throws ArgumentException, FileNotFoundException, IOException {
        bundleStore.load();

        System.out.println("Processing received tx hashes");
        try (FileWriter fr = new FileWriter(new File(storeDoc), true); BufferedWriter br = new BufferedWriter(fr)) {

            int i = 0;
            for (String transaction : transactionsAffected) {
                if (++i % 100 == 0) {
                    System.out.println("Processing... " + i);
                }
                List<Transaction> txs = api.findTransactionsObjectsByHashes(transaction);
                String address = txs.get(0).getAddress();
                if (!bundleStore.contains(address)) {
                    findAndStoreBundlesFrom(address);
                }
            }
            br.close();
            fr.close();
        } catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
    private long getTheft(Bundle bundle, String address) {
        for (Transaction t : bundle.getTransactions()) {
            if (t.getValue() > 0 && t.getAddress().equals(address)) {
                return t.getValue();
            }
        }
        return 0;
    }

    private Collection<? extends String> getNextAddresses(Bundle bundle, String currentAddress) {
        List<String> nextAddresses = new LinkedList<String>();

        for (Transaction t : bundle.getTransactions()) {
            if (t.getValue() > 0 && !t.getAddress().equals(currentAddress)) {
                nextAddresses.add(t.getAddress());
            }
        }
        return nextAddresses;
    }

    private Collection<? extends String> getPreviousAddresses(Bundle bundle) {
        List<String> previousAddresses = new LinkedList<String>();

        for (Transaction t : bundle.getTransactions()) {
            if (t.getValue() != 0) {
                previousAddresses.add(t.getAddress());
            }
        }
        return previousAddresses;
    }

    private boolean spentBundle(Bundle bundle, String currentAddress) {
        for (Transaction t : bundle.getTransactions()) {
            if (t.getValue() < 0 && t.getAddress().equals(currentAddress)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        try {
            IotaAPI api = new IotaAPI.Builder()
                    .addNode(new HttpConnector(new URL(node)))
                    .build();

            new App(api);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    
}
