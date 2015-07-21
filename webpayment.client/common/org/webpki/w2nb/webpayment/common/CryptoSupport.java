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
package org.webpki.w2nb.webpayment.common;

import java.io.IOException;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

import java.security.interfaces.ECPublicKey;

import java.security.spec.ECGenParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;

import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.KeyAlgorithms;

public abstract class CryptoSupport {
    
    public static byte[] contentEncryption(boolean encrypt, byte[] aesKey, byte[] data, byte[] iv, byte[] tag) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding", "BC");
        cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE,
                    new SecretKeySpec(aesKey, "AES"),
                    new IvParameterSpec(iv));
        return cipher.doFinal(data);
    }

    public static byte[] rsaEncryptKey(byte[] rawKey, PublicKey publicKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(rawKey);
    }

    public static byte[] rsaDecryptKey(byte[] encryptedKey, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedKey);
    }

    public static byte[] serverKeyAgreement(ECPublicKey receivedPublicKey, PrivateKey privateKey) throws GeneralSecurityException, IOException {
        KeyAgreement keyAgreement = KeyAgreement.getInstance("ECDH", "BC");
        keyAgreement.init(privateKey);
        keyAgreement.doPhase(receivedPublicKey, true);
        byte[] Z = keyAgreement.generateSecret();
        return HashAlgorithms.SHA256.digest(Z);
    }

    public static byte[] clientKeyAgreement(ECPublicKey[] generatedEphemeralKey, PublicKey staticKey) throws IOException, GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "BC");
        ECGenParameterSpec eccgen = new ECGenParameterSpec(KeyAlgorithms.getKeyAlgorithm(staticKey).getJCEName());
        generator.initialize (eccgen, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();
        generatedEphemeralKey[0] = (ECPublicKey) keyPair.getPublic();
        return serverKeyAgreement((ECPublicKey)staticKey, keyPair.getPrivate());
    }
}
