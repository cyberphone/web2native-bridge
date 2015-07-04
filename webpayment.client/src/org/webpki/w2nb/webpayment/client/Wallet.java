/*
 *  Copyright 2006-2015 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

// Simple Web2Native Bridge emulator application

package org.webpki.w2nb.webpayment.client;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Security;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.util.ArrayUtil;
import org.webpki.util.ISODateTime;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nbproxy.StdinJSONPipe;
import org.webpki.w2nbproxy.StdoutJSONPipe;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Wallet {
    static StdinJSONPipe stdin = new StdinJSONPipe();
    static StdoutJSONPipe stdout = new StdoutJSONPipe();
    static JDialog frame;

    static Logger logger = Logger.getLogger("MyLog");

    private static void initLogger(String logFile) {
        // This block configure the logger with handler and formatter
        try {
            FileHandler fh = new FileHandler(logFile);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    static class ApplicationFrame extends Thread {
        JTextArea textArea;
        JTextField sendText;
        Container cards;
        boolean running = true;
        Font standardFont;
        int fontSize;
        JPanel pane;

        ApplicationFrame() {
            cards = frame.getContentPane();
            fontSize = Toolkit.getDefaultToolkit().getScreenResolution() / 7;
            JLabel msgLabel = new JLabel("\u00a0Messages:\u00a0");
            Font font = msgLabel.getFont();
            if (font.getSize() > fontSize) {
                fontSize = font.getSize();
            }
            int stdInset = fontSize/3;
            standardFont = new Font(font.getFontName(), font.getStyle(), fontSize);

            // The initial card showing we are waiting
            cards.setLayout(new CardLayout());
            GridBagConstraints initCardConstraint = new GridBagConstraints();
            initCardConstraint.gridx = 0;
            initCardConstraint.gridy = 0;
            JPanel initCard = new JPanel();
            initCard.setLayout(new GridBagLayout());
            JLabel waitingIconHolder = getImageLabel("working128.gif", "working80.gif");
            initCard.add(waitingIconHolder, initCardConstraint);
            JLabel waitingText = new JLabel("Initializing - Please wait");
            waitingText.setFont(standardFont);
            initCardConstraint.gridy = 1;
            initCardConstraint.insets = new Insets(fontSize, 0, 0, 0);
            initCard.add(waitingText, initCardConstraint);
            cards.add(initCard,"PAY");

            // messages
            pane = new JPanel();
            cards.add(pane,"DO");
            pane.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.weightx = 0.0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            c.insets = new Insets(stdInset, stdInset, stdInset, stdInset);
            pane.add(msgLabel, c);

            textArea = new JTextArea();
            textArea.setRows(20);
            textArea.setFont(new Font("Courier", Font.PLAIN, fontSize));
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridwidth = 2;
            c.gridy = 1;
            c.insets = new Insets(0, stdInset, 0, stdInset);
            pane.add(scrollPane , c);

            JButton sendBut = new JButton("\u00a0\u00a0\u00a0Send\u00a0\u00a0\u00a0");
            sendBut.setFont(new Font(font.getFontName(), font.getStyle(), fontSize));
            sendBut.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    try {
                        stdout.writeJSONObject(new JSONObjectWriter().setString("native",
                                                                                sendText.getText()));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Writing", e);
                        System.exit(3);
                    }
                }
            });
            c.fill = GridBagConstraints.NONE;
            c.gridwidth = 1;
            c.gridy = 2;
            c.insets = new Insets(stdInset, stdInset, stdInset, 0);
            pane.add(sendBut, c);

            sendText = new JTextField(50);
            sendText.setFont(new Font("Courier", Font.PLAIN, fontSize));
            c.gridx = 1;
            c.insets = new Insets(stdInset, stdInset, stdInset, stdInset);
            pane.add(sendText, c);
        }
        
        JLabel getImageLabel(String big, String small) {
            try {
                return new JLabel(new ImageIcon(ArrayUtil.getByteArrayFromInputStream(
                        getClass().getResourceAsStream (fontSize > 20 ? big : small))));
            } catch (IOException e) {
                throw new RuntimeException (e);
            }
        }

        void showProblemDialog (boolean error, String message, final WindowAdapter windowAdapter) {
            final JDialog dialog = new JDialog(frame, error ? "Error" : "Warning", true);
            Container pane = dialog.getContentPane();
            pane.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(fontSize, fontSize * 2, fontSize, fontSize * 2);
            pane.add(error ? 
                 getImageLabel("error96.png", "error64.png")
                           :
                 getImageLabel("warning96.png", "warning64.png"), c);
            JLabel errorLabel = new JLabel(message);
            errorLabel.setFont(standardFont);
            c.anchor = GridBagConstraints.CENTER;
            c.insets = new Insets(0, fontSize * 2, 0, fontSize * 2);
            c.gridy = 1;
            pane.add(errorLabel, c);
            JButton okButton = new JButton("\u00a0\u00a0\u00a0OK\u00a0\u00a0\u00a0");
            okButton.setFont(standardFont);
            c.insets = new Insets(fontSize, fontSize * 2, fontSize, fontSize * 2);
            c.gridy = 2;
            pane.add(okButton, c);
            dialog.setResizable(false);
            dialog.pack();
            dialog.setAlwaysOnTop(true);
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            dialog.addWindowListener(windowAdapter);
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    dialog.setVisible(false);
                    windowAdapter.windowClosing(null);
                }
            });
            dialog.setVisible(true);
        }

        @Override
        public void run() {
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    running = false;
                    logger.log(Level.SEVERE, "Timeout!");
                    showProblemDialog(true, "Payment request timeout!", new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent event) {
                            System.exit(3);
                        }
                    });
                }
            }, 10000);
            try {
                JSONObjectReader or = stdin.readJSONObject();
                final String json = new String(or.serializeJSONObject(JSONOutputFormats.PRETTY_PRINT),"UTF-8");
                logger.info("Received:\n" + json);
                Messages.parseBaseMessage(Messages.INVOKE, or);
                or.getObject(BaseProperties.PAYMENT_REQUEST_JSON).getSignature();
                timer.cancel();
                if (running) {
                    // Swing is rather bad for multithreading...
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            ((CardLayout)cards.getLayout()).show(cards, "DO");
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    textArea.setText(json);
                                    textArea.setCaretPosition(0);
                                }
                            });
                        }
                    });
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Undecodable message:\n" + stdin.getJSONString(), e);
                showProblemDialog(true, "Undecodable message, see log file", new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent event) {
                        System.exit(3);
                    }
                });
            }
            // Catching the disconnect...
            try {
                stdin.readJSONObject();
            } catch (IOException e) {
                System.exit(3);
            }
        }
    }

    public static void main(String[] args) {
        initLogger(args[0]);
        for (int i = 0; i < args.length; i++) {
            logger.info("ARG[" + i + "]=" + args[i]);
        }
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        // Respond to caller to indicate that we are (almost) ready
        try {
            stdout.writeJSONObject(Messages.createBaseMessage(Messages.INITIALIZE));
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Writing", e);
            System.exit(3);
        }

        frame = new JDialog(new JFrame(), "Wallet [" + args[1] + "]");
        frame.setResizable(false);
        ApplicationFrame md = new ApplicationFrame();
        frame.pack();
        frame.setAlwaysOnTop(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                System.exit(0);
            }
        });
        frame.setVisible(true);
        md.start();
    }
}