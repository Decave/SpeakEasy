package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class ClientWriterThread implements Runnable {
	private PrintWriter out;
	private BufferedReader stdIn;
	
	public ClientWriterThread(PrintWriter out, BufferedReader stdIn) {
		this.out = out;
		this.stdIn = stdIn;
	}
	
	public void run() {
		String command;
		while (true) {
			try {
				command = stdIn.readLine();
				if (command == null) {
					System.out.println("\nYou cannot send a null command to " +
							"the server. Please try again.");
					System.out.print(">Command: ");
					continue;
				}
				out.println(command);
				if (command.equals("logout")) {
					break;
				}
			} catch (IOException e) {
				System.err.println("There was an error reading from StdIn " +
						"and sending message to server.");
				e.printStackTrace();
			}
		}
	}
}
