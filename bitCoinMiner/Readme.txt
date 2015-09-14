Bitcoin Mining

- Saravanan Setty ()

1. How to Run:

• The Server folder contains the Server that co-ordinates work among the Clients. It should be run first. It can be run with sbt for scala by navigating to the folder Server and executing the command:

sbt "run <difficulty>"

Example: sbt "run 5"

• The Client folder contains each node. It can be run with sbt by navigating to the Client folder and executing the command:

sbt "run <server IP>"

Example: sbt "run 192.168.0.4"

2. Architecture:

The architecture is divided into 3 basic parts, the Server, the Clients and the Miners.

• The Server co-ordinates Clients as they become available. It also handles its own pool of locally managed Miners.

• The Client contacts the Server 