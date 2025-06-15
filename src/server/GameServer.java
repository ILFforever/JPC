package server;

import javafx.application.Platform;


import javafx.scene.control.TextArea;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import application.Main;
import application.SharedState;

public class GameServer {
	private static DatagramSocket serverSocket;
	private static Thread serverThread;
	private static Set<ClientInfo> clientAddresses = new HashSet<>();
	private static ConcurrentHashMap<ClientInfo, AtomicInteger> clientPingCount = new ConcurrentHashMap<>();
	private static Timer pingCheckTimer;
	
	private static ConcurrentHashMap<String, PlayerInfo> playerList = new ConcurrentHashMap<>();

	public static void startBroadcasting(SharedState state, TextArea logArea, String serverName, int serverPort) {
		Thread thread = new Thread(() -> {
			try {
				DatagramSocket socket = new DatagramSocket();
				socket.setBroadcast(true);
				String broadcastMessage = serverName + ":" + serverPort; // Include port in broadcast message
				byte[] buf = broadcastMessage.getBytes(StandardCharsets.UTF_8);
				InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");
				DatagramPacket packet = new DatagramPacket(buf, buf.length, broadcastAddress, 4446);

				while (state.isBroadcast()) {
					socket.send(packet);
					// log(logArea, "Broadcast sent.");
					Thread.sleep(1000);
				}
				socket.close();
			} catch (IOException | InterruptedException e) {
				log(logArea, "Error: " + e.getMessage());
				System.out.println("Error: " + e.getMessage());
			}
		});
		thread.start();
		log(logArea, "Broadcasting as " + serverName + " on port " + serverPort + "...");
	}

	public static void startServer(SharedState state, TextArea logArea, int serverPort) {
		serverThread = new Thread(() -> {
			try {
				serverSocket = new DatagramSocket(serverPort);
				log(logArea, "Server started on port " + serverPort + ", waiting for messages...");

				// Start the ping check timer
				pingCheckTimer = new Timer();
				pingCheckTimer.scheduleAtFixedRate(new TimerTask() {
					@Override
					public void run() {
						checkClientPings(logArea);
					}
				}, 0, 1000); // Ensure the period is positive and check every 1 second

				while (state.isBroadcast()) {
					byte[] buf = new byte[1024];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					serverSocket.receive(packet); //if you get a new packet
					String received = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
					InetAddress clientAddress = packet.getAddress();
					int clientPort = packet.getPort();

					if (received.startsWith("/name/")) { // First Contact
						String clientName = received.substring(6);
						ClientInfo clientInfo = new ClientInfo(clientAddress, clientPort, clientName);
						clientAddresses.remove(clientInfo); // Remove old client info if exists
						clientAddresses.add(clientInfo); // Add updated client info
						log(logArea, clientName + " has connected");
					} else if (received.startsWith("/sys/")) { // System Message
						if ("/sys/PING".equals(received)) {
							// System.out.println("Ping Received Pong Sent");
							String response = "/sys/PONG";
							buf = response.getBytes(StandardCharsets.UTF_8);
							packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
							serverSocket.send(packet);

							ClientInfo clientInfo = getClientInfo(clientAddress, clientPort);
							if (clientInfo != null) {
								clientPingCount.putIfAbsent(clientInfo, new AtomicInteger(0));
								clientPingCount.get(clientInfo).set(0);
							}
						} else if ("/sys/Test_Handshake".equals(received)) {
							String response = "/sys/ACK";
							buf = response.getBytes(StandardCharsets.UTF_8);
							packet = new DatagramPacket(buf, buf.length, clientAddress, clientPort);
							serverSocket.send(packet);
							System.out.println("Sent ACK to client at " + clientAddress + ":" + clientPort);
							Main.setDisconnectButtonDisable(false);
						} else if ("/sys/ls".equals(received)) {
							String response = "/ls/";
							response += "======LIST OF PLAYERS======\n";
							response += InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort();
							response += " - " + Main.getServerName() + "\n";
							if (clientAddresses.size() != 0) {
								for (ClientInfo clientInfo : clientAddresses) {
									String key = clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();
									response += key + " - " + clientInfo.getName();
								}
							}
							log(logArea,response);
							byte[] responseBuf = response.getBytes(StandardCharsets.UTF_8);
							DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length,
									packet.getAddress(), packet.getPort());
							serverSocket.send(responsePacket);
						} else { // Other server messages here
							System.out.println("Received System Message: " + received);
						}

					} else if (received.startsWith("/data/")) { //not used in this Lan app reserved for the game
//						String jsonStr = received.substring(6);
//						JSONObject json = new JSONObject(jsonStr);
//						double posX = json.getDouble("PosX");
//						double posY = json.getDouble("PosY");
//
//						// Update the player's position in playerList map
//						String clientKey = packet.getAddress().getHostAddress() + ":" + packet.getPort();
//						playerList.putIfAbsent(clientKey,
//								new PlayerInfo(packet.getAddress(), packet.getPort(), "player", 0, 0, 0, "active"));
//						PlayerInfo playerInfo = playerList.get(clientKey);
//						playerInfo.setX(posX);
//						playerInfo.setY(posY);
//
//						// Create JSON response
//						json = new JSONObject();
//
//						// Add server's position without PC name
//						String serverKey = InetAddress.getLocalHost().getHostAddress() + ":"
//								+ serverSocket.getLocalPort();
//						JSONObject serverData = new JSONObject();
//						serverData.put("position", new double[] { serverX, serverY });
//						serverData.put("name", Main.getServerName());
//						serverData.put("score", 1000);
//						serverData.put("status", "active");
//						json.put(serverKey, serverData);
//
//						// Add player positions without PC name
//						for (PlayerInfo info : playerList.values()) {
//							String key = info.getAddress().getHostAddress() + ":" + info.getPort();
//							JSONObject playerData = new JSONObject();
//							playerData.put("position", new double[] { info.getX(), info.getY() });
//							playerData.put("name", info.getName());
//							playerData.put("score", info.getScore());
//							playerData.put("status", info.getStatus());
//							json.put(key, playerData);
//						}
//
//						String response = "/data/" + json.toString();
//
//						// Send the response to the client
//						byte[] responseBuf = response.getBytes(StandardCharsets.UTF_8);
//						DatagramPacket responsePacket = new DatagramPacket(responseBuf, responseBuf.length,
//								packet.getAddress(), packet.getPort());
//						serverSocket.send(responsePacket);
					} else {
						ClientInfo clientInfo = getClientInfo(clientAddress, clientPort);
						if (clientInfo != null) {
							log(logArea, clientInfo.getName() + " : " + received);
							relayMessageToClients(clientInfo, received, logArea);
						} else {
							log(logArea, "Player : " + received + " from " + clientAddress + ":" + clientPort);
						}
					}

				}
				serverSocket.close();
			} catch (IOException e) {
				if (!e.getMessage().contains("Socket closed")) {
					log(logArea, "Error: " + e.getMessage());
					System.out.println("Error: " + e.getMessage());
				}
			}
		});
		serverThread.start();
	}


	private static void relayMessageToClients(ClientInfo sender, String message, TextArea logArea) {
		String relayMessage = "/r/" + sender.getName() + " : " + message;
		byte[] buf = relayMessage.getBytes(StandardCharsets.UTF_8);

		for (ClientInfo clientInfo : clientAddresses) {
			if (!clientInfo.equals(sender)) {
				try {
					DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
							clientInfo.getPort());
					serverSocket.send(packet);
					System.out.println("Relayed message to " + clientInfo.getAddress() + ":" + clientInfo.getPort());
				} catch (IOException e) {
					log(logArea, "Error relaying message to " + clientInfo.getAddress() + ":" + clientInfo.getPort()
							+ ": " + e.getMessage());
				}
			}
		}
	}

	private static ClientInfo getClientInfo(InetAddress address, int port) {
		for (ClientInfo clientInfo : clientAddresses) {
			if (clientInfo.getAddress().equals(address) && clientInfo.getPort() == port) {
				return clientInfo;
			}
		}
		return null;
	}

	private static void checkClientPings(TextArea logArea) {
		for (ClientInfo clientInfo : clientPingCount.keySet()) {
			int missedPings = clientPingCount.get(clientInfo).incrementAndGet();
			if (missedPings > 5) {
				log(logArea, "Client " + clientInfo.getAddress() + ":" + clientInfo.getPort()
						+ " has missed 5 PINGs and is considered disconnected.");
				clientPingCount.remove(clientInfo);
				clientAddresses.remove(clientInfo);
				// Remove player position using clientKey
				String clientKey = clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();
				playerList.remove(clientKey);
			}
		}
	}

	public static void stopServer() {

		clientPingCount.clear();
		clientAddresses.clear();
		playerList.clear();

		if (serverSocket != null && !serverSocket.isClosed()) {
			serverSocket.close();
		}
		if (serverThread != null && serverThread.isAlive()) {
			serverThread.interrupt();
		}
		if (pingCheckTimer != null) {
			pingCheckTimer.cancel(); // Stop the ping check timer
		}
	}

	public static void sendMessageToClients(String message, TextArea logArea) {
		if (clientAddresses.isEmpty()) {
			log(logArea, "No connected clients to send the message.");
			return;
		}

		try {
			System.out.println(message);
			String servermsg;
            // Add logic for special messages to server
            if (message.startsWith("/sys/")) {
            	if (message.toLowerCase().equals("/sys/ls")) {
    				servermsg = "/ls/";
    				servermsg += "======LIST OF PLAYERS======\n";
    				servermsg += InetAddress.getLocalHost().getHostAddress() + ":" + serverSocket.getLocalPort();
    				servermsg += " - " + Main.getServerName() + "\n";
    				if (clientAddresses.size() != 0) {
    					for (ClientInfo clientInfo : clientAddresses) {
    						String key = clientInfo.getAddress().getHostAddress() + ":" + clientInfo.getPort();
    						servermsg += key + " - " + clientInfo.getName();
    					}
    				}
    				log(logArea,servermsg);
                }
            	else {
            		 servermsg = message; 
            	}
            }  
            else {
                servermsg = "/sname/" + Main.getServerName() + " : " + message; // Normal message
                log(logArea, "You : " + message);
            }
            
            //send packet stuff
			byte[] buf = servermsg.getBytes(StandardCharsets.UTF_8);
			for (ClientInfo clientInfo : clientAddresses) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length, clientInfo.getAddress(),
						clientInfo.getPort());
				serverSocket.send(packet);
			}
			
		} catch (IOException e) {
			log(logArea, "Error sending message to clients : " + e.getMessage());
		}
	}

	private static void log(TextArea logArea, String message) {
		Platform.runLater(() -> logArea.appendText(message + "\n"));
	}

	public static ConcurrentHashMap<String, PlayerInfo> getplayerList() {
		return playerList;
	}

	// Method to get local address and port in the required format
	public static String getLocalAddressPort() {
		if (serverSocket == null) {
			return "unknown:0";
		}
		try {
			InetAddress localAddress = InetAddress.getLocalHost();
			int localPort = serverSocket.getLocalPort();
			return localAddress.getHostAddress() + ":" + localPort;
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return "unknown:0";
		}
	}

}
