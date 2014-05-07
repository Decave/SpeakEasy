package server;

import java.net.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.io.*;

public class ServerThread implements Runnable {
	private Socket clientSocket;
	private BufferedReader in;
	private PrintWriter out;
	private Server rootServer;
	private HashSet<String> blockList = new HashSet<String>();
	private String clientUsername;
	private Date lastActivity = new Date();
	private final Long TIME_OUT;
	private boolean verbose;
	private String clientAddress;

	public ServerThread (
			Socket clientSocket, 
			BufferedReader in, 
			PrintWriter out, 
			Server rootServer) {
		this.clientSocket = clientSocket;
		this.in = in;
		this.out = out;
		this.rootServer = rootServer;
		this.TIME_OUT = 1800000L;
		this.verbose = false;
	}

	public ServerThread (
			Socket clientSocket, 
			BufferedReader in, 
			PrintWriter out, 
			Server rootServer,
			Long TIME_OUT,
			boolean verbose) {
		this.clientSocket = clientSocket;
		this.in = in;
		this.out = out;
		this.rootServer = rootServer;
		this.TIME_OUT = TIME_OUT;
		this.verbose = verbose;
	}

	public ServerThread (
			Socket clientSocket, 
			BufferedReader in, 
			PrintWriter out, 
			Server rootServer,
			Long TIME_OUT) {
		this.clientSocket = clientSocket;
		this.in = in;
		this.out = out;
		this.rootServer = rootServer;
		this.TIME_OUT = TIME_OUT;
		this.verbose = false;
	}

	/**
	 * Returns PrintWriter. Used to test ServerThread construction.
	 * @return PrintWriter
	 */
	public PrintWriter getOut() {
		return this.out;
	}

	public void run() {
		try {
			if (verbose)
				System.out.println("ServerThread: Authenticating client");
			boolean authenticated = authenticate();
			if (!authenticated) {
				return;
			} else {
				if (verbose)
					System.out.println("Client successfully authenticated.");
				this.lastActivity = new Date();
				out.println(">Welcome to simple chat server!");
				HashSet<String> offlineMessages = 
						rootServer.getOfflineMessage(this.clientUsername);

				if (offlineMessages.size() > 0) {
					out.println(">You received the following message(s) " +
							"when you were offline:");
					for (String offlineMessage : offlineMessages) {
						out.println(offlineMessage);
					}
				}
				out.println(">Command:");
			}
		} catch (IOException e) {
			System.err.println("There was an I/O error when authenticating "
					+ "the client.");
			e.printStackTrace();
		}

		/*
		 * Obtain input from user, process that input.
		 */
		String input;
		try {
			// While user has not timed out, wait for input and process said 
			// input when it is received
								
				while ((input = in.readLine()) != null) {
					if((new Date().getTime() - lastActivity.getTime())
							>= TIME_OUT) {
						out.println("You have timed out due to inactivity. " +
								"Please log back in if you would like to " +
								"continue chatting.");
						break;
					}
					if (verbose)
						System.out.println("ServerThread: Got input from client," +
								" processing now.");
					if (input.equals("logout")) {
						out.println("Thank you for using the chat program!");
						break;
					}
					process(input);
					out.println(">Command: ");
				}

			// Disconnect client from rootServer's connected Map
			rootServer.disconnect(this.clientUsername);

		} catch (IOException e) {
			System.err.println("There was an I/O error while communicating "
					+ "with the client.");
			e.printStackTrace();
		}

		try {
			this.in.close();
			this.out.close();
			this.clientSocket.close();
		} catch (IOException e) {
			System.err.println("There was an error closing resources after "
					+ "the client disconnected.");
			e.printStackTrace();
		}
	}

	/**
	 * This method is the brain of the ServerThread class. Given the command 
	 * given by the client as input, perform certain functions and return an 
	 * output. The details of these functions are given in the doucmentation 
	 * for the project.
	 * 
	 * @param input Command given by user in run()
	 * @return Output string to be sent back to the user.
	 */
	public void process(String input) {
		if (verbose)
			System.out.println("Processing input from client: " + input);
		
		if (input == null) {
			out.println("You have given me a null command that I can't use. "
					+ "Please try again.");
			this.rootServer.addStatistic("null");
			return;
		}
		this.lastActivity = new Date();

		// Split command into multiple words by spaces:
		String[] inputArray = input.split(" ");

		// For commands of length 1:
		if(inputArray.length == 1){

			// If running whoelse:
			if (inputArray[0].equals("whoelse")) {
				if (verbose)
					System.out.println("ServerThread: Running whoelse");
				this.rootServer.addStatistic("whoelse");
				runWhoElse();

				// If running wholasthr:
			} else if (inputArray[0].equals("wholasthr")) {
				if (verbose)
					System.out.println("ServerThread: Running wholasthr");
				this.rootServer.addStatistic("wholasthr");
				runWhoLastHr();

			} else if (inputArray[0].equals("help")) {
				if(verbose)
					System.out.println("Running help");
				
				this.rootServer.addStatistic("help");
				out.println("List of available commands:");
				out.println("whoelse: + " +
						"Displays names of other connected users.");
				out.println("wholasthr: " +
						"Displays name of only those users that connected " +
						"within the last " + this.rootServer.getLastHour() + 
						" microseconds.");
				out.println("broadcast <message>: " +
						"Broadcasts <message> to all connected users.");
				out.println("message <user> <message>: " +
						"Private <message> to a <user>");
				out.println("block <user>: " +
						"Blocks the <user> from sending any messages. " +
						"If <user> is self, displays error.");
				out.println("unblock <user>: " +
						"Unblocks the <user> who has been previously blocked. " +
						"If <user> was not already blocked, display error.");
				out.println("analysis: " + 
						"Prints a statistical distribution of all of the " +
						"commands invoked thus far, by all clients, since " +
						"the Server was first run.");
				out.println("logout: Log out of the chat program.");
			
			} else if (inputArray[0].equals("analysis")) {							
				if (verbose)
					System.out.println("ServerThread: Running analysis");

				out.println(this.rootServer.getStatisticsString());
				
			} else {
				// Otherwise, incorrect command
				this.rootServer.addStatistic("Unknown command");
				out.println("You have given me a command I don't understand. "
						+ "Please try again.");
			}

			// For commands of length 2:
		} else if (inputArray.length == 2) {

			// If it's a broadcast:
			if (inputArray[0].equals("broadcast")) {
				if (verbose)
					System.out.println("ServerThread: Running broadcast " + inputArray[1]);
				this.rootServer.addStatistic("broadcast");
				runBroadcast(inputArray[1]);

				// If a user is being blocked: 
			} else if (inputArray[0].equals("block")) {
				if (verbose)
					System.out.println("ServerThread: Running unblock " + inputArray[1]);
				this.rootServer.addStatistic("block");
				blockClient(inputArray[1]);

				// If a user is being unblocked:
			} else if (inputArray[0].equals("unblock")) {
				this.rootServer.addStatistic("unblock");
				unblockClient(inputArray[1]);

				// Otherwise, incorrect command
			} else {
				this.rootServer.addStatistic("Unknown command");
				out.println("You have given me a command I don't understand. "
						+ "Please try again.");
			}

			// For commands of length three or more:
		} else {
			// If it's a single message being sent:
			if (!inputArray[0].equals("message") 
					&& !inputArray[0].equals("broadcast")) {
				this.rootServer.addStatistic("Unknown command");
				out.println("You have given me a command I don't understand. "
						+ "Please try again.");
			} else if (inputArray[0].equals("message")) {
				// If it's a message, build message
				String message = "";
				for (int i = 2; i < inputArray.length; i++) {
					message += inputArray[i] + " ";
				}
				message = message.substring(0, message.length() - 1);
				if (verbose)
					System.out.println("ServerThread: Running message " +
							inputArray[1] + " " + message + ".");

				// Send message
				this.rootServer.addStatistic("message");
				messageOtherUser(
						inputArray[1],
						message,
						this.clientUsername);

				// Otherwise, is multi-word broadcast command.
			} else {
				// If it's a broadcast, build message
				String message = "";
				for (int i = 1; i < inputArray.length; i++) {
					message += inputArray[i] + " ";
				}
				message = message.substring(0, message.length() - 1);
				if (verbose)
					System.out.println("ServerThread: Running broadcast " +
							message + ".");

				// Send broadcast message
				this.rootServer.addStatistic("broadcast");
				runBroadcast(message);
			}
		}
	}

	/**
	 * If username is a user in the chat program, and username is not equal 
	 * to current ServerThread's client, then send message to user.
	 * 
	 * @param username User to send message to
	 * 
	 * @param message Message to send to user.
	 * 
	 * @param sendingClient
	 */
	public void messageOtherUser(
			String username,
			String message,
			String sendingClient) {
		if (username.equals(this.clientUsername)) {
			out.println("You cannot send a message to yourself.\n "
					+ "Please provide another command.");
		} else if (!rootServer.getAuthList().containsKey(username)) {
			out.println(username + " is not a user of this chat client"
					+ " so they can't be messaged.");
		} else if (rootServer.getConnected().containsKey(username)) {
			rootServer
			.getConnected()
			.get(username)
			.sendMessageToClient(message, sendingClient);
		} else {
			rootServer.addOfflineMessage(
					username,
					this.clientUsername + ": " + message
					);
		}
	}

	/**
	 * Print message to client terminal
	 * 
	 * @param message Message to send to client
	 * 
	 * @param sendingClient Client who sent message
	 */
	public void sendMessageToClient(String message, String sendingClient) {
		if (this.blockList.contains(sendingClient)) {
			return;
		} else {
			out.println(sendingClient + ": " + message);
			out.println(">Command: ");
		}
	}

	/**
	 * Gets list of all connected users, and sends them the message given as 
	 * an argument to the method. 
	 * 
	 * @param message Broadcast message to send to other users
	 * @return
	 */
	public void runBroadcast(String message) {
		for (String username : rootServer.getConnected().keySet()) {
			if (!username.equals(this.clientUsername)) {
				rootServer
				.getConnected()
				.get(username)
				.sendBroadcastToClient(message);
			} else {
				// Only print message if sending to self. This prevents 
				// >Command: from being printed twice
				out.println(this.clientUsername + ": " + message + "__broadcast");
			}
		}
	}

	/**
	 * Receives and displays broadcasted message.
	 * 
	 * @param message
	 */
	public void sendBroadcastToClient(String message) {
		if (message == null) {
			return;
		} else {
			out.println(this.clientUsername + ": " + message);
			out.println(">Command: ");
		}
	}

	/**
	 * Refreshes connectedLastHr map in rootServer, then adds each user on the 
	 * list to a String (with newline between each user) and sends each user 
	 * to the client.
	 */
	public void runWhoLastHr() {
		this.rootServer.refreshConnectedLastHr();
		TreeMap<String, Date> connectedLastHr = 
				this.rootServer.getConnectedLastHr();
		HashSet<String> retList = new HashSet<String>();

		for (String user : connectedLastHr.keySet()) {
			if(!user.equals(this.clientUsername)) {
				out.println(user);
			}
		}
	}

	/**
	 * Prints list of other clients currently connected in the application. 
	 */
	public void runWhoElse() {
		for (String curr: rootServer.getConnected().keySet()) {
			if (!curr.equals(this.clientUsername)) {
				out.println(curr);
			}
		}
	}

	/**
	 * Authenticates user by getting username and password from user and 
	 * querying authList in rootServer.
	 * 
	 * @return
	 * @throws IOException
	 */
	public boolean authenticate() throws IOException {
		out.println(">Username: ");
		String username = in.readLine();
		// After reading in username for the first time, check if client 
		// is blocked from this IP address
		if (verbose)
			System.out.println("ServerThread: Checking if client is blocked");
		this.clientAddress = clientSocket
				.getInetAddress()
				.getCanonicalHostName();

		if (clientIsBlocked(username)) {
			return false;
		}

		out.println(">Password: ");
		String password = in.readLine();

		int numFailures = 1;
		String lastUsername = username;

		while (!rootServer.checkUserPass(username, password)
				&& numFailures++ < 3) {
			out.println("Your credentials could not be authenticated, "
					+ "please try again.");

			out.println(">Username: ");
			username = in.readLine();

			/* If user gives a different username on a different 
			 * authentication exchange, first check whether client is blocked. 
			 * If client is blocked, end authentication session. Otherwise, 
			 * reset numFailures and set lastUsername to username. 
			 * The reason for this is that a 
			 * username should only be blocked if the same one is given 
			 * three times in a row. Otherwise, the number of failed 
			 * login attempts should be reset.
			 */
			if (!username.equals(lastUsername)) {
				if (clientIsBlocked(username)) {
					return false;
				} else {
					numFailures = 1;
					lastUsername = username;
				}
			}

			out.println(">Password: ");
			password = in.readLine();
		}

		if (numFailures >= 3) {
			if (verbose) 
				System.out.println("ServerThread: Authentication failed. " +
						"Blocking " + username);
			
			this.rootServer.block(this.clientAddress, username);
			out.println("You have failed to provide a valid username " +
					"and password.");
			out.println("You have been blocked for " + 
					rootServer.getBlockTime() +
					" microseconds.");
			
			return false;
		
		} else if (this.rootServer.isConnected(username)) {
			out.println("user " + username + " is already connected.");
			out.println("Please either log off from your other session, or " +
					"log on as a different user.");
			return false;
			
		// Else, client successfully connected.
		} else {
			this.clientUsername = username;
			rootServer.connect(username, this);
			if (verbose)
				System.out.println("ServerThread: Connected to user: '" + 
						username + "'.");
			
			return true;
		}
	}

	/**
	 * Adds a separate client to this client's blocklist. This list will be 
	 * queried when a user sends a message to this client, and the message will 
	 * be blocked if they are on the client's blocklist.
	 * 
	 * @param username Client to be blocked
	 */
	private void blockClient(String username) {
		if (!rootServer.getAuthList().containsKey(username)) {
			out.println(username + " is not a user of this chat client"
					+ " so they can't be blocked.\n"
					+ "Please provide another command.");
		} else if (!username.equals(this.clientUsername)) {
			blockList.add(username);
			out.println("You have successfully blocked "+ username + 
					" from sending you messages.");
		} else {
			out.println("You cannot block yourself.\nPlease provide another "
					+ "command.");
		}
	}

	/**
	 * Takes client off of blocked list
	 * 
	 * @param username Client to be taken off of block list
	 */
	private void unblockClient(String username) {
		if (!rootServer.getAuthList().containsKey(username)) {
			out.println(username + " is not a user of this chat client so"
					+ " they cannot be unblocked.\n"
					+ "Please provide another command.");
		} else if (blockList.contains(username)) {
			blockList.remove(username);
			out.println("You have successfully unblocked " + username + ".");
		} else {
			out.println(username + " cannot be unblocked because they are "
					+ "not blocked.");
		}
	}

	/**
	 * Checks whether this client is blocked and returns true if so, false
	 * otherwise.
	 * @param username
	 * @return True if address/username is blocked, false otherwise.
	 */
	private boolean clientIsBlocked(String username) {
		if (rootServer.isBlocked(this.clientAddress, username)) {
			out.println("Username " + username + " is still blocked at " +
					"address " + this.clientAddress + ". " +
					"Please try again later.");
			try {
				clientSocket.close();
			} catch (IOException e) {
				System.err.println("There was an error closing the connection "
						+ "to the client after verifying that he/she was "
						+ "blocked.");
				e.printStackTrace();
			}
			return true;
		} else {
			return false;
		}
	}
}
