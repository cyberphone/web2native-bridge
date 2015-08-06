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

import java.io.IOException;
import java.io.ObjectInputStream;

import java.lang.reflect.Field;

import java.net.URL;

import java.security.PublicKey;
import java.security.Security;

import java.security.cert.X509Certificate;

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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import javax.swing.border.EmptyBorder;

import javax.swing.plaf.metal.MetalButtonUI;

import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.SignerInterface;

import org.webpki.json.JSONAlgorithmPreferences;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.keygen2.KeyGen2URIs;

import org.webpki.net.HTTPSWrapper;

import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.Extension;
import org.webpki.sks.KeyProtectionInfo;
import org.webpki.sks.SKSException;
import org.webpki.sks.SecureKeyStore;

import org.webpki.sks.test.SKSReferenceImplementation;

import org.webpki.util.ArrayUtil;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.CredentialProperties;
import org.webpki.w2nb.webpayment.common.PayerPullAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.GenericAuthorizationRequest;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nb.webpayment.common.PaymentRequest;
import org.webpki.w2nb.webpayment.common.CryptoSupport;

import org.webpki.w2nbproxy.BrowserWindow;
import org.webpki.w2nbproxy.ExtensionPositioning;
import org.webpki.w2nbproxy.StdinJSONPipe;
import org.webpki.w2nbproxy.StdoutJSONPipe;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

//////////////////////////////////////////////////////////////////////////
// Web2Native Bridge emulator Payment Agent (a.k.a. Wallet) application //
//                                                                      //
// Note: This is not a product. It is an advanced prototype intended    //
// for evaluating a bunch of WebPKI.org technologies including:         //
//  - SKS (Secure Key Store)                                            //
//  - JCS (Java Clear-text Signature)                                   //
//  - Federation using credentials with embedded links                  //
// and (of course...), the Web2Native Bridge.                           //
//                                                                      //
//////////////////////////////////////////////////////////////////////////

public class PaymentAgent {

    static StdinJSONPipe stdin = new StdinJSONPipe();
    static StdoutJSONPipe stdout = new StdoutJSONPipe();
    static JDialog frame;

    static boolean testMode;

    static String domainName;

    static Logger logger = Logger.getLogger("log");
    
    static final String TOOLTIP_CANCEL         = "Click if you want to abort this payment operation";
    static final String TOOLTIP_PAY_OK         = "Click if you agree to pay";
    static final String TOOLTIP_PAYEE          = "The party who requests payment";
    static final String TOOLTIP_AMOUNT         = "How much you are requested to pay";
    static final String TOOLTIP_PIN            = "PIN, if you are running the demo try 1234 :-)";
    static final String TOOLTIP_CARD_SELECTION = "Click on a card to select it!";
    static final String TOOLTIP_SELECTED_CARD  = "This card will be used in the transaction";
    
    static final String VIEW_WAITING           = "WAIT";
    static final String VIEW_SELECTION         = "SELECT";
    static final String VIEW_DUMMY_SELECTION   = "DUMMY";
    static final String VIEW_AUTHORIZE         = "AUTH";

    static final String BUTTON_OK              = "OK";
    static final String BUTTON_CANCEL          = "Cancel";

    static final int TIMEOUT_FOR_REQUEST       = 10000;
    
    static final String DUMMY_CARD_NUMBER      = "12341234123412341234";

    static class Card {
        String cardNumber;
        ImageIcon cardIcon;
        String cardType;
        AsymSignatureAlgorithms signatureAlgorithm;
        String authUrl;
        
        // Optional (as a triple)
        String contentEncryptionAlgorithm;
        String keyEncryptionAlgorithm;
        PublicKey keyEncryptionKey;
        
        Card(String cardNumber, 
            ImageIcon cardIcon,
            String cardType,
            AsymSignatureAlgorithms signatureAlgorithm,
            String authUrl) {
            this.cardNumber = cardNumber;
            this.cardIcon = cardIcon;
            this.cardType = cardType;
            this.signatureAlgorithm = signatureAlgorithm;
            this.authUrl = authUrl;
        }
    }

    static LinkedHashMap<Integer,Card> cardCollection = new LinkedHashMap<Integer,Card>();

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
        Container views;
        JLabel waitingText;
        boolean running = true;
        Font standardFont;
        Font cardNumberFont;
        int fontSize;
        JTextField amountField;
        JTextField payeeField;
        String amountString;
        String payeeString;
        JPasswordField pinText;
        JButton selectedCardImage;
        JLabel selectedCardNumber;
        JButton authorizationCancelButton;  // Used as a master for creating unified button widths
        ImageIcon dummyCardIcon;
        boolean macOS;
        boolean retinaFlag;
        boolean hiResImages;

        SecureKeyStore sks;
        int keyHandle;
        Card selectedCard;
 
        PaymentRequest paymentRequest;
        
        JSONObjectWriter resultMessage;
        
        boolean pullPayment;
        
        ApplicationWindow() {
            // First we measure all the panes to be used to get the size of the holding window
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
        }

        JButton createCardButton (ImageIcon cardIcon, String toolTip) {
            JButton cardButton = new JButton(cardIcon);
            cardButton.setUI(new MetalButtonUI());
            cardButton.setPressedIcon(cardIcon);
            cardButton.setFocusPainted(false);
            cardButton.setMargin(new Insets(0, 0, 0, 0));
            cardButton.setContentAreaFilled(false);
            cardButton.setBorderPainted(false);
            cardButton.setOpaque(false);
            cardButton.setToolTipText(toolTip);
            return cardButton;
        }

        void insertSpacer(JPanel cardSelectionViewCore, int x, int y) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = x;
            c.gridy = y;
            c.gridheight = 2;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1.0;
            cardSelectionViewCore.add(new JLabel(), c);
        }

        JPanel initCardSelectionViewCore(LinkedHashMap<Integer,Card> cards) {
            JPanel cardSelectionViewCore = new JPanel();
            cardSelectionViewCore.setBackground(Color.WHITE);
            cardSelectionViewCore.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            int itemNumber = 0;
            for (final Integer keyHandle : cards.keySet()) {
                Card card = cards.get(keyHandle);
                boolean even = itemNumber % 2 == 0;
                c.gridx = even ? 1 : 3;
                c.gridy = (itemNumber / 2) * 2;
                insertSpacer(cardSelectionViewCore, c.gridx - 1, c.gridy);
                c.insets = new Insets(c.gridy == 0 ? 0 : fontSize,
                                      0,
                                      0,
                                      0);
                JButton cardImage = createCardButton(card.cardIcon, TOOLTIP_CARD_SELECTION);
                cardImage.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showAuthorizationView(keyHandle);
                    }
                });
                cardSelectionViewCore.add(cardImage, c);

                c.gridy++;
                c.insets = new Insets(0,
                                      0,
                                      0,
                                      0);
                JLabel cardNumber = new JLabel(GenericAuthorizationRequest.formatCardNumber(card.cardNumber),
                                               JLabel.CENTER);
                cardNumber.setFont(cardNumberFont);
                cardSelectionViewCore.add(cardNumber, c);
                if (!even) {
                    insertSpacer(cardSelectionViewCore, c.gridx + 1, c.gridy - 1);
                }
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
            c.insets = new Insets(fontSize / 2, fontSize, fontSize, fontSize);
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
                JScrollPane scrollPane = new JScrollPane(initCardSelectionViewCore(cardCollection));
                scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
                cardSelectionView.add(scrollPane, c);
            } else {
                LinkedHashMap<Integer,Card> cards = new LinkedHashMap<Integer,Card>();
                for (int i = 0; i < 2; i++) {
                    cards.put(i, new Card(DUMMY_CARD_NUMBER, dummyCardIcon, null, null, null));
                }
                cardSelectionView.add(initCardSelectionViewCore(cards), c);
            }

            JButtonSlave cancelButton = new JButtonSlave(BUTTON_CANCEL, authorizationCancelButton);
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
            // Mastering "GridBagLayout"? Not really...  
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
            c.insets = new Insets(fontSize, 0, 0, 0);
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
            c.insets = new Insets(0, 0, fontSize / 2, 0);
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
            authorizationCancelButton = new JButton(BUTTON_CANCEL);
            authorizationCancelButton.setFont(standardFont);
            authorizationCancelButton.setToolTipText(TOOLTIP_CANCEL);
            authorizationView.add(authorizationCancelButton, c);
            authorizationCancelButton.addActionListener(new ActionListener() {
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
            JButtonSlave authorizationOkButton = new JButtonSlave(BUTTON_OK, authorizationCancelButton);
            authorizationOkButton.setFont(standardFont);
            authorizationOkButton.setToolTipText(TOOLTIP_PAY_OK);
            authorizationView.add(authorizationOkButton, c);
            authorizationOkButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (userAuthorizationSucceeded()) {

                        // The user have done his/her part, now it is up to the rest of
                        // the infrastructure carry out the user's request.  This may take
                        // a few seconds so we put up the "Waiting" sign again.
                        waitingText.setText("Payment processing - Please wait");
                        ((CardLayout)views.getLayout()).show(views, VIEW_WAITING);

                        // This is a multi-threaded application, yes!
                        new PerformPayment().start();
                    }
                }
            });

            c.gridx = 3;
            c.gridy = 0;
            c.gridheight = 6;
            c.gridwidth = 1;
            c.insets = new Insets(0, 0, 0, fontSize);
            c.anchor = GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.BOTH;
            c.weightx = 0.0;
            c.weighty = 1.0;
            JPanel cardAndNumber = new JPanel();
            cardAndNumber.setBackground(Color.WHITE);
            cardAndNumber.setLayout(new GridBagLayout());
            GridBagConstraints c2 = new GridBagConstraints();
            selectedCardImage = createCardButton(dummyCardIcon, TOOLTIP_SELECTED_CARD);
            cardAndNumber.add(selectedCardImage, c2);
            selectedCardNumber = new JLabel(DUMMY_CARD_NUMBER);
            selectedCardNumber.setFont(cardNumberFont);
            c2.insets = new Insets(0, 0, 0, 0);
            c2.gridy = 1;
            cardAndNumber.add(selectedCardNumber, c2);
            authorizationView.add(cardAndNumber, c);

            views.add(authorizationView, VIEW_AUTHORIZE);
        }

        void showAuthorizationView(int keyHandle) {
            selectedCard = cardCollection.get(keyHandle);
            logger.info("Selected Card: Key=" + keyHandle +
                        ", Number=" + selectedCard.cardNumber +
                        ", URL=" + selectedCard.authUrl +
                        ", KeyEncryptionKey=" + (selectedCard.keyEncryptionAlgorithm == null ?
                           "N/A" : selectedCard.keyEncryptionKey));
            this.keyHandle = keyHandle;
            amountField.setText("\u200a" + amountString);
            payeeField.setText("\u200a" + payeeString);
            selectedCardImage.setIcon(selectedCard.cardIcon);
            selectedCardImage.setPressedIcon(selectedCard.cardIcon);
            selectedCardNumber.setText(GenericAuthorizationRequest.formatCardNumber(selectedCard.cardNumber));
            ((CardLayout)views.getLayout()).show(views, VIEW_AUTHORIZE);
            payeeField.setCaretPosition(0);
            pinText.requestFocusInWindow();
            try {
                pinBlockCheck();
            } catch (Exception e) {
                sksProblem(e);
            }
        }

        void initWaitingView() {
            JPanel waitingView = new JPanel();
            waitingView.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = 0;
            JLabel waitingIconHolder = getImageLabel("working.gif");
            waitingView.add(waitingIconHolder, c);

            waitingText = new JLabel("Initializing - Please wait");
            waitingText.setFont(standardFont);
            c.gridy = 1;
            c.insets = new Insets(fontSize, 0, 0, 0);
            waitingView.add(waitingText, c);

            views.add(waitingView, VIEW_WAITING);
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
            JButtonSlave okButton = new JButtonSlave(BUTTON_OK, authorizationCancelButton);
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

        void terminatingError(String error) {
            showProblemDialog(true, error, new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    terminate();
                }
            });
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

        void sksProblem(Exception e) {
            logger.log(Level.SEVERE, "SKS problem", e);
            terminatingError("<html>*** Internal Error ***<br>Check log file for details.</html>");
        }
        
        @Override
        public void run() {
            try {
                sks = (SKSReferenceImplementation) new ObjectInputStream(getClass()
                    .getResourceAsStream("sks.serialized")).readObject();
            } catch (Exception e) {
                sksProblem(e);
            }
            final Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (running) {
                        running = false;
                        logger.log(Level.SEVERE, "Timeout!");
                        terminatingError("Payment request timeout!");
                    }
                }
            }, TIMEOUT_FOR_REQUEST);
            try {
                JSONObjectReader invokeMessage = stdin.readJSONObject();
                logger.info("Received:\n" + invokeMessage);
                Messages.parseBaseMessage(Messages.INVOKE_WALLET, invokeMessage);
                final String[] cardTypes = invokeMessage.getStringArray(BaseProperties.ACCEPTED_CARD_TYPES_JSON);
                pullPayment = invokeMessage.getBoolean(BaseProperties.PULL_PAYMENT_JSON);
                paymentRequest = new PaymentRequest(invokeMessage.getObject(BaseProperties.PAYMENT_REQUEST_JSON));
                timer.cancel();
                if (running) {
                    // Swing is rather poor for multi-threading...
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            running = false;
                            try {
                                // Primary information to the user...
                                amountString = paymentRequest.getCurrency()
                                    .convertAmountToString(paymentRequest.getAmount());
                                payeeString = paymentRequest.getPayee();

                                // Enumerate keys but only go for those who are intended for
                                // Web Payments (according to our fictitious payment schema...)
                                EnumeratedKey ek = new EnumeratedKey();
                                while ((ek = sks.enumerateKeys(ek.getKeyHandle())) != null) {
                                    Extension ext = null;
                                    try {
                                        ext = sks.getExtension(ek.getKeyHandle(),
                                                               BaseProperties.W2NB_WEB_PAY_CONTEXT_URI);
                                    } catch (SKSException e) {
                                        if (e.getError() == SKSException.ERROR_OPTION) {
                                            continue;
                                        }
                                        throw new Exception(e);
                                    }

                                    // This key had the attribute signifying that it is a payment credential
                                    // for the fictitious payment scheme this system is supporting but it
                                    // might still not match the Payee's list of supported sub-types (brands).
                                    collectPotentialCard(ek.getKeyHandle(),
                                                         JSONParser.parse(ext.getExtensionData(SecureKeyStore.SUB_TYPE_EXTENSION)),
                                                         cardTypes);
                               }
                            } catch (Exception e) {
                                sksProblem(e);
                            }
                            if (cardCollection.isEmpty()) {
                                logger.log(Level.SEVERE, "No matching card");
                                terminatingError("No matching card!");
                            } else if (cardCollection.size() == 1) {
                                showAuthorizationView(cardCollection.keySet().iterator().next());
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
                    terminatingError("Undecodable message, see log file!");
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

        void collectPotentialCard(int keyHandle,
                                  JSONObjectReader cardProperties,
                                  String[] cardTypes) throws IOException {
            for (String cardType : cardTypes) {
                if (cardProperties.getString(CredentialProperties.CARD_TYPE_JSON).equals(cardType)) {
                    Card card = new Card(cardProperties.getString(CredentialProperties.CARD_NUMBER_JSON),
                            getImageIcon(sks.getExtension(keyHandle, 
                                  KeyGen2URIs.LOGOTYPES.CARD).getExtensionData(SecureKeyStore.SUB_TYPE_LOGOTYPE),
                                  false),
                            cardType,
                            AsymSignatureAlgorithms.getAlgorithmFromID(
                                JSONSignatureDecoder.algorithmCheck(
                                    cardProperties.getString(CredentialProperties.SIGNATURE_ALGORITHM_JSON),
                                    JSONAlgorithmPreferences.JOSE)),
                            cardProperties.getString(CredentialProperties.AUTH_URL_JSON));
                    if (cardProperties.hasProperty(CredentialProperties.KEY_ENCRYPTION_KEY_JSON)) {
                        card.keyEncryptionAlgorithm =
                                cardProperties.getString(CredentialProperties.KEY_ENCRYPTION_ALGORITHM_JSON);
                        if (!CryptoSupport.permittedKeyEncryptionAlgorithm(card.keyEncryptionAlgorithm)) {
                            logger.warning("Card " + card.cardNumber + " contained an unknown \"" +
                                           CredentialProperties.KEY_ENCRYPTION_ALGORITHM_JSON + "\": " +
                                           card.keyEncryptionAlgorithm);
                            break;
                        }
                        card.keyEncryptionKey = cardProperties.getObject(CredentialProperties.KEY_ENCRYPTION_KEY_JSON)
                                                      .getPublicKey(JSONAlgorithmPreferences.JOSE);
                        card.contentEncryptionAlgorithm =
                                cardProperties.getString(CredentialProperties.CONTENT_ENCRYPTION_ALGORITHM_JSON);
                        if (!CryptoSupport.permittedContentEncryptionAlgorithm(card.contentEncryptionAlgorithm)) {
                            logger.warning("Card " + card.cardNumber + " contained an unknown \"" +
                                           CredentialProperties.CONTENT_ENCRYPTION_ALGORITHM_JSON + "\": " +
                                           card.contentEncryptionAlgorithm);
                            break;
                        }
                    } else if (pullPayment) {
                        logger.warning("Card " + card.cardNumber + " doesn't support \"pull\" payments");
                        break;
                    }
                    cardCollection.put(keyHandle, card);
                    break;
                }
            }
        }

        boolean pinBlockCheck() throws SKSException {
            if (sks.getKeyProtectionInfo(keyHandle).isPinBlocked()) {
                terminatingError("Card blocked due to previous PIN errors!");
                return true;
            }
            return false;
        }

        boolean userAuthorizationSucceeded() {
            try {
                if (pinBlockCheck()) {
                    return false;
                }
                try {
                    // User authorizations are always signed by a key that only needs to be
                    // understood by the issuing Payment Provider (bank).  Tokenization and
                    // user anonymization is also performed by the Payment Provider.
                    resultMessage = GenericAuthorizationRequest.encode(
                        paymentRequest,
                        domainName,
                        selectedCard.cardType,
                        selectedCard.cardNumber,
                        selectedCard.signatureAlgorithm,
                        new SignerInterface () {
                            @Override
                            public X509Certificate[] getCertificatePath() throws IOException {
                                return sks.getKeyAttributes(keyHandle).getCertificatePath();
                            }
                            @Override
                            public byte[] signData(byte[] data, AsymSignatureAlgorithms algorithm) throws IOException {
                                return sks.signHashedData(keyHandle,
                                                          algorithm.getURI(),
                                                          null,
                                                          new String(pinText.getPassword()).getBytes("UTF-8"),
                                                          algorithm.getDigestAlgorithm().digest(data));
                            }
                        });
                    if (pullPayment) {
                        logger.info("Authorization before \"pull\" encryption:\n" + resultMessage);

                        // "Pull" payments must be encrypted in order to not leak user information to Payees.
                        // Only the proper Payment Provider can decrypt and process "pull" user authorizations.
                        resultMessage = PayerPullAuthorizationRequest.encode(resultMessage,
                                                                             selectedCard.authUrl,
                                                                             selectedCard.contentEncryptionAlgorithm,
                                                                             selectedCard.keyEncryptionKey,
                                                                             selectedCard.keyEncryptionAlgorithm);
                    }
                    logger.info((pullPayment || testMode ?
                                     "About to send to the browser:\n" 
                                                         :
                                     "About to send to \"" + selectedCard.authUrl + "\":\n")
                                + resultMessage);
                    return true;
                } catch (SKSException e) {
                    if (e.getError() != SKSException.ERROR_AUTHORIZATION) {
                        throw new Exception(e);
                    }
                }
                if (!pinBlockCheck()) {
                    logger.warning("Incorrect PIN");
                    KeyProtectionInfo pi = sks.getKeyProtectionInfo(keyHandle);
                    showProblemDialog(false,
                            "<html>Incorrect PIN.<br>There are " +
                             (pi.getPinRetryLimit() - pi.getPinErrorCount()) +
                             " tries left.</html>",
                            new WindowAdapter() {});
                }
                return false;
            } catch (Exception e) {
                sksProblem(e);
                return false;  
            }
        }

        class PerformPayment extends Thread {
            @Override
            public void run() {
                try {
                    if (!testMode && !pullPayment) {
                        // In the "push" payment model the Wallet sends the user-authorized request
                        // to the Payment Provider (bank) for final authorization and funds checking.
                        // The resulting message is what is finally handed over to the Merchant (Payee).
                        // The URL to the Payment Provider is a part of the user's payment credential (card).
                        HTTPSWrapper httpClient = new HTTPSWrapper();
                        httpClient.setTimeout(TIMEOUT_FOR_REQUEST);
                        httpClient.setHeader("Content-Type", BaseProperties.JSON_CONTENT_TYPE);
                        httpClient.setRequireSuccess(true);
                        httpClient.makePostRequest(selectedCard.authUrl,
                                                   resultMessage.serializeJSONObject(JSONOutputFormats.NORMALIZED));
                        String mimeType = httpClient.getContentType();
                        if (!mimeType.equals(BaseProperties.JSON_CONTENT_TYPE)) {
                            throw new IOException("Improper media type: " + mimeType);
                        }
                        resultMessage = new JSONObjectWriter(JSONParser.parse(httpClient.getData()));
                        logger.info("Returned from payment provider for handover to payee via the browser:\n"
                                    + resultMessage);
                    }

                    // In both the "push" and "pull" modes the Wallet finishes by sending the specific
                    // authorization message through the Web2Native Bridge interface to the invoking
                    // Merchant (Payee) web page which in turn posts it to the Merchant server.  The
                    // latter should return a new web-page which causes the Wallet to unload (disappear).
                    // Any further communication with the user (including "insufficient funds") is
                    // supposed to be performed using standard web technology.
                    stdout.writeJSONObject(resultMessage);

                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Communication error", e);
                    terminatingError("<html>*** Communication Error ***<br>Check log file for details.</html>");
                }
            }
        }
    }

    public static void main(String[] args) {
        initLogger(args[0]);
        for (int i = 0; i < args.length; i++) {
            logger.info("ARG[" + i + "]=" + args[i]);
        }
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        
        BrowserWindow browserWindow = null;
        ExtensionPositioning extensionPositioning = null;
        try {
            browserWindow = new BrowserWindow(args[2]);
            extensionPositioning = new ExtensionPositioning(args[3]);
            logger.info("Browser window: " + browserWindow);
            logger.info("Positioning arguments: " + extensionPositioning);
            if (args[1].startsWith("http")) {
                domainName = new URL(args[1]).getHost();
            } else {
                testMode = true;
                domainName = args[1];
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "nativeConnect argument errors", e);
            terminate();
        }

        frame = new JDialog(new JFrame(), "Payment Request [" + domainName + "]");
        ApplicationWindow md = new ApplicationWindow();
        frame.setResizable(false);
        frame.pack();

        ////////////////////////////////////////////////////////////////
        // Positioning: Calculate coordinates in Browser resolution
        ////////////////////////////////////////////////////////////////

        // Note that Swing returns native precision
        Dimension extensionWindow = frame.getSize();
        logger.info("Frame=" + extensionWindow);
        Dimension screenDimension = Toolkit.getDefaultToolkit().getScreenSize();

        // We need to know the difference between Browser/Native precision
        double factor = screenDimension.height / browserWindow.screenHeight;

        // Browser border size
        double gutter = (browserWindow.outerWidth - browserWindow.innerWidth) / 2;

        // The browser window's position (modulo fixed "chrome") on the screen
        double left = browserWindow.x + gutter;
        double top = browserWindow.y + browserWindow.outerHeight - browserWindow.innerHeight - gutter;
        double width = browserWindow.innerWidth;
        double height = browserWindow.innerHeight;

        // We may rather be targeting a specific HTML element on the invoking page
        if (extensionPositioning.targetRectangle != null) {
            left += extensionPositioning.targetRectangle.left;
            top += extensionPositioning.targetRectangle.top;
            width = extensionPositioning.targetRectangle.width;
            height = extensionPositioning.targetRectangle.height;
        }

        // Now, position it!
        // 
        double extWidth = extensionWindow.getWidth() / factor;
        if (extensionPositioning.horizontalAlignment == ExtensionPositioning.HORIZONTAL_ALIGNMENT.Center) {
            left += (width - extWidth) / 2;
        } else if (extensionPositioning.horizontalAlignment == ExtensionPositioning.HORIZONTAL_ALIGNMENT.Right) {
            left += width - extWidth;
        }
        double extHeight = extensionWindow.getHeight() / factor; 
        if (extensionPositioning.verticalAlignment == ExtensionPositioning.VERTICAL_ALIGNMENT.Center) {
            top += (height - extHeight) / 2;
        } else if (extensionPositioning.verticalAlignment == ExtensionPositioning.VERTICAL_ALIGNMENT.Bottom) {
            top += height - extHeight;
        }
        frame.setLocation((int)(left * factor), (int)(top * factor));

        // Respond to caller to indicate that we are (almost) ready for action.
        // We provide the new width and height data which can be used by the
        // calling Web application to update the page for the Wallet to make
        // it more look like a Web application.  Note that this measurement
        // lacks the 'px' part; you have to add it in the Web application.
        try {
            stdout.writeJSONObject(Messages.createBaseMessage(Messages.WALLET_IS_READY)
                .setDouble(BaseProperties.TARGET_WIDTH_JSON, extWidth)
                .setDouble(BaseProperties.TARGET_HEIGHT_JSON, extHeight));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error writing to browser", e);
            terminate();
        }

        frame.setAlwaysOnTop(true);

        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                terminate();
            }
        });

        // Show the "Waiting" window
        frame.setVisible(true);

        // Start reading and processing the payment request that should be waiting for us at this stage.
        md.start();
    }
}
