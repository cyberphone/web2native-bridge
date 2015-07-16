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

// Web2Native Bridge emulator Payment Agent (a.k.a. Wallet) application

package org.webpki.w2nb.webpayment.client;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.security.Security;
import java.util.LinkedHashMap;
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
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.keygen2.KeyGen2URIs;
import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.Extension;
import org.webpki.sks.SKSException;
import org.webpki.sks.SecureKeyStore;
import org.webpki.sks.test.SKSReferenceImplementation;
import org.webpki.util.ArrayUtil;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.CredentialProperties;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nbproxy.StdinJSONPipe;
import org.webpki.w2nbproxy.StdoutJSONPipe;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class PaymentAgent {

    static StdinJSONPipe stdin = new StdinJSONPipe();
    static StdoutJSONPipe stdout = new StdoutJSONPipe();
    static JDialog frame;

    static Logger logger = Logger.getLogger("log");
    
    static final String TOOLTIP_CANCEL         = "Click if you want to abort this payment operation";
    static final String TOOLTIP_PAY_OK         = "Click if you agree to pay";
    static final String TOOLTIP_PAYEE          = "The party who requests payment";
    static final String TOOLTIP_AMOUNT         = "How much you are requested to pay";
    static final String TOOLTIP_PIN            = "PIN, if you are running the demo try 1234 :-)";
    static final String TOOLTIP_CARD_SELECTION = "Click on a card to select it!";
    static final String TOOLTIP_SELECTED_CARD  = "This card will be used in the transaction";
    
    static final String VIEW_INITIALIZING      = "INIT";
    static final String VIEW_SELECTION         = "SELECT";
    static final String VIEW_DUMMY_SELECTION   = "DUMMY";
    static final String VIEW_AUTHORIZE         = "AUTH";
    static final String VIEW_DEBUG             = "DEBUG";

    static final String BUTTON_OK              = "OK";
    static final String BUTTON_CANCEL          = "Cancel";
    static final String BUTTON_SEND            = "Send";

    static final int TIMEOUT_FOR_REQUEST       = 10000;
    
    static final String DUMMY_CARD_NUMBER      = "1234 1234 1234 1234 1234";

    static class Card {
        String cardNumber;
        ImageIcon cardIcon;
        
        Card(String cardNumber, ImageIcon cardIcon) {
            this.cardNumber = cardNumber;
            this.cardIcon = cardIcon;
        }
    }
    
    static LinkedHashMap<Integer,Card> cardSelection = new LinkedHashMap<Integer,Card>();

    static void initLogger(String logFile) {
        // This block configure the logger with handler and formatter
        try {
            FileHandler fh = new FileHandler(logFile);
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (Exception e) {
            terminate();
        }
    }

    static class ScalingIcon extends ImageIcon {
 
        private static final long serialVersionUID = 1L;

        public ScalingIcon(byte[] byteIcon) {
            super(byteIcon);
        }
       
        @Override
        public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
            Image image = getImage();
            int width = image.getWidth(c);
            int height = image.getHeight(c);
            final Graphics2D g2d = (Graphics2D)g.create(x, y, width, height);
            g2d.scale(0.5, 0.5);
            g2d.drawImage(image, 0, 0, c);
            g2d.scale(1, 1);
            g2d.dispose();
        }

        @Override
        public int getIconHeight() {
            return super.getIconHeight() / 2;
        }

        @Override
        public int getIconWidth() {
            return super.getIconWidth() / 2;
        }
    }

    static void terminate() {
        System.exit(3);
    }

    static class JButtonSlave extends JButton {
        
        private static final long serialVersionUID = 1L;

        JButton master;
        
        public JButtonSlave(String text, JButton master) {
            super(text);
            this.master = master;
        }
        
        @Override
        public Dimension getPreferredSize() {
            Dimension dimension = super.getPreferredSize();
            if (master != null) {
                return adjustSize(dimension, master.getPreferredSize());
            } else {
                return dimension;
            }
        }

        @Override
        public Dimension getMinimumSize() {
            Dimension dimension = super.getMinimumSize();
            if (master != null) {
                return adjustSize(dimension, master.getMinimumSize());
            } else {
                return dimension;
            }
        }

        @Override
        public Dimension getSize() {
            Dimension dimension = super.getSize();
            if (master != null) {
                return adjustSize(dimension, master.getSize());
            } else {
                return dimension;
            }
        }

        Dimension adjustSize(Dimension dimension, Dimension masterDimension) {
            if (masterDimension == null ||
                dimension == null ||
                dimension.width > masterDimension.width) {
                return dimension;
            } else {
                return masterDimension;
            }
        }
    }

    static class ApplicationWindow extends Thread {
        JTextArea debugText;
        JTextField sendText;
        Container views;
        boolean running = true;
        Font standardFont;
        Font cardNumberFont;
        int cardNumberSpacing;
        int fontSize;
        String invokeMessageString;
        JTextField amountField;
        JTextField payeeField;
        String amountString;
        String payeeString;
        JPasswordField pinText;
        JLabel selectedCardImage;
        JLabel selectedCardNumber;
        JButton cancelAuthorizationButton;  // Used as a master for creating unified button widths
        ImageIcon dummyCardIcon;
        boolean macOS;
        boolean retinaFlag;
        boolean hiResImages;
        SecureKeyStore sks;
 
        ApplicationWindow() {
            views = frame.getContentPane();
            views.setLayout(new CardLayout());
            int screenResolution = Toolkit.getDefaultToolkit().getScreenResolution();
            fontSize = screenResolution / 7;
            Font font = new JLabel("Dummy").getFont();
            macOS = System.getProperty("os.name").toLowerCase().contains("mac");
            if (font.getSize() > fontSize || macOS) {
                fontSize = font.getSize();
            }
            retinaFlag = isRetina ();
            hiResImages = retinaFlag || fontSize >= 20;
            standardFont = new Font(font.getFontName(), font.getStyle(), fontSize);
            cardNumberFont = new Font("Courier", 
                                      hiResImages ? Font.PLAIN : Font.BOLD,
                                      (fontSize * 4) / 5);
            cardNumberSpacing = fontSize / 6;
            logger.info("Display Data: Screen resolution=" + screenResolution +
                         ", Screen size=" + Toolkit.getDefaultToolkit().getScreenSize() +
                         ", Font size=" + font.getSize() +
                         ", Adjusted font size=" + fontSize +
                         ", Retina=" + retinaFlag);
            dummyCardIcon = getImageIcon("dummycard.png", false);

            // The initial card showing we are waiting
            initWaitingView();
 
            // The only thing we really care about, right?
            initAuthorizationView();

            // For measuring purposes only
            initCardSelectionView(false);

            // Debug messages
            initDebugView();
        }
        
        JPanel initCardSelectionViewCore(LinkedHashMap<Integer,Card> cards) {
            JPanel cardSelectionViewCore = new JPanel();
            cardSelectionViewCore.setBackground(Color.WHITE);
            cardSelectionViewCore.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            int itemNumber = 0;
            for (final Integer keyHandle : cards.keySet()) {
                Card card = cards.get(keyHandle);
                c.gridx = itemNumber % 2;
                c.gridy = (itemNumber / 2) * 2;
                c.insets = new Insets(c.gridy == 0 ? 0 : fontSize,
                                      c.gridx == 0 ? fontSize : 0,
                                      0,
                                      c.gridx == 0 ? 0 : fontSize);
                JButton cardImage = new JButton(card.cardIcon);
                cardImage.setPressedIcon(card.cardIcon);
                cardImage.setFocusPainted(false);
                cardImage.setMargin(new Insets(0, 0, 0, 0));
                cardImage.setContentAreaFilled(false);
                cardImage.setBorderPainted(false);
                cardImage.setOpaque(false);
                cardImage.setToolTipText(TOOLTIP_CARD_SELECTION);
                cardImage.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showAuthorizationView(keyHandle);
                    }
                });
                cardSelectionViewCore.add(cardImage, c);

                c.gridy++;
                c.insets = new Insets(cardNumberSpacing,
                                      c.gridx == 0 ? fontSize : 0,
                                      0,
                                      c.gridx == 0 ? 0 : fontSize);
                JLabel cardNumber = new JLabel(card.cardNumber, JLabel.CENTER);
                cardNumber.setFont(cardNumberFont);
                cardSelectionViewCore.add(cardNumber, c);
                itemNumber++;
            }
            return cardSelectionViewCore;
        }

        void initCardSelectionView(boolean actualCards) {
            JPanel cardSelectionView = new JPanel();
            cardSelectionView.setBackground(Color.WHITE);
            cardSelectionView.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();

            JLabel headerText = new JLabel("Select Card:");
            headerText.setFont(standardFont);
            c.insets = new Insets(fontSize, fontSize, fontSize, fontSize);
            c.anchor = GridBagConstraints.WEST;
            cardSelectionView.add(headerText, c);

            c.gridx = 0;
            c.gridy = 1;
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 1.0;
            c.weighty = 1.0; 
            c.insets = new Insets(0, 0, 0, 0);
            if (actualCards) {
                JScrollPane scrollPane = new JScrollPane(initCardSelectionViewCore(cardSelection));
                scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
                cardSelectionView.add(scrollPane, c);
            } else {
                LinkedHashMap<Integer,Card> cards = new LinkedHashMap<Integer,Card>();
                for (int i = 0; i < 2; i++) {
                    cards.put(i, new Card(DUMMY_CARD_NUMBER, dummyCardIcon));
                }
                cardSelectionView.add(initCardSelectionViewCore(cards), c);
            }

            JButtonSlave cancelButton = new JButtonSlave(BUTTON_CANCEL, cancelAuthorizationButton);
            cancelButton.setFont(standardFont);
            cancelButton.setToolTipText(TOOLTIP_CANCEL);
            cancelButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    terminate();
                }
            });
            c.gridx = 0;
            c.gridy = 2;
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0.0;
            c.weighty = 0.0; 
            c.insets = new Insets(fontSize, fontSize, fontSize, fontSize);
            cardSelectionView.add(cancelButton, c);
           
            views.add(cardSelectionView, actualCards ? VIEW_SELECTION : VIEW_DUMMY_SELECTION);
        }

        void showCardSelectionView() {
            initCardSelectionView(true);
            ((CardLayout)views.getLayout()).show(views, VIEW_SELECTION);
        }

        void initAuthorizationView() {
            JPanel authorizationView = new JPanel();
            authorizationView.setBackground(Color.WHITE);
            authorizationView.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            Color fixedDataBackground = new Color(244, 253, 247);
            int spaceAfterLabel = macOS ? fontSize / 4 : fontSize / 2;
            int maginBeforeLabel = fontSize;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 3;
            c.insets = new Insets((fontSize * 3) / 2, 0, 0, 0);
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.VERTICAL;
            c.weighty = 1.0;
            JLabel dummy1 = new JLabel(" ");
            dummy1.setFont(standardFont);
            authorizationView.add(dummy1, c);

            c.gridx = 0;
            c.gridy = 1;
            c.gridwidth = 1;
            c.insets = new Insets(0, maginBeforeLabel, 0, spaceAfterLabel);
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            c.weighty = 0.0;
            JLabel payeeLabel = new JLabel("Payee");
            payeeLabel.setFont(standardFont);
            authorizationView.add(payeeLabel, c);

            c.gridx = 1;
            c.gridy = 1;
            c.gridwidth = 2;
            c.insets = new Insets(0, 0, 0, fontSize * 2);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            payeeField = new JTextField();
            payeeField.setFont(standardFont);
            payeeField.setFocusable(false);
            payeeField.setBackground(fixedDataBackground);
            payeeField.setToolTipText(TOOLTIP_PAYEE);
            authorizationView.add(payeeField, c);

            c.gridx = 0;
            c.gridy = 2;
            c.gridwidth = 1;
            c.insets = new Insets(fontSize, maginBeforeLabel, (fontSize * 3) / 2, spaceAfterLabel);
            c.anchor = GridBagConstraints.EAST;
            c.fill = GridBagConstraints.NONE;
            JLabel amountLabel = new JLabel("Amount");
            amountLabel.setFont(standardFont);
            authorizationView.add(amountLabel, c);

            c.gridx = 1;
            c.gridy = 2;
            c.gridwidth = 1;
            c.insets = new Insets(fontSize, 0, (fontSize * 3) / 2, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.WEST;
            amountField = new JTextField();
            amountField.setFont(standardFont);
            amountField.setFocusable(false);
            amountField.setBackground(fixedDataBackground);
            amountField.setToolTipText(TOOLTIP_AMOUNT);
            authorizationView.add(amountField, c);

            c.gridx = 0;
            c.gridy = 3;
            c.gridwidth = 1;
            c.insets = new Insets(0, maginBeforeLabel, 0, spaceAfterLabel);
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.EAST;
            JLabel pinLabel = new JLabel("PIN");
            pinLabel.setFont(standardFont);
            authorizationView.add(pinLabel, c);

            c.gridx = 1;
            c.gridy = 3;
            c.gridwidth = 1;
            c.insets = new Insets(0, 0, 0, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            c.anchor = GridBagConstraints.CENTER;
            c.weightx = 1.0;
            pinText = new JPasswordField(8);
            pinText.setFont(standardFont);
            pinText.setToolTipText(TOOLTIP_PIN);
            authorizationView.add(pinText, c);
            c.weightx = 0.0;

            c.gridx = 0;
            c.gridy = 4;
            c.gridwidth = 3;
            c.insets = new Insets(0, 0, fontSize, 0);
            c.fill = GridBagConstraints.BOTH;
            c.weighty = 0.6;
            JLabel dummy2 = new JLabel(" ");
            dummy2.setFont(standardFont);
            authorizationView.add(dummy2, c);

            c.gridx = 0;
            c.gridy = 5;
            c.gridwidth = 1;
            c.insets = new Insets(0, fontSize, fontSize, 0);
            c.anchor = GridBagConstraints.SOUTHWEST;
            c.fill = GridBagConstraints.NONE;
            c.weighty = 0.0;
            cancelAuthorizationButton = new JButton("Cancel");
            cancelAuthorizationButton.setFont(standardFont);
            cancelAuthorizationButton.setToolTipText(TOOLTIP_CANCEL);
            authorizationView.add(cancelAuthorizationButton, c);
            cancelAuthorizationButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    terminate();
                }
            });

            c.gridx = 2;
            c.gridy = 5;
            c.gridwidth = 1;
            c.insets = new Insets(0, 0, fontSize, 0);
            c.anchor = GridBagConstraints.SOUTH;
            JButtonSlave okButton = new JButtonSlave(BUTTON_OK, cancelAuthorizationButton);
            okButton.setFont(standardFont);
            okButton.setToolTipText(TOOLTIP_PAY_OK);
            authorizationView.add(okButton, c);
            okButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (authorizationSucceeded()) {
                        ((CardLayout)views.getLayout()).show(views, VIEW_DEBUG);
                        debugText.setText(invokeMessageString);
                        debugText.setCaretPosition(0);
                    }
                }
            });

            c.gridx = 3;
            c.gridy = 0;
            c.gridheight = 6;
            c.gridwidth = 1;
            c.insets = new Insets(0, fontSize, 0, fontSize * 2);
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 1.0;
            JPanel cardAndNumber = new JPanel();
            cardAndNumber.setBackground(Color.WHITE);
            cardAndNumber.setLayout(new GridBagLayout());
            GridBagConstraints c2 = new GridBagConstraints();
            selectedCardImage = new JLabel(dummyCardIcon);
            selectedCardImage.setToolTipText(TOOLTIP_SELECTED_CARD);
            cardAndNumber.add(selectedCardImage, c2);
            selectedCardNumber = new JLabel(DUMMY_CARD_NUMBER);
            selectedCardNumber.setFont(cardNumberFont);
            c2.insets = new Insets(cardNumberSpacing, 0, 0, 0);
            c2.gridy = 1;
            cardAndNumber.add(selectedCardNumber, c2);
            authorizationView.add(cardAndNumber, c);

            views.add(authorizationView, VIEW_AUTHORIZE);
        }

         boolean authorizationSucceeded() {
            if (new String(pinText.getPassword()).equals("1234")) {
                return true;
            }
            showProblemDialog(false,
                              "<html>Incorrect PIN.<br>There are 2 tries left.</html>",
                              new WindowAdapter() {});
            return false;
        }

        void showAuthorizationView(int keyHandle) {
            logger.info("Selected Card=" + keyHandle);
            amountField.setText("\u200a" + amountString);
            payeeField.setText("\u200a" + payeeString);
            selectedCardImage.setIcon(cardSelection.get(keyHandle).cardIcon);
            selectedCardNumber.setText(cardSelection.get(keyHandle).cardNumber);
            ((CardLayout)views.getLayout()).show(views, VIEW_AUTHORIZE);
            payeeField.setCaretPosition(0);
            pinText.requestFocusInWindow();
        }

        void initDebugView() {
            JPanel debugView = new JPanel();
            debugView.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            int standardInset = fontSize/3;

            c.weightx = 0.0;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            c.insets = new Insets(standardInset, standardInset, standardInset, standardInset);
            JLabel msgLabel = new JLabel("Messages:");
            msgLabel.setFont(standardFont);
            debugView.add(msgLabel, c);

            debugText = new JTextArea();
            debugText.setFont(cardNumberFont);
            debugText.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(debugText);
            c.weightx = 1.0;
            c.weighty = 1.0;
            c.fill = GridBagConstraints.BOTH;
            c.gridwidth = 2;
            c.gridy = 1;
            c.insets = new Insets(0, standardInset, 0, standardInset);
            debugView.add(scrollPane , c);

            JButtonSlave sendBut = new JButtonSlave(BUTTON_SEND, cancelAuthorizationButton);
            sendBut.setFont(standardFont);
            sendBut.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    try {
                        JSONObjectWriter ow = Messages.createBaseMessage(Messages.AUTHORIZE);
                        stdout.writeJSONObject(ow.setString("native", sendText.getText()));
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, "Writing", e);
                        terminate();
                    }
                }
            });
            c.weightx = 0.0;
            c.weighty = 0.0;
            c.fill = GridBagConstraints.NONE;
            c.gridwidth = 1;
            c.gridy = 2;
            c.insets = new Insets(standardInset, standardInset, standardInset, 0);
            debugView.add(sendBut, c);

            sendText = new JTextField(40);
            sendText.setFont(new Font("Courier", Font.PLAIN, fontSize));
            c.fill = GridBagConstraints.HORIZONTAL;
            c.gridx = 1;
            c.insets = new Insets(standardInset, standardInset, standardInset, standardInset);
            debugView.add(sendText, c);

            views.add(debugView, VIEW_DEBUG);
        }

        void initWaitingView() {
            JPanel waitingView = new JPanel();
            waitingView.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            JLabel waitingIconHolder = getImageLabel("working.gif");
            waitingView.add(waitingIconHolder, c);

            JLabel waitingText = new JLabel("Initializing - Please wait");
            waitingText.setFont(standardFont);
            c.gridy = 1;
            c.insets = new Insets(fontSize, 0, 0, 0);
            waitingView.add(waitingText, c);

            views.add(waitingView, VIEW_INITIALIZING);
        }

        ImageIcon getImageIcon(byte[] byteIcon, boolean animated) {
            try {
                if (retinaFlag || (!hiResImages && animated)) {
                    return new ScalingIcon(byteIcon);
                }
                ImageIcon imageIcon = new ImageIcon(byteIcon);
                if (hiResImages) {
                    return imageIcon;
                }
                int width = imageIcon.getIconWidth() / 2;
                int height = imageIcon.getIconHeight() / 2;
                return new ImageIcon(imageIcon.getImage().getScaledInstance(
                               width == 0 ? 1 : width,
                               height == 0 ? 1 : height,
                               Image.SCALE_SMOOTH));
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed converting image", e);
                terminate();
                return null;
            }
        }

        ImageIcon getImageIcon(String name, boolean animated) {
            try {
                return getImageIcon(ArrayUtil.getByteArrayFromInputStream(
                        getClass().getResourceAsStream (name)), animated);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed reading image", e);
                terminate();
                return null;
            }
        }

        JLabel getImageLabel(String name) {
            return new JLabel(getImageIcon(name, name.endsWith(".gif")));
        }

        void showProblemDialog (boolean error, String message, final WindowAdapter windowAdapter) {
            final JDialog dialog = new JDialog(frame, error ? "Error" : "Warning", true);
            Container pane = dialog.getContentPane();
            pane.setLayout(new GridBagLayout());
            pane.setBackground(Color.WHITE);
            GridBagConstraints c = new GridBagConstraints();
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(fontSize, fontSize * 2, fontSize, fontSize * 2);
            pane.add(getImageLabel(error ? "error.png" : "warning.png"), c);
            JLabel errorLabel = new JLabel(message);
            errorLabel.setFont(standardFont);
            c.anchor = GridBagConstraints.CENTER;
            c.insets = new Insets(0, fontSize * 2, 0, fontSize * 2);
            c.gridy = 1;
            pane.add(errorLabel, c);
            JButtonSlave okButton = new JButtonSlave(BUTTON_OK, cancelAuthorizationButton);
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

        boolean isRetina() {
            if (macOS) {
                GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
                final GraphicsDevice device = env.getDefaultScreenDevice();
           
                try {
                    Field field = device.getClass().getDeclaredField("scale");
           
                    if (field != null) {
                        field.setAccessible(true);
                        Object scale = field.get(device);
           
                        if (scale instanceof Integer && ((Integer)scale).intValue() == 2) {
                            return true;
                        }
                    }
                } catch (Exception ignore) {}
            }
            return false;
        }
        
        @Override
        public void run() {
            try {
                sks = (SKSReferenceImplementation) new ObjectInputStream(getClass().getResourceAsStream("sks.serialized")).readObject();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Reading SKS failed");
                showProblemDialog(true, "Could not initiate payment credentials!", new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent event) {
                        terminate();
                    }
                });
            }
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (running) {
                        running = false;
                        logger.log(Level.SEVERE, "Timeout!");
                        showProblemDialog(true, "Payment request timeout!", new WindowAdapter() {
                            @Override
                            public void windowClosing(WindowEvent event) {
                                terminate();
                            }
                        });
                    }
                }
            }, TIMEOUT_FOR_REQUEST);
            try {
                JSONObjectReader or = stdin.readJSONObject();
                invokeMessageString = new String(or.serializeJSONObject(JSONOutputFormats.PRETTY_PRINT),"UTF-8");
                logger.info("Received:\n" + invokeMessageString);
                Messages.parseBaseMessage(Messages.INVOKE, or);
                or.getObject(BaseProperties.PAYMENT_REQUEST_JSON).getSignature();
                timer.cancel();
                if (running) {
                    // Swing is rather bad for multi-threading...
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            running = false;
                            amountString = "$\u200a3.25";
                            payeeString = "Demo Merchant long version";
                            EnumeratedKey ek = new EnumeratedKey();
                            try {
                                while ((ek = sks.enumerateKeys(ek.getKeyHandle())) != null) {
                                    try {
                                        Extension ext = sks.getExtension(ek.getKeyHandle(), BaseProperties.W2NB_PAY_DEMO_CONTEXT_URI);
                                        JSONObjectReader or = JSONParser.parse(ext.getExtensionData());
                                        cardSelection.put(ek.getKeyHandle(),
                                                new Card(or.getString(CredentialProperties.CARD_NUMBER_JSON),
                                                getImageIcon(sks.getExtension(ek.getKeyHandle(), 
                                                                              KeyGen2URIs.LOGOTYPES.CARD).getExtensionData(),
                                                             false)));
                                    } catch (SKSException e) {
                                        continue;
                                    }
                                }
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "SKS problem", e);
                                showProblemDialog(true, "Internal credentials error!", new WindowAdapter() {
                                    @Override
                                    public void windowClosing(WindowEvent event) {
                                        terminate();
                                    }
                                });
                            }
                            if (cardSelection.isEmpty()) {
                                logger.log(Level.SEVERE, "No matching card");
                                showProblemDialog(true, "No matching card!", new WindowAdapter() {
                                    @Override
                                    public void windowClosing(WindowEvent event) {
                                        terminate();
                                    }
                                });
                            } else if (cardSelection.size() == 1) {
                                showAuthorizationView(cardSelection.keySet().iterator().next());
                            } else {
                                showCardSelectionView();
                            }
                        }
                    });
                }
            } catch (IOException e) {
                if (running) {
                    running = false;
                    logger.log(Level.SEVERE, "Undecodable message:\n" + stdin.getJSONString(), e);
                    showProblemDialog(true, "Undecodable message, see log file!", new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent event) {
                            terminate();
                        }
                    });
                } else {
                    terminate();
                }
            }
            // Catching the disconnect...returns success to proxy
            try {
                stdin.readJSONObject();
            } catch (IOException e) {
                System.exit(0);
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
            logger.log(Level.SEVERE, "Error writing to browser", e);
            terminate();
        }

        frame = new JDialog(new JFrame(), "Payment Request [" + args[1] + "]");
        frame.setResizable(false);
        ApplicationWindow md = new ApplicationWindow();
        frame.pack();
        frame.setAlwaysOnTop(true);
        frame.setLocationRelativeTo(null);
        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                terminate();
            }
        });
        frame.setVisible(true);
        md.start();
    }
}