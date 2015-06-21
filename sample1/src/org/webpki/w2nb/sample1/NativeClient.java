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

package org.webpki.w2nb.sample1;

import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.DataInputStream;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

import org.webpki.util.ISODateTime;

class ApplicationFrame extends Thread {
	DataInputStream dis = new DataInputStream(System.in);
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
					byte[] utf8 = new JSONObjectWriter().setString("native",
							sendText.getText()).serializeJSONObject(JSONOutputFormats.NORMALIZED);
					update(new String(utf8, "UTF-8"));
					int l = utf8.length;
					byte[] blob = new byte[l + 4];
					blob[0] = (byte) l;
					blob[1] = (byte) (l >>> 8);
					blob[2] = (byte) (l >>> 16);
					blob[3] = (byte) (l >>> 24);
					for (int i = 0; i < l; i++) {
						blob[4 + i] = utf8[i];
					}
					System.out.write(blob);
				} catch (IOException e) {
					NativeClient.logger.log(Level.SEVERE, "Writing", e);
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
		textArea.setText(ISODateTime.formatDateTime(new Date(), false).substring(0, 19) + 
		    " " + text + "\n" + textArea.getText());
		NativeClient.logger.info(text);
	}

	@Override
	public void run() {
		while (true) {
			try {
				byte[] byteBuffer = new byte[4];
				dis.readFully(byteBuffer, 0, 4);
				int l = (byteBuffer[3]) << 24 | (byteBuffer[2] & 0xff) << 16
						| (byteBuffer[1] & 0xff) << 8 | (byteBuffer[0] & 0xff);
				if (l == 0)
					System.exit(3);
				byte[] string = new byte[l];
				dis.read(string);
				update(new String(string, "UTF-8"));
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
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		initLogger(args[0]);
		for (int i = 0; i < args.length; i++) {
			logger.info("ARG[" + i + "]=" + args[i]);
		}

		JDialog frame = new JDialog(new JFrame(), "W2NB - Sample #1 [" + args[1] + "]");
		frame.setResizable(false);
		ApplicationFrame md = new ApplicationFrame(frame.getContentPane());
		frame.pack();
		frame.setAlwaysOnTop(true);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
		md.start();
	}
}