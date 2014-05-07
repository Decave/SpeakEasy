package client;

import java.io.IOException;
import java.net.*;
import java.io.*;

public class Client {
	private int port;
	private String host;
	private Socket server = null;
	private BufferedReader in = null;
	private BufferedReader stdIn = null;
	private PrintWriter out = null;

	public Client(int port, String host) {
		this.port = port;
		this.host = host;
	}

	public static void main(String[] args) {
		if (args.length == 0) {
			System.err.println("Usage: java Client <host> <port number> [-v]");
			System.exit(1);
		}

		String host = args[0];
		int portNum = Integer.parseInt(args[1]);

		boolean verbose = false;
		for(String arg : args) {
			if (arg.equals("-v")) {
				verbose = true;
			}
		}

		Client client = new Client(portNum, host);

		try {
			if (verbose)
				System.out.println("Opening connection to server.");
			client.server = new Socket(host, portNum);

			if (verbose)
				System.out.println("Getting input stream from server.");
			client.in = new BufferedReader(
					new InputStreamReader(client.server.getInputStream()));
			client.stdIn = new BufferedReader(
					new InputStreamReader(System.in));

			if (verbose)
				System.out.println("Establishing output stream to server.");
			client.out = new PrintWriter(
					client.server.getOutputStream(), true);
			Thread clientInThread = new Thread(new ClientReaderThread(
					client.in));
			clientInThread.start();

			Thread clientOutThread = new Thread(new ClientWriterThread(
					client.out, 
					client.stdIn));
			clientOutThread.start();

			// Keep threads running until client says logout
			try {
				clientInThread.join();
				clientOutThread.join();
			} catch (InterruptedException e) {
				System.err.println("There was an error during communication " +
						"with the server.");
				e.printStackTrace();
			}


		} catch (IOException e) {
			System.err.println("There was an error estabishing the connection "
					+ "with the chat server " + host + " on port " + portNum
					+ ".");
			e.printStackTrace();
		}
		try {
			if (verbose)
				System.out.println("Closing connection to server");
			client.in.close();
			client.out.close();
			client.server.close();
			client.stdIn.close();
		} catch (IOException e) {
			System.err.println("There was an error closing resources.");
			e.printStackTrace();
		}
	}
}
