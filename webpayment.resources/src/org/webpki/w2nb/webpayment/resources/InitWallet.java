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

package org.webpki.w2nb.webpayment.resources;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.cert.Certificate;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.Vector;

import org.webpki.crypto.AsymEncryptionAlgorithms;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyAlgorithms;
import org.webpki.crypto.KeyStoreReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.keygen2.KeyGen2URIs;
import org.webpki.sks.AppUsage;
import org.webpki.sks.BiometricProtection;
import org.webpki.sks.DeleteProtection;
import org.webpki.sks.EnumeratedKey;
import org.webpki.sks.EnumeratedProvisioningSession;
import org.webpki.sks.ExportProtection;
import org.webpki.sks.Grouping;
import org.webpki.sks.InputMethod;
import org.webpki.sks.PassphraseFormat;
import org.webpki.sks.PatternRestriction;
import org.webpki.sks.SecureKeyStore;
import org.webpki.sks.test.Device;
import org.webpki.sks.test.GenKey;
import org.webpki.sks.test.KeySpecifier;
import org.webpki.sks.test.PINPol;
import org.webpki.sks.test.ProvSess;
import org.webpki.sks.test.SKSReferenceImplementation;
import org.webpki.util.ArrayUtil;
import org.webpki.w2nb.webpayment.common.CredentialProperties;
import org.webpki.w2nb.webpayment.common.BaseProperties;

public class InitWallet {

    public static void main(String[] args) throws Exception {
        if (args.length < 7) {
            System.out.println("\nUsage: " +
                               InitWallet.class.getCanonicalName() +
                               "sksFile certFile certFilePassword cardPin cardType cardNumber image");
            System.exit(-3);
        }
        CustomCryptoProvider.forcedLoad(true);
        
        // Read key/certificate to be imported
        KeyStore ks = KeyStoreReader.loadKeyStore(args[1], args[2]);
        Vector<X509Certificate> cert_path = new Vector<X509Certificate>();
        PrivateKey private_key = null;
        Enumeration<String> aliases = ks.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (ks.isKeyEntry(alias)) {
                private_key = (PrivateKey) ks.getKey(alias, args[2].toCharArray());
                for (Certificate cert : ks.getCertificateChain(alias)) {
                    cert_path.add((X509Certificate) cert);
                }
                break;
            }
        }
        boolean rsa_flag = cert_path.firstElement().getPublicKey() instanceof RSAPublicKey;
        if (private_key == null) {
            throw new IOException("No private key!");
        }
        String[] endorsed_algs = rsa_flag ? new String[] {
                AsymSignatureAlgorithms.RSA_SHA256.getURI(),
                AsymSignatureAlgorithms.RSA_SHA512.getURI() } 
                                          : new String[] {
                AsymSignatureAlgorithms.ECDSA_SHA256.getURI(),
                AsymSignatureAlgorithms.ECDSA_SHA512.getURI() };

        // Setup keystore (SKS)
        SKSReferenceImplementation sks = null;
        try {
            sks = (SKSReferenceImplementation) new ObjectInputStream(new FileInputStream(args[0])).readObject();
            System.out.println("SKS found, restoring it");
        } catch (Exception e) {
            sks = new SKSReferenceImplementation();
            System.out.println("SKS not found, creating it");
        }
        Device device = new Device(sks);

        // Check for duplicates
        EnumeratedKey ek = new EnumeratedKey();
        while ((ek = sks.enumerateKeys(ek.getKeyHandle())) != null) {
            if (sks.getKeyAttributes(ek.getKeyHandle()).getCertificatePath()[0].equals(cert_path.get(0))) {
                throw new IOException("Duplicate entry - key #" + ek.getKeyHandle());
            }
        }

        // Start process by creating a session
        ProvSess sess = new ProvSess(device, 0);
        sess.setInputMethod(InputMethod.ANY);
        PINPol pin_policy = sess.createPINPolicy("PIN", 
                                                 PassphraseFormat.STRING,
                                                 EnumSet.noneOf(PatternRestriction.class),
                                                 Grouping.NONE,
                                                 1 /* min_length */,
                                                 50 /* max_length */,
                                                 (short) 3 /* retry_limit */,
                                                 null /* puk_policy */);

        GenKey key = sess.createKey("Key",
                                    SecureKeyStore.ALGORITHM_KEY_ATTEST_1,
                                    null /* server_seed */,
                                    pin_policy, 
                                    args[3] /* PIN value */,
                                    BiometricProtection.NONE /* biometric_protection */,
                                    ExportProtection.NON_EXPORTABLE /* export_policy */,
                                    DeleteProtection.NONE /* delete_policy */,
                                    false /* enable_pin_caching */,
                                    AppUsage.SIGNATURE,
                                    "" /* friendly_name */, 
                                    new KeySpecifier(KeyAlgorithms.NIST_P_256), endorsed_algs);

        key.setCertificatePath(cert_path.toArray(new X509Certificate[0]));
        key.setPrivateKey(private_key);
        JSONObjectWriter ow = new JSONObjectWriter();
        ow.setString(CredentialProperties.CARD_TYPE_JSON, args[4]);
        ow.setString(CredentialProperties.CARD_NUMBER_JSON, args[5]);
        key.addExtension(BaseProperties.W2NB_PAY_DEMO_CONTEXT_URI,
                SecureKeyStore.SUB_TYPE_EXTENSION,
                "",
                ow.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        key.addExtension(KeyGen2URIs.LOGOTYPES.CARD,
                         SecureKeyStore.SUB_TYPE_LOGOTYPE,
                         "image/png",
                         ArrayUtil.readFile(args[6]));
        sess.closeSession();
        
        // Serialize the updated SKS
        ObjectOutputStream oos = new ObjectOutputStream (new FileOutputStream(args[0]));
        oos.writeObject (sks);
        oos.close ();

        // Report
        System.out.println("Imported Subject: "
                + cert_path.firstElement().getSubjectX500Principal().getName()
                + "\nID=#" + key.key_handle + ", " + (rsa_flag ? "RSA" : "EC"));
    }
}