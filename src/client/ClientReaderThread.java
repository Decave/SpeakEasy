package client;

import java.io.BufferedReader;
import java.io.IOException;

public class ClientReaderThread implements Runnable {

	private BufferedReader in;

	public ClientReaderThread(BufferedReader in) {
		this.in = in;
	}

	public void run() {
		String serverMessage, previousMessage = "";
		try {
			while (true) {
				try {
					serverMessage = in.readLine();
					if (serverMessage.contains(">Command")) {
						System.out.print(">Command: ");
					} else if (serverMessage.contains(">Username")) {
						System.out.print(">Username: ");
					} else if (serverMessage.contains("Password")) {
						System.out.print(">Password: ");
					} else if (serverMessage.endsWith("__broadcast")) {
						System.out.println(serverMessage.substring(0, serverMessage.length() - 11));
					} else if(previousMessage.contains(">Command")) {
						System.out.println("\n" + serverMessage);
					} else {
						System.out.println(serverMessage);
					}
					previousMessage = serverMessage;
					if (serverMessage.equals("Thank you for using the chat " +
							"program!") || 
							serverMessage.equals("You have timed out due to " +
									"inactivity. Please log back in if you " +
									"would like to continue chatting.") || 
									serverMessage.contains("is still blocked at " +
											"address") || 
											serverMessage.contains("You have been " +
													"blocked for ") || 
													serverMessage.contains("Please " +
															"either log off from " +
															"your other session, " +
															"or log on as a " +
															"different user.")) {
						System.exit(1);
					}	
				} catch (IOException e) {
					System.err.println("There was an error reading in a message " +
							"from the server.");
					e.printStackTrace();
				}
			}
		} catch (NullPointerException e) {
			System.err.println("The server shut down unexpectedly. " +
					"Closing program.");
			System.exit(1);
		}
	}
}
