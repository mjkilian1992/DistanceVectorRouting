package events;

/**
 * Defines a change in cost of a link between two nodes
 */
public class CostChangeEvent extends NetworkEvent {
    /**
     * The new cost of the link
     */
    private int newCost;

    /**
     * Constructs the cost change event. Note the order of the nodes
     * is not important.
     *
     * @param iteration
     * @param node1
     * @param node2
     * @param newCost
     */
    public CostChangeEvent(int iteration, String node1, String node2, int newCost) {
        super(iteration, node1, node2);
        this.newCost = newCost;
    }

    public int getNewCost() {
        return newCost;
    }


}
