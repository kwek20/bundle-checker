package org.iota.theft_leftover.display;


public interface Graph {
    enum GraphPoint {
        ORIGINAL,
        THEFT,
        FUNDS,
        INTERMEDIATE,
        UNKNOWN, 
        UNKNOWN_STUCK, 
        BIG,
    }
    
    void display();
    void close();
    
    void addNode(String address, GraphPoint type, String bundle);
    void addEdge(String edgeOne, String edge2);
    
    void setType(String address, GraphPoint type, String bundle);
    
    boolean hasNode(String address);
    
}
