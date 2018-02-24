package routing;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class NetworkNode {
    /**
     * the name of this node
     */
    private String name;

    /**
     * The routing table for this node. Key is the destination address. Value
     * is a (cost,forwarding address) pair
     */
    private Map<String, TableEntry> table;

    /**
     * Temporary Table used for updating purposes
     */
    private Map<String, TableEntry> tempTable;

    /**
     * The links in which this node participates. Key is the name of an adjacent
     * node. Value is the a (cost, reference to adj node0 pair
     */
    private Map<String, TableEntry> links;

    /**
     * Construct a NetworkNode
     */
    public NetworkNode(String name) {
        this.name = name;
        this.table = new HashMap<String, TableEntry>();
        this.links = new HashMap<String, TableEntry>();
    }

    /**
     * Represents an entry in a routing table
     * <p>
     * If a link, the cost is the node cost, the target node hold a reference to
     * the adjacent node.
     * <p>
     * If a routing table entry, the target node is the forwarding
     * address, the cost is the cost to the destination.
     * This class does not hold the destination address; it is expected that
     * a mapping be made from destination addresses to instances of TableEntry
     * such that the key is the destination address.
     */
    private class TableEntry {
        int cost;
        NetworkNode target;

        TableEntry(int cost, NetworkNode target) {
            this.cost = cost;
            this.target = target;
        }
    }

    /**
     * Initialise this nodes routing table, with only the routes to its
     * neighbours known.
     *
     * @param nodeset the nodes in the network
     */
    public void initialiseTable(Collection<NetworkNode> nodeset) {
        for (NetworkNode node : nodeset) {
            if (node == this) {
                continue; //dont add node to its own table
            }
            //check to see if a link exists already
            if (this.table.get(node.getName()) == null) {
                //infinite cost and unkown next hop (node doesnt know path to target)
                table.put(node.getName(), new TableEntry(Defaults.INFINITY, null));
            }
        }
    }

    /**
     * Adds a new link from this node to the target node
     * Note this will overwrite any existing link between the two,
     * regardless of the cost of the existing link.
     *
     * @param target
     * @param cost
     */
    public void addLink(NetworkNode target, int cost) {
        //add the link
        links.put(target.getName(), new TableEntry(cost, target));

        //here we are saying that the route to the target node is
        //by forwarding directly to the target node (a direct link)
        //with the cost equal to the link cost
        table.put(target.getName(), new TableEntry(cost, target));
    }

    /**
     * Destroys the link from this node to the target node
     * Returns true if a link existed
     * returns false if there was no link to destroy
     *
     * @param nodeName
     * @return
     */
    public boolean destroyLink(String nodeName) {
        if (links.get(nodeName) != null) {
            links.remove(nodeName); //remove the link
            //reset the routing table entry (as if the node has spotted the link failure)
            //we have to update all entries which use the
            //destroyed link as its outgoing link
            for (TableEntry entry : table.values()) {
                //the target may be null if the table is still in its initial state
                if (entry.target != null && entry.target.getName().equals(nodeName)) {
                    entry.cost = Defaults.INFINITY;
                    entry.target = null;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Change the cost of the link from this node to the destination node
     * returns true if the link existed and was changed
     * returns false if there is no link from this node to the destination
     *
     * @param nodeName
     * @param cost
     * @return
     */
    public boolean changeLinkCost(String nodeName, int cost) {
        if (links.get(nodeName) != null) {
            links.get(nodeName).cost = cost;
            table.get(nodeName).cost = cost; //can change this since we know the entry is a direct link
            return true;
        } else {
            return false;
        }
    }


    /**
     * Look up the cost of the currently known route from this node to the target node.
     *
     * @return the cost of the route
     */
    public int lookupCost(String nodeName) {
        return this.table.get(nodeName).cost;
    }

    /**
     * Find the next node in the route from this node to the destination.
     *
     * @param destination
     * @return
     */
    public NetworkNode nextHop(String destination) {
        return table.get(destination).target;
    }

    /**
     * Tells this node to fetch the routing tables from its adjacent nodes in a periodic update.
     * Update the routing table according to the procedure for distance vector routing.
     * Returns true if an update was made, false if not change was made
     * Does not correct for link failures/cost change
     * When these occur, a triggered update is used.
     */
    public boolean updateRoutingTable(boolean splitHorizon) {
        //create a temporary copy of the routing table
        deepCopyTables();

        boolean change = false;
        //iterate over all links
        for (TableEntry adjEntry : links.values()) {
            NetworkNode adjacent = adjEntry.target;
            int costToAdjacent = adjEntry.cost;
            if (adjacent == null) continue; //link from this node to adjacent is broken

            //for each node in the network
            for (String destination : tempTable.keySet()) {
                //destination is the name of the destination
                //node whose routing table entry
                //we are attempting to update

                //catch case where table entry refers to node we are receiving
                //data from (it wont have an entry for itself)
                if (destination == adjacent.getName()) continue;

                //split horizon
                //ignore any entries in the adjacent table where the next hop
                //is this node
                if (splitHorizon) {
                    if (adjacent.nextHop(destination) == this) continue;
                }

                //look up the current known cost to the target
                int currentCost = tempTable.get(destination).cost;

                //the potential new cost is the cost from the adjacent node to
                //the target plus the cost from this node to the adjacent node
                int newCost = adjacent.lookupCost(destination) + costToAdjacent;


                //if the new cost is less than the old one, we update the table so that
                //the routing entry for our target node directs to the adjacent node
                TableEntry routeEntry = tempTable.get(destination);
                if (newCost < currentCost) {
                    routeEntry.cost = newCost;
                    routeEntry.target = adjacent;
                    change = true;
                    //if the current route is through the adjacent node and that node
                    //is reporting a negative change in the link state (higher cost or infinity)
                } else if (routeEntry.target == adjacent) {
                    if (adjacent.lookupCost(destination) == Defaults.INFINITY) {
                        routeEntry.cost = Defaults.INFINITY;
                        routeEntry.target = null;
                        change = true;
                    } else if (newCost > currentCost) {
                        routeEntry.cost = newCost;
                        change = true;
                    }


                }
            }
        }
        return change;

    }


    /**
     * Returns a printable represenation of this nodes routing table.
     *
     * @return
     */
    public String routingTable() {
        String output = "";
        output += this.name + " routing table:\n";
        output += "Destination\tCost\tOutgoing Link\n";
        for (String destination : this.table.keySet()) {
            int cost = table.get(destination).cost;

            String out;
            if (table.get(destination).target != null)
                out = table.get(destination).target.getName();
            else //catch case where next hop is not yet known
                out = "Unknown";

            output += (destination + "\t" +
                    cost + "\t" + out + "\n");
        }
        return output;
    }

    /**
     * Returns a printable representation of this nodes links
     */
    public String links() {
        String output = "";
        output += this.name + " links:\n";
        output += "Adjacent Node\tCost\n";
        for (String destination : this.links.keySet()) {
            int cost = links.get(destination).cost;
            ;
            output += (destination + "\t" +
                    cost + "\n");
        }
        return output;
    }

    /**
     * Returns the name of this node.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Get this nodes routing table.
     */
    public Map<String, TableEntry> getTable() {
        return table;
    }

    /**
     * Get this nodes links
     */
    public Map<String, TableEntry> getLinks() {
        return links;
    }

    /**
     * Take a deep copy of the routing table for use in an update
     */
    private void deepCopyTables() {
        tempTable = new HashMap<String, TableEntry>();
        for (Entry<String, TableEntry> kv : table.entrySet()) {
            TableEntry orig = kv.getValue();
            TableEntry lCopy = new TableEntry(orig.cost, orig.target);

            tempTable.put(kv.getKey(), lCopy);
        }
    }

    /**
     * Finalises an update by replacing the routing table with the copy
     * which was updated.
     */
    public void finaliseTable() {
        table = tempTable;
    }


}
