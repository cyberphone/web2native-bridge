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

import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.security.Security;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.webpki.json.JSONObjectWriter;
import org.webpki.util.ISODateTime;
import org.webpki.w2nbproxy.StdinJSONPipe;
import org.webpki.w2nbproxy.StdoutJSONPipe;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

class ApplicationFrame extends Thread {
    StdinJSONPipe stdin = new StdinJSONPipe();
    StdoutJSONPipe stdout = new StdoutJSONPipe();
    JTextArea textArea;
    JTextField sendText;

    ApplicationFrame(Container pane) {
        int fontSize = Toolkit.getDefaultToolkit().getScreenResolution() / 7;
        JLabel msgLabel = new JLabel("\u00a0Messages:\u00a0");
        Font font = msgLabel.getFont();
        if (font.getSize() > fontSize) {
            fontSize = font.getSize();
        }
        pane.setLayout(new GridBagLayout());
        JPanel myPanel = new JPanel();
        textArea = new JTextArea(20, 50);
        textArea.setFont(new Font("Courier", Font.PLAIN, fontSize));
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        myPanel = new JPanel();
        msgLabel.setFont(new Font(font.getFontName(), font.getStyle(), fontSize));
        myPanel.add(msgLabel);
        myPanel.add(scrollPane);
        pane.add(myPanel);
        JPanel myPanel2 = new JPanel();
        sendText = new JTextField(50);
        sendText.setFont(new Font("Courier", Font.PLAIN, fontSize));
        myPanel2.add(sendText);
        JButton sendBut = new JButton("\u00a0\u00a0\u00a0Send\u00a0\u00a0\u00a0");
        sendBut.setFont(new Font(font.getFontName(), font.getStyle(), fontSize));
        sendBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    update(stdout.writeJSONObject(new JSONObjectWriter().setString("native",
                                                                                   sendText.getText())));
                } catch (IOException e) {
                    Wallet.logger.log(Level.SEVERE, "Writing", e);
                    System.exit(3);
                }
            }
        });
        myPanel2.add(sendBut);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        pane.add(myPanel2, c);
    }

    void update(String text) {
        String localTime = ISODateTime.formatDateTime(new Date(), false);
        textArea.setText(localTime.substring(0, 10) + " " + localTime.substring(11, 19) +
            " " + text + "\n" + textArea.getText());
        Wallet.logger.info(text);
    }

    @Override
    public void run() {
        while (true) {
            try {
                stdin.readJSONObject();  // Just syntax checking used in our crude sample
                update(stdin.getJSONString());
            } catch (IOException e) {
                Wallet.logger.log(Level.SEVERE, "Reading", e);
                System.exit(3);
            }
        }
    }
}

public class Wallet {
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

    public static void main(String[] args) {
        initLogger(args[0]);
        for (int i = 0; i < args.length; i++) {
            logger.info("ARG[" + i + "]=" + args[i]);
        }
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        JDialog frame = new JDialog(new JFrame(), "Wellet [" + args[1] + "]");
        frame.setResizable(false);
        ApplicationFrame md = new ApplicationFrame(frame.getContentPane());
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