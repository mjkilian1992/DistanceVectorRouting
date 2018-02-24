package events;

/**
 * Defines the failure of a link between two nodes
 */
public class FailureEvent extends NetworkEvent {

    /**
     * Constructs the FailureEvent. Note the ordering of the nodes is
     * not important.
     *
     * @param iteration
     * @param node1
     * @param node2
     */
    public FailureEvent(int iteration, String node1, String node2) {
        super(iteration, node1, node2);
    }
}
