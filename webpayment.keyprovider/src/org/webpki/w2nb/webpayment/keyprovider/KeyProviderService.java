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
package org.webpki.w2nb.webpayment.keyprovider;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyStoreVerifier;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONX509Verifier;
import org.webpki.keygen2.CredentialDiscoveryResponseDecoder;
import org.webpki.keygen2.InvocationResponseDecoder;
import org.webpki.keygen2.KeyCreationResponseDecoder;
import org.webpki.keygen2.ProvisioningFinalizationResponseDecoder;
import org.webpki.keygen2.ProvisioningInitializationResponseDecoder;
import org.webpki.util.ArrayUtil;
import org.webpki.w2nb.webpayment.common.Authority;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.DecryptionKeyHolder;
import org.webpki.w2nb.webpayment.common.Encryption;
import org.webpki.w2nb.webpayment.common.Expires;
import org.webpki.w2nb.webpayment.common.KeyStoreEnumerator;
import org.webpki.w2nb.webpayment.common.ServerSigner;
import org.webpki.webutil.InitPropertyReader;

public class KeyProviderService extends InitPropertyReader implements ServletContextListener {

    static Logger logger = Logger.getLogger(KeyProviderService.class.getCanonicalName());
    
    static final String KEYSTORE_PASSWORD     = "key_password";

    static final String BANK_HOST             = "bank_host";
    static final String DECRYPTION_KEY1       = "bank_decryptionkey1";
    static final String DECRYPTION_KEY2       = "bank_decryptionkey2";
    
    static final String SERVER_PORT_MAP       = "server_port_map";
    
    static Vector<DecryptionKeyHolder> decryptionKeys = new Vector<DecryptionKeyHolder>();
    
    static KeyStoreEnumerator keygen2KeyManagemenentKey;
    
    static String keygen2EnrollmentUrl;
    
    static Integer serverPortMapping;

    static JSONDecoderCache keygen2JSONCache;

    static String grantedVersions[];

    static class PaymentCredential {
        String accountType;
        String accountId;
        String authorityUrl;
        byte[] cardImage;
        KeyStoreEnumerator signatureKey;
        PublicKey encryptionKey;
    }

    static Vector<PaymentCredential> paymentCredentials = new Vector<PaymentCredential>();

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

            ////////////////////////////////////////////////////////////////////////////////////////////
            // KeyGen2
            ////////////////////////////////////////////////////////////////////////////////////////////
            keygen2JSONCache = new JSONDecoderCache ();
            keygen2JSONCache.addToCache (InvocationResponseDecoder.class);
            keygen2JSONCache.addToCache (ProvisioningInitializationResponseDecoder.class);
            keygen2JSONCache.addToCache (CredentialDiscoveryResponseDecoder.class);
            keygen2JSONCache.addToCache (KeyCreationResponseDecoder.class);
            keygen2JSONCache.addToCache (ProvisioningFinalizationResponseDecoder.class);
            
            if (getPropertyString(SERVER_PORT_MAP).length () > 0) {
                serverPortMapping = getPropertyInt(SERVER_PORT_MAP);
            }
            
            addDecryptionKey(DECRYPTION_KEY1);
            addDecryptionKey(DECRYPTION_KEY2);
            
            String bankHost = getPropertyString(BANK_HOST);
            publishedAuthorityData =
                Authority.encode(bankHost + "/authority",
                                 bankHost + "/transact",
                                 decryptionKeys.get(0).getPublicKey(),
                                 Expires.inDays(365),
                                 bankKey).serializeJSONObject(JSONOutputFormats.PRETTY_PRINT);

            logger.info("Web2Native Bridge KeyProvider-server initiated");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "********\n" + e.getMessage() + "\n********", e);
        }
    }

    static boolean isDebug() {
        return true;
    }
}
