# READ THIS

Please note that this application is still in development, and will not work in its current build. To be more precise, it will compile...but it should *NOT BE USED*. I made this application as part of a project for a class, and I will have to perform extensive updates to make it even remotely useable in a production environment. For example, per the odd instructions of the assignment, I configured the application's users, and their passwords, to be read from a plaintext file as opposed to having any kind of registration capability. So yup, you couldn't even use the application if you wanted to at this point. In addition, I have not had a chance to even hide the password when written to stdin, much less encrypt any communication between the client or server, and I have a LOT of tests to write. 

tldr: *DO NOT* use this code. You have been warned.

## Code Description

The code is contained in a set of Server classes, and a set of Client classes, which handle the server side, 
and client side services of the chat program respectively. The server side classes include Server.java and 
ServerThread.java.

Server.java is the driver class of the server-side of the chat program. It is responsible 
for booting up the server-side of the program, establishing socket connections with clients, creating and 
starting new ServerThread instances, connecting those threads with each incoming client, and maintaining 
the program-wide data structures that allow separate ServerThread communication, maintaining a list of blocked 
clients, etc. When a Server object is instantiated, it can be instantiated with just a port number, or it can 
be instantiated with a port number and with various constant Long values that determine the length of time that 
a client is blocked, the length of time before a client times out from inactivity, etc. The values of these 
constants can also be given at the command line, and more will be discussed on them later.

ServerThread.java is the class of the program that is responsible for communicating with one specific client. 
In this communication, the ServerThread is responsible for processing a client's input, for sending messages 
to a client, for authenticating a client, and for connecting to and disconnecting from a client.

Client.java is the driver class of the client-side of the chat program. It is responsible for booting up 
the client-side of the program, and must be run AFTER the server is running. Furthermore, it must be run on 
the same port as a server-side of the program. A Client instance is also responsible for establishing a 
connection with, and disconnecting from, the server, and instantiating the ClientReaderThread, and 
ClientWriterThread, for reading from, and writing to, the server respectively.

ClientReaderThread.java is the class of the program that is responsible for reading input from the 
server. It is implemented as a thread to account for the fact that a client may receive input from a 
server at any time, and not only after sending a command to the server.

ClientWriterThread.java is the class of the program that is responsible for writing output/commands 
to the sever. For essentially the same reason as ClientReaderThread.java, ClientWriterThread.java is 
implemented as a thread to account for the fact that a client may receive input from a server at any 
time, and not only after sending a command to the server.


## Development Environment

I chose to develop my code in Java. As was explained in class, Java has a rich and powerful Socket 
programming library, and is ideal for a chat program (especially as a first-time network programmer!). 

My program was both unit-tested within Eclipse, and command-line (acceptance) tested with a bash shell 
on an Ubuntu 12.04 LTS operating system (though with the JVM, this is of course supplementary 
information).


## How to run the code

From here on, assume a `$' character represents the start of a terminal command. The code must be 
run with the following steps:

(1) Compile the program using make
    From inside the directory of the application, run the command `make' as such:

    $make

    When running make, Makefile will generate the .class files you need to run the application. 

(2) Run the server side of the chat program
    To run the server side of the chat program, you must first run the server side of the chat 
    program so that the client will have a Socket to connect to when he/she starts the program. 
    To run the server side of the chat program, run the following command:
    
    $java Server <port_no>

    This will start the program, with the Server listening on port <port_no> for a connection.
    This is the only command that you need to run on the server side, though in a typical 
    client/server architecture, Server should be `always on' so that it can service a client 
    at any time.

### Command line options
    When starting the Server program, there are several command line options available to customize 
    the program:
        * --LAST_HOUR  - Specifies the length of time, in milliseconds, that wholasthr should consider 
                         when displaying those users who have been logged in within LAST_HOUR.
        * --BLOCK_TIME - Specifies the length of time, in milliseconds, for which a user at a specific 
                         IP address should be blocked after failing to authenticate three times in a row.
        * --TIME_OUT   - Specifies the length of time, in milliseconds, after which a user should be 
                         logged out of the chat client due to inactivity.
        * -v           - Specifies that the program should run in verbosity mode. In verbosity mode, 
                         output specifying events such as when a server connects to a client, when a 
                         server runs a command, etc, is shown on the command line.
    
    These command-line arguments can be passed in any order, with --LAST_HOUR, --BLOCK_TIME, and 
    --TIME_OUT being specified as --COMMAND=time_in_milliseconds, and -v being passed simply as -v.

    Here are some examples of how the Server program can be run with command line arguments:

    $java Server 13431 --LAST_HOUR=10000 --BLOCK_TIME=5000 -v

    $java Server 5313 -v

    $java Server 4119 -v --TIME_OUT=1000 --BLOCK_TIME=159355 --LAST_HOUR=63000
                       

(3) Run the client side of the chat program
    After, and ONLY after, the Server program has been started, can the Client program 
    be started. To connect to the server, the client must both specify the IP Address of the server,
    and the port number of the Server process, when starting the program. This command is run as such:

    $java Client <server_ip> <port_no>

    Thehis will start the client-side of the program, and will connect the client to the server at
    <server_ip>, and to the process of this server that is listening at port <port_no>. Of course,
    this command can be repeated for every client who wishes to connect to the chat program.

    ** Command line options **
    When starting the Client program, you can optionally pass a `-v' command line option that 
    puts the program in verbosity mode. Similarly to the Server program, in verbosity mode, 
    the Client program prints extra output to reflect events such as when a client opens an 
    input stream and an output stream to the ServerThread.

    Here are some examples of how the Client program can be run with command line arguments:

    $java Client 192.168.0.3 13431 -v

    $java Client 74.125.226.212 5313 -v

For a guide to using the chat-program as a client (i.e. different commands that are available), 
please refer to the project spec. For a guide to the supplementary features of the application, 
see below.


## Supplementary features
As mentioned above, for a guide to using the normal commands of the chat-program, see the 
project spec. In this section, we will describe the functionality of all the supplementary 
features of the chat-program that are included in this release.

### Server program supplementary features
On the server side, the only supplementary features are the command line arguments mentioned
in the section above in the `How to run the code' section. Please refer to that documentation 
for information about how to use the Server program supplementary features.

### Client program supplementary features
On the client side, there are several supplementary features to those included in the normal 
project spec. Firstly, there is an option to run the Client in 'verbosity' mode. For information 
on how to run the client in `verbosity' mode, see the documentation in the `How to run the code'
section above.

In addition to running in verbosity mode, there are two more supplementary features that are 
available to the Client program. Both of these features are commands that can be sent to the 
Server, and both are described below:

    - help: The `help' command prints all of the commands that are available in the chat-program, 
            as well as their necessary arguments. It can be invoked as such:

    $>Command: help

    and prints an output such as this:

    whoelse: + Displays names of other connected users.
    wholasthr: Displays name of only those users that connected within the last 3600000 microseconds.
    broadcast <message>: Broadcasts <message> to all connected users.
    message <user> <message>: Private <message> to a <user>
    block <user>: Blocks the <user> from sending any messages. If <user> is self, displays error.
    unblock <user>: Unblocks the <user> who has been previously blocked. If <user> was not already blocked, display error.
    analysis: Prints a statistical distribution of all of the commands invoked thus far, by all clients, since the Server was first run.
    logout: Log out of the chat program.

    - analysis: The `analysis' command prints statistics about all of the commands that have been
                run thus far, by all clients, since the Server program was first run. On the back-end,
                this feature works by storing every command in a Red-Black tree (TreeMap in Java) that 
                maps each command to the number of times it has been invoked. Then, when analysis is 
                called, it performs basic arithmetic and formats output to display these statistics in 
                a useful way to the user. The command can be invoked as such:

    $>Command: analysis

    and prints an output such as this:

    Statistics for commands run in chat program:

    Unknown command: 4/10 --- 40.00%
    broadcast: 2/10 --- 20.00%
    help: 1/10 --- 10.00%
    message: 1/10 --- 10.00%
    whoelse: 1/10 --- 10.00%
    wholasthr: 1/10 --- 10.00%


## Last notes
Thank you for using my chat program. Please feel free to use it and distribute it to your liking.

Happy chatting!
