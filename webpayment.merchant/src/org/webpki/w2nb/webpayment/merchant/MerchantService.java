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
package org.webpki.w2nb.webpayment.merchant;

import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import java.util.EnumSet;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONX509Verifier;

import org.webpki.util.ArrayUtil;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.CardTypes;
import org.webpki.w2nb.webpayment.common.Currencies;
import org.webpki.w2nb.webpayment.common.KeyStoreEnumerator;
import org.webpki.w2nb.webpayment.common.ServerSigner;

import org.webpki.webutil.InitPropertyReader;

public class MerchantService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(MerchantService.class.getCanonicalName());
    
    static Set<CardTypes> acceptedCards = EnumSet.noneOf(CardTypes.class);
  
    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String PAYMENT_ROOT          = "payment_root";
    
    static final String MERCHANT_EECERT       = "merchant_eecert";
    
    static final String ACQUIRER_HOST         = "acquirer_host";

    static final String BANK_PORT_MAP         = "bank_port_map";
    
    static final String CURRENCY              = "currency";

    static final String ADD_UNUSUAL_CARD      = "add_unusual_card";

    static final String ERR_MEDIA             = "err_media_type";
    
    static final String W2NB_NAME             = "w2nb_name";

    static JSONX509Verifier paymentRoot;
    
    static ServerSigner merchantKey;
    
    static Integer bankPortMapping;
    
    static Currencies currency;

    static String jsonMediaType = BaseProperties.JSON_CONTENT_TYPE;

    static Object w2nbName;
    
    static String acquirerHost;

    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
    }
    
    JSONX509Verifier getRoot(String name) throws IOException, GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load (null, null);
        keyStore.setCertificateEntry ("mykey",
                                      CertificateUtil.getCertificateFromBlob (
                                           ArrayUtil.getByteArrayFromInputStream (getResource(name))));        
        return new JSONX509Verifier(new KeyStoreVerifier(keyStore));
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }

    @Override
    public void contextInitialized(ServletContextEvent event) {
        initProperties (event);
        try {
            CustomCryptoProvider.forcedLoad (false);

            if (getPropertyString(BANK_PORT_MAP).length () > 0) {
                bankPortMapping = getPropertyInt(BANK_PORT_MAP);
            }

            merchantKey = new ServerSigner(new KeyStoreEnumerator(getResource(MERCHANT_EECERT),
                                                                  getPropertyString(KEYSTORE_PASSWORD)));

            paymentRoot = getRoot(PAYMENT_ROOT);

            for (CardTypes card : CardTypes.values()) {
                if (card != CardTypes.UnusualCard || getPropertyBoolean(ADD_UNUSUAL_CARD)) {
                    acceptedCards.add(card);
                }
            }
         
            currency = Currencies.valueOf(getPropertyString(CURRENCY));

            w2nbName = getPropertyString(W2NB_NAME);

            acquirerHost = getPropertyString(ACQUIRER_HOST);

            if (getPropertyBoolean(ERR_MEDIA)) {
                jsonMediaType = "text/html";
            }

            logger.info("Web2Native Bridge Merchant-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}
