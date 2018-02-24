package routing;

import java.util.Scanner;

public class DistanceRoutingSimulator {
    public static String usage() {
        return "Usage: java DistanceVectorRouting <filename> [options]\n"
                + "Options:\n" +
                "-s: engage split horizon on the network";
    }

    public static void printHelp() {
        String helpMessage = "Available Commands:\n" +
                "vt <name> : view the routing table for node <name>\n" +
                "vta: view all routing tables\n" +
                "e: cause an exchange (an iteration of the distance-vector protocol)\n" +
                "r: run the protocol until changes temporarily stop (note the network may"
                + " not yet be in a consistent state.\n" +
                "cl -n n1 n2 newCost: change the link cost from n1-n2 to newCost right now\n" +
                "cl n1 n2 newCost iteration: schedule the link n1-n2 to change cost after "
                + "iteration\n" +
                "dl -n n1 n2: destroy the link between n1 and n2 now.\n" +
                "dl n1 n2 iteration: schedule the link n1-n2 to fail after iteration\n" +
                "tr n1 n2: show the current best route from n1 to n2, if one exists.\n" +
                "h or help: bring up this help message\n" +
                "q or quit: exit the simulator\n";

        System.out.println(helpMessage);
    }

    public static void main(String[] args) {
        //get filename
        if (args.length < 1) {
            System.out.println("Please include a file name.");
            System.out.println(usage());
            System.exit(1);
        }

        //check for split horizon
        boolean splitHorizon = false;
        if (args.length > 1 && (args[1].equals("s") || args[1].equals("-s")))
            splitHorizon = true;

        //create the network
        Network network = new Network(args[0], splitHorizon);

        //print out the possible options
        System.out.println("Running Distance Vector Simulator");
        printHelp();

        //read in commands
        Scanner scan = new Scanner(System.in); //scanner for input
        try {
            while (scan.hasNextLine()) { //loop while there is user input
                String[] cmmd = scan.nextLine().split(" ");

                if (cmmd[0].equals("vt")) {
                    //view routing table for a given node
                    if (cmmd.length < 2) {
                        System.out.println("Usage view table: vt <nodename>");
                    } else {
                        network.printRoutingTable(cmmd[1]);
                    }
                } else if (cmmd[0].equals("vta")) {
                    //view all routing tables
                    network.printRoutingTables();
                } else if (cmmd[0].equals("e")) {
                    //cause an exchange
                    boolean change = network.exchange();
                    System.out.println("Network Updated:\n"
                            + "Completed " + network.getCurrentIteration() +
                            " iterations.");
                    if (change) {
                        System.out.println("Change?: Yes");
                    } else {
                        System.out.println("Change?: No");
                    }
                } else if (cmmd[0].equals("r")) {
                    System.out.println("Running");
                    while (network.exchange()) ;
                    System.out.println("Changes Stopped. Total Iterations: "
                            + network.getCurrentIteration());
                } else if (cmmd[0].equals("dl")) {
                    //destroy a link
                    if (cmmd.length < 4) {
                        System.out.println("Usage to destroy link: dl [options] n1 n2 iteration\n " +
                                "Options:\n" +
                                "-n: destroy the link now. If this is chosen the iteration can be ignored\n" +
                                "Iteration is the iteration after which the link should fail.");
                    } else {
                        if (cmmd[1].equals("-n")) {
                            network.destroyLink(cmmd[2], cmmd[3]);
                        } else {
                            network.addLinkDestroyEvent(cmmd[1], cmmd[2], Integer.parseInt(cmmd[3]));

                        }
                    }
                } else if (cmmd[0].equals("cl")) {
                    //change link cost
                    if (cmmd.length < 5) {
                        System.out.println("Usage to change link cost: cl [options] n1 n2 newCost iteration \n " +
                                "Options:\n" +
                                "-n: change the link cost now. If this is chosen the iteration can be ignored\n" +
                                "Iteration is the iteration after which the link should change cost.");
                    } else {
                        if (cmmd[1].equals("-n")) {
                            network.changeLinkCost(cmmd[2], cmmd[3], Integer.parseInt(cmmd[4]));
                        } else {
                            network.addCostChangeEvent(cmmd[1], cmmd[2],
                                    Integer.parseInt(cmmd[4]), Integer.parseInt(cmmd[3]));
                        }
                    }
                } else if (cmmd[0].equals("tr")) {
                    if (cmmd.length < 3) {
                        System.out.println("Usage of traceroute: tr n1 n2");
                    } else {
                        System.out.println(network.traceRoute(cmmd[1], cmmd[2]));
                    }
                } else if (cmmd[0].equals("h") || cmmd[0].equals("help")) {
                    printHelp();
                } else if (cmmd[0].equals("q") || cmmd[0].equals("quit")) {
                    System.out.println("System exiting.");
                    scan.close();
                    System.exit(0);
                } else {
                    System.out.println("No such command. Enter 'h' or 'help' for " +
                            "the list of valid commands");
                }
            }
        } catch (Exception e) { //catch-all for incorrect input
            System.err.println("Poorly formatted or illegal command. Please try again.");
        }

    }

}
