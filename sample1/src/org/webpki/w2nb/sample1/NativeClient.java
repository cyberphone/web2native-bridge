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
package org.webpki.w2nb.sample1;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

import java.io.IOException;

import java.util.Date;

import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.SimpleFormatter;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.webpki.json.JSONObjectWriter;

import org.webpki.util.ISODateTime;

import org.webpki.w2nbproxy.BrowserWindow;
import org.webpki.w2nbproxy.StdinJSONPipe;
import org.webpki.w2nbproxy.StdoutJSONPipe;


//Simple Web2Native Bridge emulator application

class ApplicationFrame extends Thread {
    StdinJSONPipe stdin = new StdinJSONPipe();
    StdoutJSONPipe stdout = new StdoutJSONPipe();
    JTextArea textArea;
    JTextField sendText;

    ApplicationFrame(Container pane) {
        int fontSize = Toolkit.getDefaultToolkit().getScreenResolution() / 7;
        JLabel msgLabel = new JLabel("Messages:");
        Font font = msgLabel.getFont();
        boolean macOS = System.getProperty("os.name").toLowerCase().contains("mac");
        if (font.getSize() > fontSize || macOS) {
            fontSize = font.getSize();
        }
        int stdInset = fontSize/3;
        msgLabel.setFont(new Font(font.getFontName(), font.getStyle(), fontSize));
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
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
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
                    update(stdout.writeJSONObject(new JSONObjectWriter().setString("native",
                                                                                   sendText.getText())));
                } catch (IOException e) {
                    NativeClient.logger.log(Level.SEVERE, "Writing", e);
                    System.exit(3);
                }
            }
        });
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.gridwidth = 1;
        c.gridy = 2;
        c.insets = new Insets(stdInset, stdInset, stdInset, 0);
        pane.add(sendBut, c);

        sendText = new JTextField(50);
        sendText.setFont(new Font("Courier", Font.PLAIN, fontSize));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 1;
        c.insets = new Insets(stdInset, stdInset, stdInset, stdInset);
        pane.add(sendText, c);
    }

    void update(String text) {
        String localTime = ISODateTime.formatDateTime(new Date(), false);
        textArea.setText(localTime.substring(0, 10) + " " + localTime.substring(11, 19) +
            " " + text + "\n" + textArea.getText());
        NativeClient.logger.info(text);
    }

    @Override
    public void run() {
        while (true) {
            try {
                stdin.readJSONObject();  // Just syntax checking used in our crude sample
                update(stdin.getJSONString());
            } catch (IOException e) {
                NativeClient.logger.log(Level.SEVERE, "Reading", e);
                System.exit(3);
            }
        }
    }
}

public class NativeClient {
    static Logger logger = Logger.getLogger("MyLog");

    private static void initLogger(String logFile) {
        // This block configure the logger with handler and formatter
        try {
            FileHandler fh = new FileHandler(logFile);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (Exception e) {
            System.exit(3);
        }
    }

    public static void main(String[] args) {
        initLogger(args[0]);
        for (int i = 0; i < args.length; i++) {
            logger.info("ARG[" + i + "]=" + args[i]);
        }

        BrowserWindow browserWindow = null;
        try {
            browserWindow = new BrowserWindow(args[2]);
            logger.info("Browser window: " + browserWindow);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "nativeConnect argument errors", e);
        }
        JDialog frame = new JDialog(new JFrame(), "W2NB - Sample #1 [" + args[1] + "]");
        ApplicationFrame md = new ApplicationFrame(frame.getContentPane());
        frame.pack();

        // Put the extension window on top of the upper right of the calling (browser)window
        // The alignment varies a bit between platforms :-(
        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension extensionWindow = frame.getSize();
        double factor = screenDimension.height / browserWindow.screenHeight;
        double gutter = (browserWindow.outerWidth - browserWindow.innerWidth) / 2;
        double x = browserWindow.x + gutter;
        x += browserWindow.innerWidth - extensionWindow.width / factor;
        double y = browserWindow.y + browserWindow.outerHeight - browserWindow.innerHeight - gutter;
        frame.setLocation((int)(x * factor), (int)(y * factor));
        frame.setAlwaysOnTop(true);

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