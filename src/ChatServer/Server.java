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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.net.URL;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import javax.swing.UIManager.LookAndFeelInfo;

/**
 * Server for Internet chat room
 *
 * @author NTropy
 * @ver 2.0
 */
public class Server extends Thread {

    private static final int DEFAULT_PORT = 22333;
    private static final int DEFAULT_CONNECTION_NUM = 1;
    
    private static ActionEvent sendOverride;

    private static BufferedReader fromClient;

    private static int clientPos; //Client pos in array
    private static int connectionNum;
    private static int portNum;
    
    private static JButton jbtnSend; 
    
    private static JFrame jfrm;
    
    private static JTextArea jtaDisplay; 
    
    private static JScrollPane jscrlp; 
    
    private static JTextField jtfInput;

    private static PrintWriter toClient;

    private static ServerSocket connectionSocket;

    private static Socket clients[];

    private static String clientNames[];
    private static String clientName;
    private static String ipLocal;
    private static String ipPublic;

    private static Thread threadArr[]; //array of client threads

    /**
     * Constructor for client threads, tracked via name and number
     *
     * @param n
     * @param c
     */
    private Server(String n, int c) {
        clientPos = c;
        clientName = n;
    }

    /**
     * Creates threads
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
        
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException exe) {
            System.err.println("Nimbus unavailable");
        }

        URL publicIP = new URL("http://checkip.amazonaws.com");

        BufferedReader ipReader = new BufferedReader(new InputStreamReader(publicIP.openStream()));

        ipPublic = ipReader.readLine();
        
        jfrm = new JFrame("Chat Server");
        jfrm.setLayout(new BorderLayout()); //sets layout based on borders
        jfrm.setSize(500, 420); //sets size
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize(); //gets screen dimensions
        
        double screenWidth = screenSize.getWidth(); //width of screen
        double screenHeight = screenSize.getHeight(); //height of screen
        
        jfrm.setLocation((int) screenWidth / 2 - 250, (int) screenHeight / 2 - 210); //sets location of chat to center

        jtaDisplay = new JTextArea(20, 30); //size of display
        jtaDisplay.setEditable(false); //display not editable
        jtaDisplay.setLineWrap(true); //lines wrap down
        
        jscrlp = new JScrollPane(jtaDisplay); //makes dispaly scrollable
        
        jtfInput = new JTextField(30); //sets character width of input field

        jbtnSend = new JButton("Send"); //sets button text

        jbtnSend.addActionListener(new handler()); //adds listener to button
        
        KeyListener key = new handler(); //adds handler for 'enter' key
        
        jtfInput.addKeyListener(key); //adds keylistener for 'enter'
        jfrm.add(jscrlp, BorderLayout.PAGE_START); //adds scrollable display to main frame

        sendOverride = new ActionEvent(jbtnSend, 1001, "Send"); //allows key to trigger same method as button

        JPanel p1 = new JPanel(); //panel for input/button
        
        p1.setLayout(new FlowLayout()); //flow layout for input/button
        p1.add(jtfInput, BorderLayout.LINE_END); //adds input to panel
        p1.add(jbtnSend, BorderLayout.LINE_END); //adds button to panel

        jfrm.add(p1, BorderLayout.PAGE_END); //add button/input to main frame

        jfrm.setVisible(true); //makes frame visible

        jfrm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); //kills application on close

        BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));

        jtaDisplay.setText("Enter desired port (enter for default: " + DEFAULT_PORT + " )");

        String temp = consoleInput.readLine();

        if (temp.equals("")) {
            portNum = DEFAULT_PORT;
        } else {
            portNum = Integer.parseInt(temp);
        }

        connectionSocket = new ServerSocket(portNum);

        System.out.println("Number of connections(enter for default: " + DEFAULT_CONNECTION_NUM + " )");

        temp = consoleInput.readLine();

        if (temp.equals("")) {
            connectionNum = DEFAULT_CONNECTION_NUM;
        } else {
            connectionNum = Integer.parseInt(temp);
        }

        clients = new Socket[connectionNum];
        threadArr = new Thread[connectionNum];

        clientNames = new String[connectionNum];

        System.out.println("Local IP: " + ipLocal);
        System.out.println("Public IP: " + ipPublic);

        for (int i = 0; i < clients.length; i++) {
            System.out.println("Waiting for connection...");

            clients[i] = connectionSocket.accept();

            System.out.println("Connection found for Client: " + (i + 1));

            toClient = new PrintWriter(clients[i].getOutputStream(), true);

            fromClient = new BufferedReader(new InputStreamReader(clients[i].getInputStream()));

            clientNames[i] = fromClient.readLine();
            for (int j = 0; j < clientNames.length; j++) {
                if (i != j) {
                    if (clientNames[i].equals(clientNames[j])) {
                        clientNames[i] = clientNames[i] + (i + 1);
                    }
                }
            }

            toClient.println(i + 1);
            toClient.println("Welcome to the Chatroom, " + (clientNames[i]) + "!");

            threadArr[i] = new Server(clientNames[i], i);

            threadArr[i].start();
        }
    }

    /**
     * Thread code to get/send input from/to clients
     */
    @Override
    public void run() {
        int currentClient = clientPos;
        String tName = clientName;

        try {
            fromClient = new BufferedReader(new InputStreamReader(clients[currentClient].getInputStream()));

            String inputLine;
            while (true) {
                if ((inputLine = fromClient.readLine()) != null) { //When input is detected
                    System.out.println("C:" + (currentClient + 1) + " " + tName + ": " + inputLine);

                    for (int i = 0; i < clients.length; i++) {
                        if (clients[i] != null) {
                            toClient = new PrintWriter(clients[i].getOutputStream(), true);

                            if (i != currentClient) //prevents sending own message back to client
                            {
                                toClient.println(tName + ": " + inputLine);
                            }
                        }
                    }
                }
            }
        } catch (IOException e) { //If connection lost
            System.err.println("Client was stopped");
            try {
                System.out.println("Waiting for connection...");
                clients[currentClient] = connectionSocket.accept();
                System.out.println("Connection found for Client: " + (currentClient + 1));

                toClient = new PrintWriter(clients[currentClient].getOutputStream(), true);

                fromClient = new BufferedReader(new InputStreamReader(clients[currentClient].getInputStream()));

                clientNames[currentClient] = fromClient.readLine();
                for (int j = 0; j < clientNames.length; j++) {
                    if (currentClient != j) {
                        if (clientNames[currentClient].equals(clientNames[j])) {
                            clientNames[currentClient] = clientNames[currentClient] + (currentClient + 1);
                        }
                    }
                }
                toClient.println(currentClient + 1);
                toClient.println("Welcome to the Chatroom, " + clientNames[currentClient] + "!");

                threadArr[currentClient] = new Server(clientNames[currentClient], currentClient); 
                
                threadArr[currentClient].start();
            } catch (IOException io2) {
                System.err.println("Client was stopped");
            }
        }
    }
    /**
     * Handles sending to server on button press
     */
    private static class handler implements ActionListener, KeyListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (ae.getActionCommand().equals("Send")) {
                sendCount++;

                input = jtfInput.getText();

                if (sendCount > 3) {
                    if (sendCount == 4) {
                        print = new Client(); //Create a Thread to run seperatly that is always looking to print incomming data to screen (see run() method).
                        print.start(); //Start Thread
                    }

                    String userInput; //String for user's input

                    if ((userInput = input) != null) { //If the userInput has characters(Triggers on enter key).  
                        jtaDisplay.setText(jtaDisplay.getText() + "You: " + input + "\n");
                        out.println(userInput); //Send the users message to server.
                    }
                } else if (sendCount == 1) {
                    String hostInput;
                    if ((hostInput = input) != null) {
                        hostName = hostInput;
                    }
                    if (hostName.equals("")) { //If enter, set host name to deafult
                        hostName = DEFAULT_HOSTNAME;
                    }
                    jtaDisplay.setText(jtaDisplay.getText() + "\nYou: " + input + "\n"); //displays input
                    jfrm.repaint(); //repaints
                    jtaDisplay.setText(jtaDisplay.getText() + "What is the Port? (hit enter for default: " + DEFAULT_PORT + " )\n"); //Ask Question
                } else if (sendCount == 2) {
                    String tempInput;
                    if ((tempInput = input) != null) {
                        if (!tempInput.equals("")) { //If not enter, set port number to temp
                            tempInput = input;
                            portNumber = Integer.parseInt(tempInput);
                        } else {//If enter, set port number to deafult
                            portNumber = DEFAULT_PORT;
                        }
                    }
                    jtaDisplay.setText(jtaDisplay.getText() + "You: " + input + "\n");
                    jfrm.repaint(); //repaints
                    jtaDisplay.setText(jtaDisplay.getText() + "What do you want to be called? (hit enter for default: " + DEFAULT_CLIENTNAME + " )\n");
                } else if (sendCount == 3) {
                    String tempInput;
                    if ((tempInput = input) != null) {
                        if (!tempInput.equals("")) {//If not enter, set client name to temp
                            clientName = tempInput;
                        } else { //If enter, set client name to deafult
                            clientName = DEFAULT_CLIENTNAME;
                        }
                        try {
                            echoSocket = new Socket(hostName, portNumber); //Create Socket for Client to connect to Server
                        } catch (UnknownHostException e) {
                            System.err.println("No Host");
                        } catch (IOException e) {
                            System.err.println("No Input");
                        }
                    }
                    jtaDisplay.setText(jtaDisplay.getText() + "You: " + input + "\n");
                    jfrm.repaint();

                    try {
                        out = new PrintWriter(echoSocket.getOutputStream(), true); //Print Writer to send input over Socket to the Server.

                        in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream())); //Set Buffered Reader "in" to input from Socket

                        out.println(clientName); //Send the name of the client to the server

                        clientNumber = Integer.parseInt(in.readLine());       //The first data the server sends to the client
                        jtaDisplay.setText(jtaDisplay.getText() + "Client number: " + clientNumber + "\n"); //is the position of the client in the server
                        jtaDisplay.setText(jtaDisplay.getText() + "Client name: " + clientName + "\n");     //array of clients. 
                        jtaDisplay.setText(jtaDisplay.getText() + in.readLine() + "\n"); //displays input from server
                        jfrm.repaint();
                    } catch (UnknownHostException uke) {
                        System.err.println("Host DNE");
                    } catch (IOException iexe) {
                        System.err.println("Unrecognized input");
                    }
                    jfrm.repaint();
                }
                jtfInput.setText("");
            }
        }

        /**
         * Invokes handler for server communication
         *
         * @param e
         */
        @Override
        public void keyPressed(KeyEvent e) //see comments from above
        {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                actionPerformed(sendOverride);
            }
        }

        /**
         * Necessary override, does nothing
         *
         * @param e
         */
        @Override
        public void keyReleased(KeyEvent e) {
        }

        /**
         * Necessary override, does nothing
         *
         * @param e
         */
        @Override
        public void keyTyped(KeyEvent e) {
        }
    }
}
