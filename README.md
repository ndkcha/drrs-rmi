# The Distributed Room Reservation System using Java RMI

It's an assignment in Distributed System Design course at Concordia University. It is a distributed room reservation system (DRRS) for a university with multiple campuses: a distributed system used by administrators who manage the availability information about the university’s rooms and students who can reserve the rooms across the university’s different campuses.

## Development

I've used [IntelliJ IDEA](https://www.jetbrains.com/idea/) IDE (for macOS 10.13.2) for the developing this assignment. The configuration files for the IDE is already pushed to the repository. `auth` contains the code base for the central repository that stores the details of each active server node in the network. The code in `admin` and `student` packages handles the operations performed by clients. `server` packeage contains the code base for the server (contains the RMI code). `schema` contains the data signatures used in each packages in order to maintain the known data signatures while communication.

## Run the application

One file in each package (except from `schema`) contains the psvm block that can run as a single process. The number of server processes represents the number of campuses available to client.
