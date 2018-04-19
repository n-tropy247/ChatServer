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
    private static int sendCount = 0;

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
    private static String input;
    private static String ipLocal;
    private static String ipPublic;

    public static Thread threadArr[]; //array of client threads

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
                    jtaDisplay.setText(jtaDisplay.getText() + tName + ": " + inputLine);

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
                jtaDisplay.setText(jtaDisplay.getText() + "\nWaiting for connection...");
                clients[currentClient] = connectionSocket.accept();
                jtaDisplay.setText(jtaDisplay.getText() + "\nConnection found for Client: " + (currentClient + 1));

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

    private static void runThread() {
        clients = new Socket[connectionNum];
        threadArr = new Thread[connectionNum];
        clientNames = new String[connectionNum];
        jtaDisplay.setText(jtaDisplay.getText() + "\nLocal IP: " + ipLocal);
        jtaDisplay.setText(jtaDisplay.getText() + "\nPublic IP: " + ipPublic);
        for (int i = 0; i < clients.length; i++) {
            jtaDisplay.setText(jtaDisplay.getText() + "\nWaiting for connection...");

            try {
                clients[i] = connectionSocket.accept();

                jtaDisplay.setText(jtaDisplay.getText() + "\nConnection found for Client: " + (i + 1));

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
            } catch (IOException ie) {
                System.err.println("Read/Write Error");
            }
        }
    }

    /**
     * Handles passing variables on button press
     */
    private static class handler implements ActionListener, KeyListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            if (ae.getActionCommand().equals("Send")) {
                sendCount++;

                input = jtfInput.getText();

                switch (sendCount) {
                    case 1:
                        String portInput;
                        int portInt = 0;
                        try {
                            portInt = Integer.valueOf(input);
                        } catch (NumberFormatException nfe) {
                            System.err.println("Number invalid");
                        }
                        if (!(portInput = input).equals("")) {
                            portNum = portInt;
                        } else {
                            portNum = DEFAULT_PORT;
                        }
                        try {
                            connectionSocket = new ServerSocket(portNum);
                        } catch (IOException ioe) {
                            System.err.println("Socket could not be created");
                        }
                        jtaDisplay.setText(jtaDisplay.getText() + "\nYou: " + input);
                        jfrm.repaint();
                        jtaDisplay.setText(jtaDisplay.getText() + "\nNumber of connections(enter for default: " + DEFAULT_CONNECTION_NUM + " )");
                        break;
                    case 2:
                        String numInput;
                        int numInt = 0;
                        try {
                            numInt = Integer.valueOf(input);
                        } catch (NumberFormatException nfe) {
                            System.err.println("Number invalid");
                        }
                        if (!(numInput = input).equals("")) {
                            connectionNum = numInt;
                        } else {
                            connectionNum = DEFAULT_CONNECTION_NUM;
                        }
                        jtaDisplay.setText(jtaDisplay.getText() + "\nYou: " + input);
                        jtfInput.setText("");
                        jfrm.repaint();
                        runThread();
                        break;
                    default:
                        break;
                }
            }
            jtfInput.setText("");
            jfrm.repaint();
        }

        /**
         * Invokes handler for server communication
         *
         * @param e
         */
        @Override
        public void keyPressed(KeyEvent e) {

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
