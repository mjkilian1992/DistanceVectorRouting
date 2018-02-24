# Distance Vector Routing Simulator - Michael Kilian

## Overview
The aim of this project is to provide a command-line simulator mimicking [Distance Vector Routing](https://en.wikipedia.org/wiki/Distance-vector_routing_protocol).
It was written by me in 2014 as a solution to a piece of university coursework. The documentation
below is taken from my original report. 

## System Overview
The core of the system is made up of 3 classes:
1. `NetworkNode`: a class representing a node in the network. Each node
holds a routing table and a set of links (with cost) which associate it with
its adjacent nodes in the network.
2. `Network`: holds a collection of networks and defines necessary methods
to manipulate the network, including causing exchanges. It implements
the distance vector routing. All additions/changes to the network must be
done through an instance of the Network class: an object using a Network
instance does not require knowledge of the underlying NetworkNodes. A
Network takes a file as input from which it builds itself.
3. `DistanceRoutingSimulator`: the main class which implements the command line interface. This is effectively a wrapper over a Network instance.

The above are contained in the `routing` package. In addition, the `events` package
defines two events which may be queued to happen after a given number of
iterations. These are:
1. `FailureEvent`: defines a failure of a link in the network.
2. `CostChangeEvent`: defines a change in link cost.

Both of these classes inherit from an abstract `NetworkEvent` class. An event
is defined to occur between two nodes, which must be specified in the events
constructor.

Nodes may be named by any String, without space. E.g. _‘n1’_, _‘node1’_ and
_‘198.0.0.1’_ would all be valid node names. The format of an input network file
must conform to the following:
1. The first line contains the names of the nodes in the network, each separated by a space.
2. Subsequent lines define the links in the network as a space-separated triple
(n 1 ,n 2 ,cost).
3. There is an empty line after the list of links, after which any text is allowed
as comments. These are not read in by the program, and exist only to aid
in making the file human-readable by providing a brief description of the
network.

In order to control the count-to-infinity problem, the max cost of a route is
defined to be 256. Note that in each routing table the outgoing link is defined by
the ‘next hop’ that must be taken from the current node. There are no concrete
link objects in the system.

Instead of true broadcasting, a node updates by pulling the routing tables
from other nodes. During an exchange each node updates a copy of its routing
table. When a node _n1_ pulls a routing table from another node _n2_, it pulls a copy
of the table at the start of the iteration. In this manner, _n1_ will not accidentally
receive any of the changes made to _n2_ in the current iteration before it should.
At the conclusion of an iteration, the table for each node is replaced by the
copy on which updates were made. When discussing the example networks in
the next section we will discuss exchanges in terms of each node pulling data
from its neighbours.

The system does not guarantee that nodes are updated in any particular
order. The examples in this report assume an order of updating which may not
occur on a particular execution of the simulator.

## Run Instructions
The program can be run from the main class `routing.DistanceRoutingSimulator`. 
It takes a filename as an argument, which should point to a network file as 
described previously. Use `--help` once running to see the full list of options.

## Worked Examples

### Network 1 - Normal Convergence

![Network 1](https://github.com/mjkilian1992/DistanceVectorRouting/blob/master/diagrams/Network1.png)

The diagram above shows the first example network. This network can reach stability after 3 exchanges (2 exchanges which result in changes plus 1 to establish
stability has been reached). The initial routing tables contain only the links
between nodes.

The tables below show the routing tables for the nodes after the first iteration. Many
of the optimal routes have been found by this point, but the nodes at the edge
of the network still do not have complete tables.


**Node1 (n1)**

Destination | Distance (Total Cost) | Next Hop
------------|-----------------------|---------
      n5    |           26          |    n4
      n4    |           10          |    n4
      n3    |           13          |    n4             
      n2    |           5           |    n2
      n6    |           256         |    Unknown
      
**Node 2 (n2)**

Destination | Distance (Total Cost) | Next Hop
------------|-----------------------|---------
      n1    |           5           |    n1
      n5    |           28          |    n3
      n3    |           12          |    n3             
      n4    |           9           |    n3
      n6    |           24          |    n3
      
**Node 3 (n3)**

Destination | Distance (Total Cost) | Next Hop
------------|-----------------------|---------
      n1    |           13          |    n4
      n5    |           19          |    n4
      n4    |           3           |    n4             
      n2    |           9           |    n2
      n6    |           15          |    n6
      
**Node 4 (n4)**

Destination | Distance (Total Cost) | Next Hop
------------|-----------------------|---------
      n1    |           10          |    n1
      n5    |           16          |    n5
      n3    |           3           |    n3             
      n2    |           12          |    n3
      n6    |           18          |    n3
      
**Node 5 (n5)**

Destination | Distance (Total Cost) | Next Hop
------------|-----------------------|---------
      n1    |           26          |    n3
      n4    |           16          |    n5
      n3    |           19          |    n3             
      n2    |           256         |    Unknown
      n6    |           6           |    n3
      
**Node 6 (n6)**

Destination | Distance (Total Cost) | Next Hop
------------|-----------------------|---------
      n1    |           28          |    n3
      n5    |           6           |    n5
      n4    |           18          |    n3             
      n3    |           15          |    n3
      n2    |           24          |    n3
      
The tables below show the routing tables after the second iteration (only the routing
tables which have changed have been shown). At this point stability has been
achieved. A third iteration will result in no changes to routing tables but will
confirm stability.

**Node 1 (n1)**

Destination | Distance (Total Cost) | Next Hop
------------|-----------------------|---------
      n5    |           26          |    n4
      n4    |           10          |    n4
      n3    |           13          |    n4             
      n2    |           5           |    n2
      n6    |           28          |    n4
      
**Node 5 (n5)**

Destination | Distance (Total Cost) | Next Hop
------------|-----------------------|---------
      n1    |           26          |    n3
      n4    |           16          |    n5
      n3    |           19          |    n3             
      n2    |           28          |    n4
      n6    |           6           |    n3
      
      
### Network 2 

![Network 2](https://github.com/mjkilian1992/DistanceVectorRouting/blob/master/diagrams/Network2.png)

The above diagram shows the second example network. Below we describe how routing
occurs with Slow Convergence, then how this is solved by introducing Split Horizon.

#### With Slow Convergence
Suppose the link from _n2_ to _n5_ has failed. We assume at this stage that _n2_
has immediately recognised its link to _n5_ is broken. The following sequence of 
events occur:
1. _n4_ pulls the routing tables from its neighbours _n1_ and _n2_. The table
from _n2_ shows that _n5_ is now unreachable through _n2_. However, _n1_ is
advertising that _n5_ can be reached at cost 9 (_n1_-_n2_-_n5_). _n4_ concludes
incorrectly that it can reach _n5_ through _n1_ at cost 13.
2. _n3_ now pulls from its neighbours. In a similar manner, it recognises that
_n5_ can no longer be reached through _n2_, but accepts the route advertised
by _n4_. _n3_ now believes it can reach _n5_ through _n4_.
3. Next _n1_ updates. It sees from _n2_’s routing table that _n5_ can no longer be
reached through _n2_. However, _n4_ is advertising a route through _n1_, which
without split horizon, _n1_ will accept. _n1_ now falsely believes it can reach
_n5_ through _n4_ at cost 15 (using the route _n1_-_n4_-_n1_-_n2_-_n5_, which clearly
is not feasible.
4. Finally, _n2_ updates. Although it knows that it can no longer reach _n5_
through its own link, its neighbours _n1_ and _n3_ are still broadcasting a
route to _n5_. _n2_ will accept the route advertised by _n3_ (at a cost of 19, as
opposed to the route advertised by _n1_ at a cost of 23).

A loop has now been created between _n1_ and _n4_. If the nodes continue to
update in this pattern, the nodes in the route will continue to mistakenly believe
that _n5_ can be reached. On each iteration the cost held by _n1_ will be increased,
as it constantly must add a loop from itself to _n4_ and back to avoid the broken
link from itself to _n2_ (step 3 is repeated). The network will only recover from
this when the route to _n5_ advertised by _n1_ increases in cost to infinity (256 in
this implementation). Once this occurs, _n4_ will recognise that _n5_ cannot be
reached through _n1_ (the cost of the route advertised by _n1_ to _n5_ is infinity) and
likewise advertise that _n5_ cannot be reached. This will eventually propagate to
_n3_ and _n2_ over the subsequent iteration.

#### With Split Horizon
In the above example, the slow convergence occurred because _n1_ was allowed to
receive a route from _n4_ to _n5_ which involved the link from _n1_ to _n4_. In other
words, _n1_ was fed a route which it had advertised itself, which was considered a
better route than the infinite distance to _n5_ advertised by _n2_. With split horizon
active, step 3 in the above example would not happen. Instead, _n1_ would not
receive the route to _n5_ from _n4_ and continue to believe that _n5_ is unreachable.
On the next iteration, _n4_ would pull this information from _n1_ and conclude
there is no route to _n5_. _n3_ and _n2_ will then pick up on this respectively over
the next two iterations.