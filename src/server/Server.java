package server;

import java.net.*;
import java.io.*;
import java.text.NumberFormat;
import java.util.*;

/**
 * 
 * This class functions as the root server of the chat application. It holds the 
 * list of acceptable username/password combinations, and constantly listens on 
 * the port provided by the user at the command line to open new client/server 
 * connections.
 * 
 * @author David C. Vernet
 *
 */
public class Server {

	private ServerSocket serverSocket;
	private TreeMap<String, String> authList;
	private TreeMap<String, TreeMap<String, Date>> blocked = 
			new TreeMap<String, TreeMap<String, Date>>();
	private TreeMap<String, ServerThread> connected = 
			new TreeMap<String, ServerThread>();
	private TreeMap<String, Date> connectedLastHr = 
			new TreeMap<String, Date>();
	private TreeMap<String, HashSet<String>> offlineMessages = 
			new TreeMap<String, HashSet<String>>();
	private TreeMap<String, Integer> statistics = 
			new TreeMap<String, Integer>();
	private final long BLOCK_TIME;
	private final long LAST_HOUR;
	private final long TIME_OUT;
	private final Object blockedLock = new Object();
	private final Object connectedLock = new Object();
	private final Object connectedLastHrLock = new Object();
	private final Object offlineMessageLock = new Object();
	private final Object statisticsLock = new Object();

	/**
	 * Constructs ServerSocket and user-password list. This socket continually 
	 * listens on portNum for new connections.
	 * 
	 * @param portNum Port on which Server listens for new Client chat 
	 * connections.
	 */
	public Server(int portNum) {
		try {
			serverSocket = new ServerSocket(portNum);
		} catch (IOException e) {
			System.err.println(
					"The server had an error opening a socket "
							+ "on port " + portNum + ".");
			e.printStackTrace();
			System.exit(1);
		}
		try {
			authList = new TreeMap<String, String>();
			File userPass = new File("user_pass.txt");
			BufferedReader reader = null;
			reader = new BufferedReader(new FileReader(userPass));
			String line;
			String[] entries = new String[2];

			// Populate authList from user_pass.txt
			while ((line = reader.readLine()) != null) {
				entries = line.split(" ");
				authList.put(entries[0], entries[1]);
			}

			// Close reader -- will not reread user_pass.txt more than once.
			reader.close();
		} catch (IOException e) {
			System.err.println(
					"There was an error reading in the list of "
							+ "authorized users and passwords.");
			e.printStackTrace();
			System.exit(1);
		}

		this.BLOCK_TIME = 60000L;
		this.LAST_HOUR = 3600000L;
		this.TIME_OUT = 1800000L;
	}

	/**
	 * Constructs ServerSocket, user-password list, and initializes 
	 * BLOCK_TIME and LAST_HOUR from constructor parameters. 
	 * This socket continually listens on portNum for new connections.
	 * 
	 * @param portNum Port on which Server listens for new Client chat 
	 * connections.
	 */
	public Server(int portNum, long BLOCK_TIME, long LAST_HOUR, long TIME_OUT) {
		try {
			serverSocket = new ServerSocket(portNum);
		} catch (IOException e) {
			System.err.println(
					"The server had an error opening a socket "
							+ "on port " + portNum + ".");
			e.printStackTrace();
			System.exit(1);
		}
		try {
			authList = new TreeMap<String, String>();
			File userPass = new File("user_pass.txt");
			BufferedReader reader = null;
			reader = new BufferedReader(new FileReader(userPass));
			String line;
			String[] entries = new String[2];

			// Populate authList from user_pass.txt
			while ((line = reader.readLine()) != null) {
				entries = line.split(" ");
				authList.put(entries[0], entries[1]);
			}

			// Close reader -- will not reread user_pass.txt more than once.
			reader.close();
		} catch (IOException e) {
			System.err.println(
					"There was an error reading in the list of "
							+ "authorized users and passwords.");
			e.printStackTrace();
			System.exit(1);
		}

		if (BLOCK_TIME >= 0L) {
			this.BLOCK_TIME = BLOCK_TIME;
		} else {
			this.BLOCK_TIME = 60000L;
		}

		if (LAST_HOUR >= 0L) {
			this.LAST_HOUR = LAST_HOUR;
		} else {
			this.LAST_HOUR = 3600000L; 
		}

		if (TIME_OUT >= 0L) {
			this.TIME_OUT = TIME_OUT;
		} else {
			this.TIME_OUT = 1800000L;
		}
	}

	/**
	 * Checks whether the username and password provided by the client is 
	 * contained in user_pass.txt
	 * 
	 * @param userName Username (first response by client)
	 * 
	 * @param passWord Password (second response by client)
	 * 
	 * @return True if username/password combination exists, false otherwise.
	 */
	public boolean checkUserPass(String userName, String passWord) {
		// Corner cases check. Could also throw Null Pointer Exception
		if (userName == null || passWord == null) {
			return false;
		}

		if (!authList.containsKey(userName)) {
			return false;
		} else {
			return authList.get(userName).equals(passWord);
		}
	}

	public TreeMap<String, String> getAuthList() {
		return this.authList;
	}

	public static void main(String[] args) {

		// If port number not given as argument, print usage statement
		if (args.length == 0) {
			System.err.println("Usage: java Server <port number> " +
					"[--BLOCK_TIME, --LAST_HOUR, --TIME_OUT] [-v]");
			System.exit(1);
		}

		boolean verbose = false;

		long blockTime = -1L;
		long lastHour = -1L;
		long timeOut = -1L;

		StringBuilder commandLine = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			if(args[i].contains("BLOCK_TIME")) {
				blockTime = Long.parseLong(args[i]
						.substring(args[i]
								.indexOf("=") + 1, args[i]
										.length()));
				commandLine.append("Server: Got command line argument " +
						"--BLOCK_TIME=" + blockTime + "\n");
			} else if (args[i].contains("LAST_HOUR")) {
				lastHour = Long.parseLong(args[i]
						.substring(args[i]
								.indexOf("=") + 1, args[i]
										.length()));
				commandLine.append("Server: Got command line argument " +
						"--LAST_HOUR=" + lastHour + "\n");
			} else if (args[i].contains("TIME_OUT")) {
				timeOut = Long.parseLong(args[i]
						.substring(args[i]
								.indexOf("=") + 1, args[i]
										.length()));
				commandLine.append("Server: Got command line argument " +
						"--TIME_OUT=" + timeOut + "\n");
			} else if (args[i].equals("-v")) {
				System.out.println("Server: Got command line argument " + 
						"'-v.' Entering verbosity mode.");
				verbose = true;
			}
		}

		String commandLineStr = commandLine.toString();
		if (commandLineStr.length() > 0) {
			commandLineStr = commandLineStr
					.substring(0, commandLineStr.length() - 1);
		}

		if(verbose)
			System.out.println(commandLineStr);


		int portNum = Integer.parseInt(args[0]);

		if (verbose)
			System.out.println("Server: Instantiating Server on port " + 
					portNum + ".");
		Server rootServer = new Server(portNum, blockTime, lastHour, timeOut);

		while (true) {
			try {
				if (verbose)
					System.out.println("Server: Waiting for client to connect.");

				// Wait for client to connect, and obtain socket with client.
				Socket clientSocket = rootServer.serverSocket.accept();

				if (verbose)
					System.out.println("Server: Received request to connect.");

				// Open up PrintWriter to client and set autoflush to true
				PrintWriter out = new PrintWriter
						(clientSocket.getOutputStream(), true);

				// Open up BufferedReader with client
				BufferedReader in = new BufferedReader
						(new InputStreamReader
								(clientSocket.getInputStream()));


				if (verbose)
					System.out.println("Server: Creating and running new " +
							"thread to serve client.");
				/* 
				 * Pass all 3 I/O arguments to new server thread (who will now 
				 * communicate with client), pass reference to root server 
				 * (this), and resume listening on port args[0]
				 */
				Thread serverThread = new Thread(
						new ServerThread(
								clientSocket, 
								in, 
								out, 
								rootServer, 
								rootServer.TIME_OUT, 
								verbose
								)
						);

				// Run serverThread
				serverThread.start();
			} catch (IOException e) {
				System.err.println("There was an error in the client/server "
						+ "communication.");
				e.printStackTrace();
			}
		}
	}

	/**
	 * Adds an IP address / username combination to the blocked list
	 * 
	 * @param client
	 */
	public void block(String address, String username) {
		synchronized (this.blockedLock) {
			if (!blocked.containsKey(address)) {
				blocked.put(address, new TreeMap<String, Date>());
				blocked.get(address).put(username, new Date());
			} else {
				blocked.get(address).put(username, new Date());
			}
		}
	}

	/**
	 * Verifies whether client is still blocked.
	 * 
	 * @param client
	 * 
	 * @return True if client is still blocked, false otherwise
	 */
	public boolean isBlocked(String address, String username) {
		synchronized (this.blockedLock) { 
			if (!blocked.containsKey(address)) {
				return false;
			}	else if (!blocked.get(address).containsKey(username)) {
				return false;
			} else {
				Date now = new Date();
				long elapsedTime = now.getTime() - 
						blocked
						.get(address)
						.get(username)
						.getTime();
				return elapsedTime < this.BLOCK_TIME;
			}
		}
	}


	/**
	 * Adds client/server pair to the connected map.
	 * 
	 * @param client
	 */
	public void connect(String username, ServerThread serverThread) {
		synchronized (this.connectedLock) { 
			connected.put(username, serverThread);
		}
	}

	/**
	 * Removes client/server pair from the connected map.
	 * 
	 * @param client
	 */
	public void disconnect(String client) {
		synchronized (this.connectedLock) {
			connected.remove(client);
		}
	}

	/**
	 * Determines whether a client is currently connected to the chat client.
	 * 
	 * @param username Username of connected client
	 * 
	 * @return True if client is connected, false otherwise.
	 */
	public boolean isConnected(String username) {
		synchronized (this.connectedLock) {
			return this.connected.containsKey(username);
		}
	}

	/**
	 * @return Map of usernames to the ServerSockets they are connected to.
	 */
	public TreeMap<String, ServerThread> getConnected() {
		synchronized (this.connectedLock) {
			return this.connected;
		}
	}

	/**
	 * @return Map containing usernames and times that they were authenticated.
	 * When a user calls wholasthr connectedLastHr is 'refreshed' to reset the 
	 * authentication time of all connected users to the current time, and all 
	 * users who have been disconnected for at least LAST_HOUR time are removed.
	 */
	public TreeMap<String, Date> getConnectedLastHr() {
		synchronized (this.connectedLastHrLock) {
			return this.connectedLastHr;
		}
	}

	/**
	 * When client connects, add them to the 
	 * 
	 * @param username Username of client to be added
	 */
	public void addConnectedLastHr(String username) {
		synchronized (this.connectedLastHrLock) {
			this.connectedLastHr.put(username, new Date());
		}
	}

	/**
	 * 'Refreshes' connectedLastHr in a two step process:
	 *
	 * 1) Adds all currently connected users to connectedLastHr with current 
	 * time as value.
	 * 
	 * 2) Checks all remaining users in connectedLastHr. If time between now 
	 * and value in mapping is greater than LAST_HOUR, then user has been 
	 * disconnected for at least LAST_HOUR, and they are removed from list.
	 */
	public void refreshConnectedLastHr() {
		synchronized (connectedLastHrLock) {
			for (String username : connected.keySet()) {
				connectedLastHr.put(username, new Date());
			}

			for (String username: connectedLastHr.keySet()) {
				if (new Date().getTime()
						- this.connectedLastHr.get(username).getTime()
						>= this.LAST_HOUR) {
					connectedLastHr.remove(username);
				}
			}
		}
	}

	/**
	 * Returns messages that have been sent to username while they were 
	 * offline.
	 * 
	 * @param username Client who has been receiving messages.
	 * @return HashSet containing username's messages.
	 */
	public HashSet<String> getOfflineMessage(String username) {
		synchronized (this.offlineMessageLock) {
			if (this.offlineMessages.get(username) == null) {
				return new HashSet<String>();
			} else {
				HashSet<String> messages = 
						new HashSet<String>(offlineMessages.get(username));
				this.offlineMessages.remove(username);
				return messages;
			}
		}
	}

	/**
	 * Adds message to set of username's offline messages.
	 * 
	 * @param username
	 * 
	 * @param message
	 */
	public void addOfflineMessage(String username, String message) {
		synchronized (this.offlineMessageLock) {
			if (!this.offlineMessages.containsKey(username)) {
				this.offlineMessages.put(username, new HashSet<String>());
			}
			this.offlineMessages.get(username).add(message);
		}
	}

	/**
	 * Gets and returns BLOCK_TIME attribute of server.
	 * 
	 * @return BLOCK_TIME in microseconds.
	 */
	protected Long getBlockTime() {
		return this.BLOCK_TIME;
	}

	/**
	 * Gets and returns LAST_HOUR attribute of server.
	 * 
	 * @return LAST_HOUR in microseconds.
	 */
	protected Long getLastHour() {
		return this.LAST_HOUR;
	}

	/**
	 * When a command is run, increment the number of times it has been run to 
	 * the statistics tree for analytic use.
	 * 
	 * @param command Command to be run
	 */
	public void addStatistic(String command) {
		synchronized (this.statisticsLock) {
			if (!this.statistics.containsKey(command)) {
				this.statistics.put(command, 1);
			} else {
				int numCommands = this.statistics.get(command);
				this.statistics.put(command, numCommands + 1);
			}
		}
	}

	/**
	 * Constructs String that represents analysis of commands used in program.
	 * 
	 * @return Analytics string
	 */
	public String getStatisticsString() {
		synchronized (this.statisticsLock) {
			NumberFormat format = NumberFormat.getInstance();
			format.setMinimumFractionDigits(2);
			format.setMaximumFractionDigits(2);

			StringBuilder builder = new StringBuilder();
			builder.append("Statistics for commands run in chat program:\n\n");
			double totalCommands = 0;
			for (String command : this.statistics.keySet()) {
				totalCommands += statistics.get(command);
			}

			for (String command : this.statistics.keySet()) {
				Double numCommand = (double)this.statistics.get(command);
				builder.append(command +
						": " +
						statistics.get(command) +
						"/" +
						(int)totalCommands +
						" --- " +
						format.format((100*numCommand)/totalCommands) + 
						"%\n");
			}
			String ret = builder.toString();
			ret = ret.substring(0, ret.length() - 1);

			return ret;
		}
	}
}
