package routing;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Scanner;

import events.CostChangeEvent;
import events.FailureEvent;
import events.NetworkEvent;

public class Network {
    /**
     * The set of nodes in the network
     */
    private Map<String, NetworkNode> nodes;

    /**
     * True if split horizon is active, false otherwise
     */
    private boolean splitHorizon;

    /**
     * Queue of events (link cost changes/failures) to occur as
     * the protocol executes
     */
    private Queue<NetworkEvent> events;

    /**
     * The current iteration of the protocol
     */
    private int currentIteration;

    /**
     * Instantiates a Network from an input file
     */
    public Network(String filename, boolean splitHorizon) {
        //instantiate the node map
        nodes = new HashMap<String, NetworkNode>();
        //instantiate event queue and iteration number
        events = new PriorityQueue<NetworkEvent>();
        currentIteration = 0;

        //set split horizon settings
        this.splitHorizon = splitHorizon;

        //set up scanner
        File in = new File(filename);
        Scanner scan = null;
        try {
            scan = new Scanner(in);
        } catch (IOException e) {
            System.err.println("Input file '" + filename + "' not found");
            System.exit(1);
        }

        //first line of file is the list of networks
        if (scan.hasNextLine()) {
            String[] names = scan.nextLine().split(" ");
            for (String name : names) { //add each node
                nodes.put(name, new NetworkNode(name));
            }
        } else {
            System.err.println("Input file empty");
            System.exit(1);
        }

        //subsequent lines are links
        try {
            while (scan.hasNextLine()) {
                String line = scan.nextLine();
                if (line.isEmpty()) break; //catch end of readable file
                String[] link = line.split(" ");

                //suppose the link is of form (N1,N2,C)
                //we must add data to both N1 and N2 telling
                //them they can reach each other
                String node1 = link[0];
                String node2 = link[1];
                int cost = Integer.parseInt(link[2]);

                //add from node1 to node2
                NetworkNode n1 = nodes.get(node1);
                n1.addLink(nodes.get(node2), cost);
                //add from node2 to node1
                NetworkNode n2 = nodes.get(node2);
                n2.addLink(n1, cost);
            }
        } catch (Exception e) {
            //fairly generic exception handler. triggered by a problem reading the file
            System.err.println("Input file '" + filename + "' is not in an acceptable format");
            e.printStackTrace();
            System.exit(0);
        }

        //initialise routing tables
        for (NetworkNode node : nodes.values()) {
            node.initialiseTable(nodes.values());
        }
    }

    /**
     * Causes a vector exchange on the network.
     * Returns true if a change was made to any routing table
     * Returns false if the network is already stable (no changes)
     */
    public boolean exchange() {
        currentIteration++; //next iteration
        boolean change = false; //whether or not a change occurs

        //update each node, checking for a change in the routing tables.
        for (NetworkNode node : nodes.values()) {
            if (node.updateRoutingTable(splitHorizon)) change = true;
        }
        //turn temporary tables into current tables (synchronised update)
        for (NetworkNode n : nodes.values()) {
            n.finaliseTable();
        }
        //cause any events to happen that should happen
        boolean eventsLeft = true;
        while (eventsLeft) {
            NetworkEvent event = events.peek();
            if (event == null) {
                break; //no events to check
            }
            if (event.getIteration() < currentIteration) {
                //event is for an iteration which has already passed
                events.poll();
            } else if (event.getIteration() == currentIteration) {
                //event should occur now
                event = events.poll(); //remove event from queue
                if (event instanceof FailureEvent) {
                    System.out.println("Link (" + event.getNode1() + "," + event.getNode2() + ") failed.");
                    //if it is a failure event, fail the link
                    destroyLink(event.getNode1(), event.getNode2());
                } else if (event instanceof CostChangeEvent) {
                    //if it is a cost change, change the link cost
                    CostChangeEvent e = (CostChangeEvent) event;
                    changeLinkCost(e.getNode1(), e.getNode2(), e.getNewCost());
                }
            } else {
                //event should not happen yet, we are done for now
                eventsLeft = false;
            }
        }

        return change;
    }

    /**
     * Destroys the link between n1 and n2.
     * note the order in which the nodes are specified is irrlevant
     * returns true if there was a link to destroy
     * returns false otherwise
     *
     * @param n1
     * @param n2
     * @return
     */
    public boolean destroyLink(String n1, String n2) {
        //use result of first destroy to check if link actually exists
        if (nodes.get(n1).destroyLink(n2)) {
            nodes.get(n2).destroyLink(n1);
            return true;

        }
        return false;
    }

    /**
     * Changes the link cost between n1 and n2
     * Note the order in which the nodes are specified does not matter
     * returns true if there was a link between n1 and n2
     * returns false otherwise
     *
     * @param n1
     * @param n2
     * @param cost
     * @return
     */
    public boolean changeLinkCost(String n1, String n2, int cost) {
        //use the result of first change to check if link actually exists
        if (nodes.get(n1).changeLinkCost(n2, cost)) {
            nodes.get(n2).changeLinkCost(n1, cost);
            return true;
        }
        return false;
    }

    /**
     * Traces the current optimal route from the start node to the destination.
     *
     * @param start
     * @param destination
     * @return Printable String detailing the route.
     */
    public String traceRoute(String start, String destination) {
        String output = nodes.get(start).getName();
        boolean routeExists = true;
        NetworkNode cursor = nodes.get(start);
        while (!cursor.getName().equals(destination)) {
            cursor = cursor.nextHop(destination);
            if (cursor == null) {
                routeExists = false;
                break;
            }
            output += "-" + cursor.getName();
        }
        if (!routeExists) {
            return "No route from " + start + " to " + destination + " exists.";
        } else {
            int cost = nodes.get(start).lookupCost(destination);
            return "The best route from " + start + " to " + destination +
                    " is: " + output + " with total cost " + cost;
        }
    }

    /**
     * Adds a link destruction event to occur on a given iteration between two nodes.
     * Note the order in which the nodes are specified is not important.
     *
     * @param n1
     * @param n2
     * @param iteration
     */
    public void addLinkDestroyEvent(String n1, String n2, int iteration) {
        this.events.add(new FailureEvent(iteration, n1, n2));
    }

    /**
     * Adds a link cost change event to occur on a given iteration between two nodes.
     * Note the order in which the nodes are specified is not important.
     *
     * @param n1
     * @param n2
     * @param iteration
     * @param newCost
     */
    public void addCostChangeEvent(String n1, String n2, int iteration, int newCost) {
        this.events.add(new CostChangeEvent(iteration, n1, n2, newCost));
    }

    /**
     * Prints all routing tables to standard output
     */
    public void printRoutingTables() {
        for (NetworkNode node : nodes.values()) {
            System.out.println(node.routingTable());
        }
    }

    /**
     * Prints the routing table belonging to a given node to standard output
     */
    public void printRoutingTable(String name) {
        if (nodes.get(name) != null) {
            System.out.println(nodes.get(name).routingTable());
        } else {
            System.out.println("No node named " + name);
        }
    }

    /**
     * Prints all the links from the point of view of each node.
     * As such there will be duplicates.
     * Each link (n1,n2) is printed twice: once from n1 to n2
     * and once from n2 to n1.
     */
    public void printLinks() {
        for (NetworkNode node : nodes.values()) {
            System.out.println(node.links());
        }
    }

    /**
     * Returns true if the network has split horizon enabled, false otherwise
     */
    public boolean isSplitHorizon() {
        return splitHorizon;
    }

    /**
     * Gets the current iteration of the protocol
     */
    public int getCurrentIteration() {
        return currentIteration;
    }


}
