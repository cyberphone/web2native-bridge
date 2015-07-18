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
import java.math.BigDecimal;

import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONX509Signer;
import org.webpki.w2nb.webpayment.common.CardTypes;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.Currencies;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nb.webpayment.common.PaymentRequest;
import org.webpki.w2nb.webpayment.common.ServerSigner;

public class InitTestPage implements BaseProperties {
    
    enum TESTS {
        Normal      ("Normal"), 
        Slow        ("Slow (but legal) response"),
        Scroll      ("Many matching cards (=scroll view)"),
        NonMatching ("No card should match"),
        Timeout     ("Timeouted response"),
        Syntax      ("Bad message syntax"),
        Signature   ("Bad signature");
        
        String descrition;
        
        TESTS(String description) {
            this.descrition = description;
        }
    };
   
    static FileOutputStream fos;
    
    static void write(byte[] utf8) throws Exception {
        fos.write(utf8);
    }
    
    static void write(String utf8) throws Exception {
        write(utf8.getBytes("UTF-8"));
    }
    static void write(JSONObjectWriter json) throws Exception {
        write(json.serializeJSONObject(JSONOutputFormats.JS_NATIVE));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.out.println("\nUsage: " +
                               InitTestPage.class.getCanonicalName() +
                               "testpage merchantCertFile certFilePassword");
            System.exit(-3);
        }
        CustomCryptoProvider.forcedLoad(true);
        
        fos = new FileOutputStream(args[0]);
        
        // Read key/certificate to be imported and create signer
        JSONX509Signer signer = new ServerSigner(new FileInputStream(args[1]), args[2]).getJSONX509Signer();
        JSONObjectWriter standardRequest = PaymentRequest.encode("Demo Merchant",
                                                                 new BigDecimal("306.25"),
                                                                 Currencies.USD,
                                                                 "#6100004",
                                                                 signer);
        // Header
        write("<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Payment Agent (Wallet) Tester</title>"
              + "</head><body><script>\n\n" +

              "\"use strict\";\n\n" +

              "function setString(rawString) {\n" +
              "    var text = \"\";\n" +
              "    for (var n = 0; n < rawString.length; n++) {\n" +
              "        var c = rawString.charAt(n);\n" +
              "        text += c == \"<\" ? \"&lt;\" : c == \">\" ? \"&gt;\" : c;\n" +
              "    }\n" +
              "    document.getElementById(\"response\").innerHTML = text;\n" +
              "}\n\n" +
    
              "var nativePort = null;\n\n" +
              "var normalRequest =\n");
        write(Messages.createBaseMessage(Messages.INVOKE_WALLET)
            .setStringArray(ACCEPTED_CARD_TYPES_JSON,
                            new String[]{"NoSuchCard",
                                          CardTypes.SuperCard.toString(),
                                          CardTypes.CoolCard.toString()})
            .setObject(PAYMENT_REQUEST_JSON, standardRequest));

        write(";\n\n" +
              "// All our cards should match during the discovery phase...\n" +
              "var scrollMatchingRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
              "scrollMatchingRequest." + ACCEPTED_CARD_TYPES_JSON + " = [\"NoSuchCard\"");
        for (CardTypes card : CardTypes.values()) {
            write(", \"");
            write(card.toString());
            write("\"");
        }

        write("];\n\n" +
                "// No cards should match during the discovery phase...\n" +
                "var nonMatchingRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
                "nonMatchingRequest." + ACCEPTED_CARD_TYPES_JSON + " = [\"NoSuchCard\"];\n\n");

        write("// Note the modified \"" + PAYEE_JSON + "\" property...\n" +
              "var badSignatureRequest = JSON.parse(JSON.stringify(normalRequest)); // Deep clone\n" +
              "badSignatureRequest." + PAYMENT_REQUEST_JSON + "." +  PAYEE_JSON + "= \"DEmo Merchant\";\n\n");

        write("var badMessageRequest = {\"hi\":\"there!\"};\n\n" +

              "function closeExtension() {\n" +
              "    if (nativePort) {\n" +
              "        nativePort.disconnect();\n" +
              "        nativePort = null;\n" +
              "    }\n" +
              "}\n\n" +

              "function sendMessageConditional(message) {\n" +
              "    if (nativePort) {\n" +
              "        nativePort.postMessage(message);\n" +
              "    }\n" +
              "}\n\n" +

              "function activateExtension() {\n" +
              "    if (nativePort) {\n" +
              "        closeExtension();\n" +
              "    }\n" +
              "    setString(\"\");\n" +
              "    var initMode = true;\n" +
              "    var test = document.forms.shoot.test.value;\n" +
              "    navigator.nativeConnect(\"org.webpki.w2nb.webpayment.client\").then(function(port) {\n" +
              "        nativePort = port;\n" +
              "        console.debug('conn=' + JSON.stringify(port));\n" +
              "        port.addMessageListener(function(message) {\n" +
              "            if (message[\"@context\"] != \"" + BaseProperties.W2NB_PAY_DEMO_CONTEXT_URI + "\") {\n" +
              "                setString(\"Missing or wrong \\\"@context\\\"\");\n" +
              "                return;\n" +
              "            }\n" +
              "            var qualifier = message[\"@qualifier\"];\n" +
              "            if ((initMode && qualifier != \"" + Messages.WALLET_IS_READY.toString() + "\" ) ||\n" +
              "                (!initMode && qualifier != \"" + Messages.PAYER_GENERIC_AUTH_REQ.toString() + "\" && qualifier != \"" + Messages.PAYER_PULL_AUTH_REQ.toString() + "\")) {\n" +  
              "                setString(\"Wrong or missing \\\"@qualifier\\\"\");\n" +
              "                return;\n" +
              "            }\n" +
              "            if (initMode) {\n" +
              "                initMode = false;\n" +
              "                if (test == \"" + TESTS.Normal + "\") {\n" +
              "                    sendMessageConditional(normalRequest);\n" +
              "                } else if (test == \"" + TESTS.Slow + "\") {\n" +
              "                    setTimeout(function() {\n" +
              "                        sendMessageConditional(normalRequest);\n" +
              "                    }, 2000);\n" +
              "                } else if (test == \"" + TESTS.Scroll + "\") {\n" +
              "                    sendMessageConditional(scrollMatchingRequest);\n" +
              "                } else if (test == \"" + TESTS.NonMatching + "\") {\n" +
              "                    sendMessageConditional(nonMatchingRequest);\n" +
              "                } else if (test == \"" + TESTS.Timeout + "\") {\n" +
              "                    setTimeout(function() {\n" +
              "                        sendMessageConditional(normalRequest);\n" +
              "                    }, 20000);\n" +
              "                } else if (test == \"" + TESTS.Syntax + "\") {\n" +
              "                    sendMessageConditional(badMessageRequest);\n" +
              "                } else if (test == \"" + TESTS.Signature + "\") {\n" +
              "                    sendMessageConditional(badSignatureRequest);\n" +
              "                } else {\n" +
              "                    alert(\"Not implemented: \" + test);\n" +
              "                }\n" +
              "            } else {\n" +
              "                setTimeout(function() {\n" +
              "                    setString(JSON.stringify(message));\n" +
              "                    closeExtension();\n" +
              "                }, 1000);\n" +
              "            }\n"+
              "        });\n" +
              "        port.addDisconnectListener(function() {\n" +
              "            if (nativePort) {\n" +
              "                setString(\"Application Unexpectedly disconnected\");\n" +
              "            }\n" +
              "            nativePort = null;\n" +
              "        });\n" +
              "    }, function(err) {\n" +
              "        console.debug(err);\n" +
              "    });\n" +
              "}\n\n" +

              "window.addEventListener(\"beforeunload\", function(event) {\n" +
              "    closeExtension();\n" +
              "});\n\n" +

              "</script>\n" +
              "<h2>Web2Native Bridge &quot;Emulator&quot; - Payment Agent (Wallet) Tester</h2>\n" +
              "<input type=\"button\" style=\"margin-bottom:10pt;width:50pt\" value=\"Run!\" onclick=\"activateExtension()\">\n" +
              "<form name=\"shoot\">\n");
        for (TESTS test : TESTS.values()) {
            write("<input type=\"radio\" name=\"test\" value=\"");
            write(test.toString());
            write("\"");
            if (test == TESTS.Normal) {
                write(" checked");
            }
            write(">");
            write(test.descrition);
            write("<br>\n");
        }
       write("</form>\n" +
              "<div style=\"margin-top:10pt;margin-bottom:10pt\">Result:</div>\n" +
              "<div id=\"response\" style=\"font-family:courier;font-size:10pt;word-wrap:break-word;width:800pt\"></div>\n" +
              "</body></html>\n");
        fos.close();
    }
}