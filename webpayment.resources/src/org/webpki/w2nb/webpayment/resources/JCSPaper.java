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
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Vector;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.crypto.SignatureWrapper;
import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONArrayWriter;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.json.JSONSignatureDecoder;
import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.util.ISODateTime;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.KeyStoreEnumerator;
import org.webpki.w2nb.webpayment.common.Payee;
import org.webpki.w2nb.webpayment.common.ServerAsymKeySigner;
import org.webpki.w2nb.webpayment.common.ServerX509Signer;

public class JCSPaper implements BaseProperties {
    
    static FileOutputStream fos;
    
    static final String CONTEXT         = "https://json.sample-standards.org/payment";

    static final String STYLE_SIGNATURE = "background:#ffe8e8";
    static final String STYLE_KEY       = "background:#e8ffe8";
    static final String STYLE_MSG       = "font-weight:bold";
    
    static final String AUTHORIZATION   = "Authorization";
    static final String PAYMENT_REQUEST = "PaymentRequest";
    
    static final String JOSE_PAYLOAD    = "payload";
    static final String JOSE_PROTECTED  = "protected";
    static final String JOSE_SIGNATURE  = "signature";
    static final String JOSE_X5C        = "x5c";
    static final String JOSE_ALG        = "alg";
    static final String JOSE_KTY        = "kty";
    static final String JOSE_CRV        = "crv";
    static final String JOSE_JWK        = "jwk";
        
    static void write(byte[] utf8) throws Exception {
        fos.write(utf8);
    }
    
    static void write(String utf8) throws Exception {
        write(utf8.getBytes("UTF-8"));
    }
    static void write(JSONObjectWriter json) throws Exception {
        write(json.serializeJSONObject(JSONOutputFormats.PRETTY_HTML));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.out.println("\nUsage: " +
                               JCSPaper.class.getCanonicalName() +
                               "jcspaper logotype merchantCertFile mybankCertFile certFilePassword");
            System.exit(-3);
        }
        CustomCryptoProvider.forcedLoad(true);
        Date date = ISODateTime.parseDateTime("2016-02-02T10:07:00Z").getTime();
        
        fos = new FileOutputStream(args[0]);
        
        // Read key/certificate to be imported and create signer
        
        KeyStoreEnumerator paymentRequestKey = new KeyStoreEnumerator (new FileInputStream(args[2]), args[4]);
        ServerAsymKeySigner paymentRequestSigner = new ServerAsymKeySigner(paymentRequestKey);
        KeyStoreEnumerator authorizationKey = new KeyStoreEnumerator (new FileInputStream(args[3]), args[4]);
        ServerX509Signer authorizationSigner = new ServerX509Signer(authorizationKey);

        // Header
        write("<!DOCTYPE html><html><head><title>JSON Signatures</title><style type=\"text/css\">\n" +
              "body {font-size:8pt;color:#000000;font-family:verdana,arial;background-color:white;margin:10pt}\n" +
              "h2 {font-weight:bold;font-size:12pt;color:#000000;font-family:arial,verdana,helvetica}\n" +
              "h3 {font-weight:bold;font-size:11pt;color:#000000;font-family:arial,verdana,helvetica}\n" +
              "a {font-weight:bold;font-size:8pt;color:blue;font-family:arial,verdana;text-decoration:none}\n" +
              "table {border-spacing:0;border-collapse: collapse;box-shadow:3pt 3pt 3pt #D0D0D0}\n" +
              "td {border-color:black;border-style:solid;border-width:1px;" +
              "padding:2pt 4pt 2pt 4pt;font-size:8pt;font-family:verdana,arial;text-align:center}\n" +
              "div.json {word-wrap:break-word;width:800pt;background:#F8F8F8;border-width:1px;border-style:solid;border-color:grey;padding:10pt;box-shadow:3pt 3pt 3pt #D0D0D0}\n" +
              ".smalltext {font-size:6pt;font-family:verdana,arial}\n" +
              "</style></head><body>" +
              "<div style=\"cursor:pointer;padding:2pt 0 0 0;width:100pt;height:47pt;border-width:1px;" +
              "border-style:solid;border-color:black;box-shadow:3pt 3pt 3pt #D0D0D0\" " +
              "onclick=\"document.location.href='http://webpki.org'\" title=\"Home of WebPKI.org\">");
        write(ArrayUtil.readFile(args[1]));
        write("</div><div style=\"width:800pt;text-align:center\"><h2>JSON Signatures</h2></div>" +
           "<div style=\"width:800pt;text-align:center\">" +
           "<table style=\"margin-left:auto;margin-right:auto\"><tr><td><span style=\"" + STYLE_MSG + "\">&nbsp;<br>" + AUTHORIZATION + "</span>" +
              "<table style=\"margin:15pt\"><tr><td style=\"padding:10pt 15pt 10pt 15pt;" + STYLE_MSG + "\">" + PAYMENT_REQUEST + "</td></tr>" +
                "<tr><td style=\"" + STYLE_KEY + "\">Public Key</td></tr>" +
                 "<tr><td style=\"" + STYLE_SIGNATURE + "\">Signature</td></tr>" +
               "</table></td></tr>" +
                "<tr><td style=\"" + STYLE_KEY + "\">X.509 Certificate Path</td></tr>" +
                "<tr><td style=\"" + STYLE_SIGNATURE + "\">Signature</td></tr>" +
           "</table></div><div class=\"json\">");
        JSONObjectWriter paymentRequest = new JSONObjectWriter();
        paymentRequest.setString(JSONDecoderCache.CONTEXT_JSON, CONTEXT);
        paymentRequest.setString(JSONDecoderCache.QUALIFIER_JSON, PAYMENT_REQUEST);
        paymentRequest.setObject(PAYEE_JSON, new Payee("Demo Merchant", "1209").writeObject());
        paymentRequest.setString(AMOUNT_JSON, "235.50");
        paymentRequest.setString(CURRENCY_JSON, "USD");
        paymentRequest.setString(REFERENCE_ID_JSON, "05630753");
        paymentRequest.setString(TIME_STAMP_JSON, "2016-02-02T10:07:41Z");
        JSONObjectWriter josePaymentRequest = new JSONObjectWriter(JSONParser.parse(paymentRequest.toString()));
        paymentRequest.setSignature(paymentRequestSigner);
        JSONObjectReader joseHeader = JSONParser.parse(paymentRequest.toString())
            .getObject(JSONSignatureDecoder.SIGNATURE_JSON)
                .getObject(JSONSignatureDecoder.PUBLIC_KEY_JSON);
        JSONObjectWriter joseSignedPaymentRequest = 
            jws(josePaymentRequest, 
                new JSONObjectWriter()
                    .setString(JOSE_ALG, AsymSignatureAlgorithms.ECDSA_SHA256.getAlgorithmId(AlgorithmPreferences.JOSE))
                    .setObject(JOSE_JWK, new JSONObjectWriter()
                        .setString(JOSE_KTY, joseHeader.getString(JSONSignatureDecoder.TYPE_JSON))
                        .setString(JOSE_CRV, joseHeader.getString(JSONSignatureDecoder.CURVE_JSON))
                        .setString(JSONSignatureDecoder.X_JSON, joseHeader.getString(JSONSignatureDecoder.X_JSON))
                        .setString(JSONSignatureDecoder.Y_JSON, joseHeader.getString(JSONSignatureDecoder.Y_JSON))),
                 paymentRequestKey);
        JSONObjectWriter writer = new JSONObjectWriter();
        writer.setString(JSONDecoderCache.CONTEXT_JSON, CONTEXT);
        writer.setString(JSONDecoderCache.QUALIFIER_JSON, AUTHORIZATION);
        writer.setObject(PAYMENT_REQUEST_JSON, paymentRequest);
        writer.setString(TRANSACTION_ID_JSON, "#1250000005");
        writer.setString(TIME_STAMP_JSON, "2016-02-02T10:07:42Z");
        JSONObjectWriter joseAuthorization = new JSONObjectWriter(JSONParser.parse(writer.toString()));
        joseAuthorization.setupForRewrite(PAYMENT_REQUEST_JSON);
        joseAuthorization.setObject(PAYMENT_REQUEST_JSON, joseSignedPaymentRequest);
        writer.setSignature(authorizationSigner);
        write(writer.serializeJSONObject(JSONOutputFormats.PRETTY_HTML));
        write("</div>");
 
        write("</div>HI<div class=\"json\">");
        JSONObjectWriter joseAuthorizationHeader = new JSONObjectWriter()
            .setString(JOSE_ALG, AsymSignatureAlgorithms.ECDSA_SHA256.getAlgorithmId(AlgorithmPreferences.JOSE));
        JSONArrayWriter aw = joseAuthorizationHeader.setArray(JOSE_X5C);
        for (X509Certificate cert : authorizationKey.getCertificatePath()) {
            aw.setString(new Base64(false).getBase64StringFromBinary(cert.getEncoded()));
        }
        writer = jws(joseAuthorization,
                     joseAuthorizationHeader,
                     authorizationKey);
        JSONObjectReader verifier = JSONParser.parse(writer.toString());
        checkJws (verifier);
        checkJws(JSONParser.parse(verifier.getBinary(JOSE_PAYLOAD)).getObject(PAYMENT_REQUEST_JSON));
        write(writer.serializeJSONObject(JSONOutputFormats.PRETTY_HTML));
        write("</div>");

        write("<div class=\"json\">");
        String jsPaymentRequest = paymentRequest.serializeToString(JSONOutputFormats.PRETTY_HTML);
        jsPaymentRequest = jsPaymentRequest.replaceAll("(&quot;)(<span style=\"color:#C00000\">)([a-z A-Z]+)(<\\/span>)(&quot;)", "$2$3$4");
        write("<span style=\"color:orange\">var</span>&nbsp;<span style=\"color:#00A000\">" +
              PAYMENT_REQUEST_JSON + "</span>&nbsp;=&nbsp;" + jsPaymentRequest.replaceAll("<br>}", "<br>};"));
        write("</div>");
        write("</body></html>");
        fos.close();
    }

    static void checkJws(JSONObjectReader jws) throws Exception {
        JSONObjectReader protectedHeader = JSONParser.parse(jws.getBinary(JOSE_PROTECTED));
        if (!protectedHeader.getString(JOSE_ALG).equals(AsymSignatureAlgorithms.ECDSA_SHA256.getAlgorithmId(AlgorithmPreferences.JOSE))) {
            throw new IOException("Bad alg");
        }
        PublicKey publicKey = null;
        if (protectedHeader.hasProperty(JOSE_JWK)) {
            JSONObjectReader jwk = protectedHeader.getObject(JOSE_JWK);
            JSONObjectWriter josePK = new JSONObjectWriter()
                .setObject(JSONSignatureDecoder.PUBLIC_KEY_JSON, new JSONObjectWriter()
                    .setString(JSONSignatureDecoder.TYPE_JSON, jwk.getString(JOSE_KTY))
                    .setString(JSONSignatureDecoder.CURVE_JSON, jwk.getString(JOSE_CRV))
                    .setString(JSONSignatureDecoder.X_JSON, jwk.getString(JSONSignatureDecoder.X_JSON))
                    .setString(JSONSignatureDecoder.Y_JSON, jwk.getString(JSONSignatureDecoder.Y_JSON)));
            publicKey = JSONParser.parse(josePK.toString()).getPublicKey();
        } else {
            JSONArrayReader ar = protectedHeader.getArray(JOSE_X5C);
            Vector<X509Certificate> certs = new Vector<X509Certificate>();
            while (ar.hasMore()) {
                certs.add(CertificateUtil.getCertificateFromBlob(new Base64().getBinaryFromBase64String(ar.getString())));
            }
            publicKey = certs.firstElement().getPublicKey();
        }
        protectedHeader.checkForUnread();
        if (!new SignatureWrapper(AsymSignatureAlgorithms.ECDSA_SHA256, publicKey)
                     .update ((jws.getString(JOSE_PAYLOAD) + "." + jws.getString(JOSE_PROTECTED)).getBytes("UTF-8"))
                     .verify (jws.getBinary(JOSE_SIGNATURE))) {
            throw new IOException ("Verify");
        }
        jws.checkForUnread();
    }

    static JSONObjectWriter jws(JSONObjectWriter payload, 
                                JSONObjectWriter protectedHeader,
                                KeyStoreEnumerator signatureKey) throws Exception {
        JSONObjectWriter signature = new JSONObjectWriter()
            .setBinary(JOSE_PAYLOAD, payload.serializeJSONObject(JSONOutputFormats.NORMALIZED))
            .setBinary(JOSE_PROTECTED, protectedHeader.serializeJSONObject(JSONOutputFormats.NORMALIZED));
        JSONObjectReader reader = JSONParser.parse(signature.toString());
        signature.setBinary(JOSE_SIGNATURE,
                             new SignatureWrapper(AsymSignatureAlgorithms.ECDSA_SHA256,
                                                  signatureKey.getPrivateKey())
                                 .update((reader.getString(JOSE_PAYLOAD) + "." + reader.getString(JOSE_PROTECTED)).getBytes("UTF-8"))
                                 .sign());
        return signature;
    }
}