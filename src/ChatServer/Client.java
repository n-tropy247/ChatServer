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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

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
 * Internet chat room client
 *
 * @author NTropy
 * @ver 2.0
 */
public class Client extends Thread {
    
    private static final int DEFAULT_PORT = 22333; 
    
    private static final String DEFAULT_CLIENTNAME = "Unknown"; 

    private static ActionEvent sendOverride;
    
    private static BufferedReader in; //inflow from server
    
    private static int clientNumber;
    private static int portNumber; 
    private static int sendCount = 0; 
    
    private static JButton jbtnSend; 
    
    private static JFrame jfrm;
    
    private static JTextArea jtaDisplay; 
    
    private static JScrollPane jscrlp; 
    
    private static JTextField jtfInput; 
    
    private static PrintWriter out; //outflow to server
    
    private static String clientName;
    private static String DEFAULT_HOSTNAME; 
    private static String hostName; 
    private static String input; 
    
    private static Socket echoSocket; 
    
    private static Thread print;

    /**
     * For instantiating threads
     */
    private Client() {
    }

    /**
     * Creates JFrame
     *
     * @param args
     * @throws IOException
     */
    public static void main(String args[]) throws IOException {
        try {
            InetAddress localhost = InetAddress.getLocalHost();
            DEFAULT_HOSTNAME = (localhost.getHostAddress()).trim();
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
        
        jfrm = new JFrame("Chatroom");
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
        jtaDisplay.setText("What is the IP? (hit enter for default: " + DEFAULT_HOSTNAME + " )"); //Ask Question
    }

    /**
     * Thread waits for server input then updates screen
     */
    @Override
    public void run() { //The code that the Thread runs
        try {
            while (true) {
                String output = in.readLine();
                jtaDisplay.setText(jtaDisplay.getText() + output + "\n");
                if (sendCount >= 3) {
                    jfrm.repaint();
                }
            }
        } catch (IOException iexe) {
            System.err.println("ERROR: IOEXCEPTION");
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
