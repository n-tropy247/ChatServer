/*
 * Copyright (C) 2018 Ryan Castelli
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ChatServer;

import java.net.*;
import java.io.*;
import java.net.InetAddress;

/**
 * Server for Internet chat room
 *
 * @author NTropy
 * @ver 2.0
 */
public class Server extends Thread {

    private static Socket clients[]; //Create an array of Sockets to store all of the Client Sockets
    private static String names[]; //Create an array of names to store all of the names of the Clients
    private static String name; //A Variable to temporarly hold a name for the client
    private static int client; //A Variable to temporarly hold a position in the client array
    private static int portNumber; //The port to make the server on
    private static int connections; //Number of Clients
    private static final int DEFAULT_PORT = 22333; //Default Port
    private static final int DEFAULT_CONNECTIONS = 1; //Default Connections
    private static Thread threads[]; //array for threads for connection
    private static String ipLocal; //storage for local ip
    private static String ipPublic; //storage for public ip
    private static ServerSocket connectionSocket; //socket to connect to client
    private static Thread threadArr[]; //array of client threads
    private static PrintWriter toClient; //send to client
    private static BufferedReader fromClient; //get from client

    /**
     * Constructor for client threads, tracked via name and number
     *
     * @param n
     * @param c
     */
    private Server(String n, int c) {
        client = c;
        name = n;
    }

    /**
     * Creates threads in terminal
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            ipLocal = (localhost.getHostAddress()).trim();
        } catch (UnknownHostException hostErr) {
            System.err.println("Localhost not recognized, can't find IP!");
        }

        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader ipReader = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
        ipPublic = ipReader.readLine(); //you get the IP as a String

        BufferedReader person = new BufferedReader(new InputStreamReader(System.in)); //Get input from Person

        System.out.println("Port? (hit enter for default: " + DEFAULT_PORT + " )"); //Ask Question
        String temp = person.readLine(); // Temp String for input
        if (!temp.equals("")) { //If not enter, set port number to temp
            portNumber = Integer.parseInt(temp);
        } else { //If enter, set port number to default
            portNumber = DEFAULT_PORT;
        }

        connectionSocket = new ServerSocket(portNumber);//Create Socket for Server to connect to clients

        System.out.println("How many Connections (hit enter for default: " + DEFAULT_CONNECTIONS + " )"); //Ask Question
        temp = person.readLine(); // Use same temp String for input
        if (!temp.equals("")) { //If not enter, set connections to temp
            connections = Integer.parseInt(temp);
        } else { //If enter, set connections to deafult
            connections = DEFAULT_CONNECTIONS;
        }

        clients = new Socket[connections]; //Set the array length of clients to the amount of connections
        threadArr = new Thread[connections];//Create a thread array with the same size as the clients so
        //each client has a Thread
        names = new String[connections]; //Set the array length of names to the amount of connections

        System.out.println("Local IP: " + ipLocal);
        System.out.println("Public IP: " + ipPublic);

        for (int i = 0; i < clients.length; i++) { //Loop through all the clients
            System.out.println("Waiting for connection...");
            clients[i] = connectionSocket.accept(); //Wait for a client to connect and set a Socket in the array to
            //the socket the client connected on
            System.out.println("Connection found for Client: " + (i + 1));

            //Sets a writer to print out to the client that just connected
            toClient = new PrintWriter(clients[i].getOutputStream(), true);

            //Sets a reader to get input from the client that just connected
            fromClient = new BufferedReader(new InputStreamReader(clients[i].getInputStream()));

            names[i] = fromClient.readLine(); //Sets the name at position i in the array to the name the clinet sends
            for (int j = 0; j < names.length; j++) { //Loops through all the names
                if (i != j) { //If the name is being checked is not the one that was just put in
                    if (names[i].equals(names[j])) //If the current name is the same as another name
                    {
                        names[i] = names[i] + (i + 1); //Give it a the number of the client at the end
                    }
                }
            }

            toClient.println(i + 1); //Send back the clients number
            toClient.println("Welcome to the Chatroom! You are client " + (i + 1) + "!");

            threadArr[i] = new Server(names[i], i); //Make a Thread using the postion in the array of the 
            //client and the name of the client
            threadArr[i].start(); //Start that Thread
        }
    }

    /**
     * Thread code to get/send input from/to clients
     */
    @Override
    public void run() {
        int tClient = client; //A local variable for the thread that stores the client position in array
        String tName = name; //A local variable for the thread that stores the client name

        try { //Try statement to catch errors from loosing contact with client.
            fromClient = new BufferedReader(new InputStreamReader(clients[tClient].getInputStream()));
            //Buffered Reader for taking input from the inputstream of the Socket connected to the client of the Thread.   

            String inputLine; //String to hold input from the client of the Thread
            while (true) {
                if ((inputLine = fromClient.readLine()) != null) { //Waits for input from client of Thread
                    System.out.println("C:" + (tClient + 1) + " " + tName + ": " + inputLine);
                    //Prints Input from Client of Thread.
                    for (int i = 0; i < clients.length; i++) { //Loops through all clients
                        if (clients[i] != null) { //If the client Exists
                            toClient = new PrintWriter(clients[i].getOutputStream(), true);
                            //Sets a writer to print out to all clients (because of the for loop)

                            if (i != tClient) //Sends to all clients but the client of the thread
                            {
                                toClient.println(tName + ": " + inputLine); //Sends input
                            }
                        }
                    }
                }
            }
        } catch (IOException e) { //Catch a error
            System.err.println("Client was stopped");
            try {
                System.out.println("Waiting for connection...");
                clients[tClient] = connectionSocket.accept(); //Wait for a client to connect and set a Socket in the array to
                //the socket the client connected on
                System.out.println("Connection found for Client: " + (tClient + 1));

                toClient = new PrintWriter(clients[tClient].getOutputStream(), true);
                //Sets a writer to print out to the client that just connected

                fromClient = new BufferedReader(new InputStreamReader(clients[tClient].getInputStream()));
                //Sets a reader to get input from the client that just connected

                names[tClient] = fromClient.readLine(); //Sets the name at position i in the array to the name the clinet sends
                for (int j = 0; j < names.length; j++) { //Loops through all the names
                    if (tClient != j) { //If the name is being checked is not the one that was just put in
                        if (names[tClient].equals(names[j])) //If the current name is the same as another name
                        {
                            names[tClient] = names[tClient] + (tClient + 1); //Give it a the number of the client at the end
                        }
                    }
                }
                toClient.println(tClient + 1); //Send back the clients number
                toClient.println("Welcome to the Chatroom! You are client " + (tClient + 1) + "!");

                threads[tClient] = new Server(names[tClient], tClient); //Make a Thread using the postion in the array of the 
                //client and the name of the client
                threads[tClient].start(); //Start that Thread
            } catch (IOException io2) {
                System.err.println("Client was stopped");
            }
        }
    }
}
