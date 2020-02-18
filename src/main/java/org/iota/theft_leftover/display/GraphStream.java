package org.iota.theft_leftover.display;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.View;
import org.graphstream.ui.view.Viewer;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class GraphStream implements org.iota.theft_leftover.display.Graph {
    
    private static final String STYLE_DOC = "/home/brord/eclipse-workspace/theft-leftover/stylesheet.css";
    
    Graph graph;
    Viewer viewer;
    
    public GraphStream() {
        System.setProperty("org.graphstream.ui", "swing");
        graph = new SingleGraph("Hack");
        graph.setAutoCreate(true);
        graph.setStrict(false);
        graph.setAttribute("ui.stylesheet",
                "url('file://" + STYLE_DOC + "')");
    }
    
    @Override
    public void display() {
        viewer = graph.display();
        View view = viewer.getDefaultView();

        // Zoom, non functional, no idea how to attach JFrame to Viewer
        JFrame frame = new JFrame();
        frame.setLayout(new GridLayout());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBounds(0, 0, 100, 100);
        frame.setPreferredSize(new Dimension(700, 500));

        // Components
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout());
        frame.add(panel);

        panel.addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent mwe) {
                if (mwe.getButton() == MouseEvent.MOUSE_WHEEL) {
                    if (mwe.isControlDown()) {
                        view.getCamera().resetView();
                    } else {
                        view.getCamera().setViewPercent(view.getCamera().getViewPercent() + 0.05);
                    }
                }
            }
        });
    }

    @Override
    public void addNode(String address, GraphPoint type, String bundlehash) {
        graph.addNode(address);
        if (type != null) {
            setType(address, type, bundlehash);
        }
    }
    
    

    @Override
    public void addEdge(String edgeOne, String edge2) {
        graph.addEdge(edgeOne + "-" + edge2, edgeOne, edge2);
    }

    @Override
    public boolean hasNode(String address) {
        return graph.getNode(address) != null;
    }

    @Override
    public void setType(String address, GraphPoint type, String bundlehash) {
        Node n = graph.getNode(address);
        String toAdd = "";
        if (n.hasAttribute("ui.class")) {
            if (type.equals(GraphPoint.FUNDS) && n.getAttribute("ui.class").equals("big")) {
                System.out.println("Found dupe: " + address);
            }
            if (n.getAttribute("ui.class").equals("big")){
                toAdd = ", " + n.getAttribute("ui.class");
            }
            n.removeAttribute("ui.class");
        }
        if (n.hasAttribute("ui.label")) {
            n.removeAttribute("ui.label");
        }
        switch (type) {
            case ORIGINAL:
                n.setAttribute("ui.class", "original_owner" + toAdd);
                n.setAttribute("ui.label", address.subSequence(0, 5));
                break;
            case THEFT:
                n.setAttribute("ui.class", "theft" + toAdd);
                n.setAttribute("ui.label", address.subSequence(0, 5) + " | " + bundlehash.substring(0, 5));
                break;
            case FUNDS:
                n.setAttribute("ui.class", "remainder" + toAdd);
                n.setAttribute("ui.label", address.subSequence(0, 5) + " | " + bundlehash.substring(0, 5));
                break;
            case INTERMEDIATE:
                n.setAttribute("ui.class", "regular" + toAdd);
                n.setAttribute("ui.label", address.subSequence(0, 5));
                break;
            case UNKNOWN:
                n.setAttribute("ui.class", "unknown_investigate" + toAdd);
                n.setAttribute("ui.label", address.subSequence(0, 5));
                break;
            case UNKNOWN_STUCK:
                n.setAttribute("ui.class", "unknown_stuck" + toAdd);
                n.setAttribute("ui.label", address.subSequence(0, 5));
                break;
            case BIG:
                n.setAttribute("ui.class", "big" + toAdd);
                n.setAttribute("ui.label", address.subSequence(0, 5) + " | " + bundlehash.substring(0, 5));
                break;
            default:
                break;
        }
    }

    @Override
    public void close() {
        if (viewer != null) {
            viewer.close();
            viewer = null;
        }
    }
}
