Bitcoin Mining

- Saravanan Setty (33299221)
- Ankur Bagchi (05549066)

----------------------------------

1. How to Run:

• The Server folder contains the Server that co-ordinates work among the Clients. It should be run first. It can be run with sbt for scala by navigating to the folder Server and executing the command:

sbt "run <difficulty>"

Example: sbt "run 5"

• The Client folder contains each node. It can be run with sbt by navigating to the Client folder and executing the command:

sbt "run <server IP>"

Example: sbt "run 192.168.0.4"

----------------------------------

2. Architecture:

The architecture is divided into 3 basic parts, the Server, the Clients and the Miners.

• The Server co-ordinates Clients as they become available. It also handles its own pool of locally managed Miners.

• The Client contacts the Server and waits for work assignments from the server. The Client locally handles its own Miners.

• The Miners work in parallel to generate random strings and find bitcoins. The number of created Miners on a system equals the number of processors.

----------------------------------

3. Work Unit

The work unit is dynamically adjusted by the Server for every node at runtime to account for overheads. The Server calculates the work done (strings processed) by a Client (or node) per unit time as workRatio, then increases or decreases work unit given to a node if there is a change of over 25% in the work ratio.

• If the ratio decreases to less than 80% of the previous ratio, the new work unit size is decreased to 80% of the previous one.
• If the ratio increases to more than 125% of the previous ratio, the new work unit size is increased to 125% of the previous one.

The outputs from the Client terminals reveal the periodic update of work unit size.

----------------------------------

4. Outputs

Example of Server output: http://showterm.io/1b872855bbadfdb2c57f5
Example of Client output: http://showterm.io/3e5e122c77787cf29002b

Running for bitcoin difficulty 4: http://showterm.io/3d3085f8fad3ee8479664

User Time vs Real Time output for difficulty 5: http://showterm.io/49db3eef70f4e6c8b0276
As seen, the ratio of user time to real time is over 3.5.

----------------------------------

5. Best Result

Bitcoin: ankurbagchi;JgO_sQB?[Pd!|>
Difficulty: 8
