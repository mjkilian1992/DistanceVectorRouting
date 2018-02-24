package events;

/**
 * Abstract class for an event in the network. An event is some change in link state
 *
 * @author michael
 */
public abstract class NetworkEvent implements Comparable<NetworkEvent> {
    /**
     * First node in the event
     */
    private String node1;
    /**
     * Second node in the event
     */
    private String node2;
    /**
     * Iteration of the distance-vector protocol on which the event should occur.
     */
    private int iteration;

    /**
     * Constructor for an event. Takes in the two nodes on the link for which the
     * event should occur and the iteration AFTER which the event will occur.
     * Note the ordering of the nodes is irrelevant.
     *
     * @param iteration
     * @param node1
     * @param node2
     */
    public NetworkEvent(int iteration, String node1, String node2) {
        this.iteration = iteration;
        this.node1 = node1;
        this.node2 = node2;
    }

    @Override
    public int compareTo(NetworkEvent o) {
        if (this.iteration < o.iteration) {
            return -1;
        } else if (this.iteration > o.iteration) {
            return 1;
        }
        return 0;
    }

    public String getNode1() {
        return node1;
    }

    public String getNode2() {
        return node2;
    }

    public int getIteration() {
        return iteration;
    }


}
