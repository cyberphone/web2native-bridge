package org.webpki.w2nb.webpayment.test;

import java.io.IOException;

import java.math.BigInteger;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.security.interfaces.ECPublicKey;

import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;

import java.util.Vector;

import javax.crypto.KeyAgreement;

import org.webpki.asn1.ASN1OctetString;
import org.webpki.asn1.ASN1Sequence;
import org.webpki.asn1.BaseASN1Object;
import org.webpki.asn1.CompositeContextSpecific;
import org.webpki.asn1.DerDecoder;
import org.webpki.asn1.ParseUtil;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.KeyAlgorithms;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONParser;

import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.util.Base64URL;
import org.webpki.util.DebugFormatter;

import org.webpki.w2nb.webpayment.common.DecryptionKeyHolder;
import org.webpki.w2nb.webpayment.common.EncryptedData;
import org.webpki.w2nb.webpayment.common.Encryption;

public class Test {
    
     public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Missing: outputfile");
            System.exit(3);
        }
        CustomCryptoProvider.forcedLoad(true);
        System.out.println("Content Encryption");
    }
}
