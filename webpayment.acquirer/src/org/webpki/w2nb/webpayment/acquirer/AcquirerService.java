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
package org.webpki.w2nb.webpayment.acquirer;

import java.io.IOException;
import java.io.InputStream;

import java.security.GeneralSecurityException;
import java.security.KeyStore;

import java.security.interfaces.RSAPublicKey;

import java.util.Vector;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;

import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONX509Verifier;

import org.webpki.util.ArrayUtil;

import org.webpki.w2nb.webpayment.common.DecryptionKeyHolder;
import org.webpki.w2nb.webpayment.common.Encryption;
import org.webpki.w2nb.webpayment.common.KeyStoreEnumerator;
import org.webpki.w2nb.webpayment.common.ServerSigner;

import org.webpki.webutil.InitPropertyReader;

public class AcquirerService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(AcquirerService.class.getCanonicalName());
    
    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String ACQUIRER_EECERT       = "acquirer_eecert";
    static final String DECRYPTION_KEY1       = "acquirer_decryptionkey1";  // PUBLISHED
    static final String DECRYPTION_KEY2       = "acquirer_decryptionkey2";

    static final String MERCHANT_ROOT         = "merchant_root";
    static final String MERCHANT_DN           = "merchant_dn";  // The acquirer's only customer...

    static final String PAYMENT_ROOT          = "payment_root";
    
    static Vector<DecryptionKeyHolder> decryptionKeys = new Vector<DecryptionKeyHolder>();

    static JSONX509Verifier merchantRoot;

    static JSONX509Verifier paymentRoot;

    static ServerSigner acquirerKey;
    
    static String merchantDN;
    
    static byte[] publishedEncryptionKey;
    
    InputStream getResource(String name) throws IOException {
        return this.getClass().getResourceAsStream(getPropertyString(name));
    }
    
    void addDecryptionKey(String name) throws IOException {
        KeyStoreEnumerator keyStoreEnumerator = new KeyStoreEnumerator(getResource(name),
                                                                       getPropertyString(KEYSTORE_PASSWORD));
        decryptionKeys.add(new DecryptionKeyHolder(keyStoreEnumerator.getPublicKey(),
                                                   keyStoreEnumerator.getPrivateKey(),
                                                   keyStoreEnumerator.getPublicKey() instanceof RSAPublicKey ?
                Encryption.JOSE_RSA_OAEP_256_ALG_ID : Encryption.JOSE_ECDH_ES_ALG_ID));
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

            acquirerKey = new ServerSigner(new KeyStoreEnumerator(getResource(ACQUIRER_EECERT),
                                                                  getPropertyString(KEYSTORE_PASSWORD)));
            
            merchantRoot = getRoot(MERCHANT_ROOT);
            paymentRoot = getRoot(PAYMENT_ROOT);

            merchantDN = getPropertyString(MERCHANT_DN);

            addDecryptionKey(DECRYPTION_KEY1);
            addDecryptionKey(DECRYPTION_KEY2);

            publishedEncryptionKey =
                new JSONObjectWriter().setCertificatePath(
                    new KeyStoreEnumerator(getResource(DECRYPTION_KEY1),
                                           getPropertyString(KEYSTORE_PASSWORD)).getCertificatePath()
                                                         ).serializeJSONObject(JSONOutputFormats.PRETTY_PRINT);

            logger.info("Web2Native Bridge Acquirer-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }
}