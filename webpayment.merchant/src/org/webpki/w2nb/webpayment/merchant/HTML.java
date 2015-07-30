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

import javax.servlet.ServletException;

import javax.servlet.http.HttpServletResponse;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nb.webpayment.common.PaymentRequest;

public class HTML {

    static final int PAYMENT_TIMEOUT_INIT            = 5000;
    
    static final String FONT_VERDANA = "Verdana,'Bitstream Vera Sans','DejaVu Sans',Arial,'Liberation Sans'";
    static final String FONT_ARIAL = "Arial,'Liberation Sans',Verdana,'Bitstream Vera Sans','DejaVu Sans'";
    
    static final String HTML_INIT = 
        "<!DOCTYPE html>"+
        "<html><head><meta charset=\"UTF-8\"><link rel=\"shortcut icon\" href=\"favicon.ico\">"+
//        "<meta name=\"viewport\" content=\"initial-scale=1.0\"/>" +
        "<title>W2NB Payment Demo</title>"+
        "<style type=\"text/css\">html {overflow:auto}\n"+
        ".tftable {border-collapse:collapse;box-shadow:3pt 3pt 3pt #D0D0D0}\n" +
        ".tftable th {font-size:10pt;background:" +
          "linear-gradient(to bottom, #eaeaea 14%,#fcfcfc 52%,#e5e5e5 89%);" +
          "border-width:1px;padding:4pt 10pt 4pt 10pt;border-style:solid;border-color:#a9a9a9;" +
          "text-align:center;font-family:" + FONT_ARIAL + "}\n" +
        ".tftable td {background-color:#FFFFE0;font-size:10pt;border-width:1px;padding:4pt 8pt 4pt 8pt;border-style:solid;border-color:#a9a9a9;font-family:" + FONT_ARIAL + "}\n" +
        "body {font-size:10pt;color:#000000;font-family:" + FONT_VERDANA + ";background-color:white}\n" +
        "a {font-weight:bold;font-size:8pt;color:blue;font-family:" + FONT_ARIAL + ";text-decoration:none}\n" +
        "td {font-size:8pt;font-family:" + FONT_VERDANA + "}\n" +
        ".quantity {text-align:right;font-weight:normal;font-size:10pt;font-family:" + FONT_ARIAL + "}\n" +
        ".stdbtn {font-weight:normal;font-size:10pt;font-family:" + FONT_ARIAL + "}\n" +
        ".updnbtn {vertical-align:middle;text-align:center;font-weight:normal;font-size:8px;font-family:" + FONT_VERDANA + ";margin:0px;border-spacing:0px;padding:2px 3px 2px 3px}\n";
    
    static String encode(String val) {
        if (val != null) {
            StringBuffer buf = new StringBuffer(val.length() + 8);
            char c;
            for (int i = 0; i < val.length(); i++) {
                c = val.charAt(i);
                switch (c) {
                    case '<':
                      buf.append("&lt;");
                      break;
                    case '>':
                      buf.append("&gt;");
                      break;
                    case '&':
                      buf.append("&amp;");
                      break;
                    case '\"':
                      buf.append("&#034;");
                      break;
                    case '\'':
                      buf.append("&#039;");
                      break;
                    default:
                      buf.append(c);
                      break;
                }
            }
            return buf.toString();
        } else {
            return new String("");
        }
    }
    
    static String getHTML(String javascript, String bodyscript, String box) {
        StringBuffer s = new StringBuffer(HTML_INIT + "html, body {margin:0px;padding:0px;height:100%}</style>");
        if (javascript != null) {
            s.append("<script type=\"text/javascript\">").append(javascript).append("</script>");
        }
        s.append("</head><body");
        if (bodyscript != null) {
            if (bodyscript.charAt(0) != '>') {
                s.append(' ');
            }
            s.append(bodyscript);
        }
        s.append("><div onclick=\"document.location.href='home"
            + "'\" title=\"Home sweet home...\" style=\"cursor:pointer;position:absolute;top:15px;"
            + "left:15px;z-index:5;visibility:visible;padding:5pt 8pt 5pt 8pt;font-size:10pt;"
            + "text-align:center;background: radial-gradient(ellipse at center, rgba(255,255,255,1) "
            + "0%,rgba(242,243,252,1) 38%,rgba(196,210,242,1) 100%);border-radius:8pt;border-width:1px;"
            + "border-style:solid;border-color:#B0B0B0;box-shadow:3pt 3pt 3pt #D0D0D0;}\">"
            + "Web2Native Bridge<br><span style=\"font-size:8pt\">Payment Demo Home</span></div>"
            + "<table cellpadding=\"0\" cellspacing=\"0\" width=\"100%\" height=\"100%\">")
         .append(box)
         .append("</table></body></html>");
        return s.toString();
    }
    
    static void output(HttpServletResponse response, String html) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(html.getBytes("UTF-8"));
    }

    public static void homePage(HttpServletResponse response, boolean pullPaymentMode) throws IOException, ServletException {
        HTML.output(response, HTML.getHTML(null, null,
                "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
                "<table style=\"max-width:600px;\" cellpadding=\"4\">" +
                   "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">Web2Native Bridge Payment Demo<br>&nbsp;</td></tr>" +
                   "<tr><td style=\"text-align:left\">This application is a demo of what a true WebCrypto++ implementation " +
                   "could offer for <span style=\"color:red\">decentralized payment systems</span>.</td></tr>" +
                   "<tr><td style=\"text-align:left\">In particular note the <span style=\"color:red\">automatic payment card discovery</span> process " +
                   "and that <span style=\"color:red\">payment card logotypes are personalized</span> since they "+
                   "are read from the user's local credential-store.</td></tr>" +
                   "<tr><td>By applying <span style=\"color:red\">3D Secure</span> like methods and <span style=\"color:red\">EMV tokenization</span>, there is no need for " +
                   "handing over static credit-card information to merchants.</td></tr>" +
                   "<tr><td style=\"text-align:left\">For protecting the user's privacy, <span style=\"color:red\">user-related data is encrypted</span> and only readable " +
                   "by the payment-provider who issued the specific payment card.</td></tr>" +
                   "<tr><td style=\"text-align:left\">Although the demo is <i>partially</i> a mockup (no &quot;polyfill&quot; in the world can replace WebCrypto++), " +
                   "the IFRAME solution and cross-domain communication using <code>postMessage()</code> should be pretty close to that of a real system.</td></tr>" +
                   "<tr><td style=\"text-align:left\"><i>In case you are testing with a WebCrypto-enabled browser, the user-authorization will be signed and encrypted " +
                   "which can viewed in a browser debugger window.</i></td></tr>" +
                   "<tr><td align=\"center\"><table cellspacing=\"0\">" +
//TODO
//                   "<tr style=\"text-align:left\"><td><a href=\"" + "hh" + "/cards\">Initialize Payment Cards&nbsp;&nbsp;</a></td><td><i>Mandatory</i> First Step</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a href=\"" + "shop" + "\">Go To Merchant</a></td><td>Shop Til You Drop!</td></tr>" +
                   "<tr style=\"text-align:left\"><td><form name=\"pull\" method=\"POST\"><input type=\"checkbox\" name=\"pull\" onclick=\"document.forms.pull.submit()\"" +
                   (pullPaymentMode ? " checked" : "") +
                   "></form></td><td>Pull payment option</td></tr>" +
                                  "<tr><td style=\"text-align:center;padding-top:15pt;padding-bottom:5pt\" colspan=\"2\"><b>Documentation</b></td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"http://webpki.org/papers/PKI/pki-webcrypto.pdf\">WebCrypto++</a></td><td><i>Conceptual</i> Specification</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"http://webpki.org/papers/PKI/EMV-Tokenization-SET-3DSecure-WebCryptoPlusPlus-combo.pdf#page=4\">Demo Payment System</a></td><td>State Diagram Etc.</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://code.google.com/p/openkeystore/source/browse/#svn/wcpp-payment-demo\">Demo Source Code</a></td><td>For Nerds...</td></tr>" +
                   "<tr><td style=\"text-align:center;padding-top:15pt;padding-bottom:5pt\" colspan=\"2\"><b>Related Applications</b></td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://mobilepki.org/jcs\">JCS</a></td><td>JSON Cleartext Signature</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://play.google.com/store/apps/details?id=org.webpki.mobile.android\">SKS/KeyGen2</a></td><td>Android PoC</td></tr>" +
                   "<tr style=\"text-align:left\"><td><a target=\"_blank\" href=\"https://mobilepki.org/WCPPSignatureDemo\">User Signatures</a></td><td>WebCrypto++ Signature Demo</td></tr>" +
                 "</table></td></tr></table></td></tr>"));
    }

    static String javaScript(String string) {
        StringBuffer s = new StringBuffer();
        for (char c : string.toCharArray()) {
            if (c == '\n') {
                s.append("\\n");
            } else if (c == '\'') {
                s.append("\\'");
            } else if (c == '\\') {
                s.append("\\\\");
            } else {
                s.append(c);
            }
        }
        return s.toString();
    }

    private static StringBuffer productEntry(StringBuffer temp_string,
                                             ProductEntry product_entry,
                                             String sku,
                                             SavedShoppingCart savedShoppingCart,
                                             int index) {
        int units = savedShoppingCart.items.containsKey(sku) ? savedShoppingCart.items.get(sku): 0;
        StringBuffer s = new StringBuffer(
            "<tr style=\"text-align:center\"><td><img src=\"images/")
        .append(product_entry.imageUrl)
        .append("\"></td><td>")
        .append(product_entry.name)
        .append("</td><td style=\"text-align:right\">")
        .append(price(product_entry.priceX100))
        .append(
            "</td><td><form>" +
            "<table style=\"border-width:0px;padding:0px;margin:0px;border-spacing:2px;border-collapse:separate\">" +
            "<tr>" +
            "<td style=\"border-width:0px;padding:0px;margin:0px\"><input type=\"button\" class=\"updnbtn\" value=\"&#x25b2;\" title=\"More\" onclick=\"updateUnits(this.form.p")
        .append(index)
        .append(", 1, ")
        .append(index)
        .append(")\"></td>" +
            "</tr>" +
            "<tr>" +
            "<td style=\"border-width:0px;padding:0px;margin:0px\"><input size=\"6\" type=\"text\" name=\"p")
        .append(index)
        .append("\" value=\"")
        .append(units)
        .append("\" class=\"quantity\" oninput=\"updateInput(")
        .append(index)
        .append(", this);\" autocomplete=\"off\"/></td>" +
            "</tr>" +
            "<tr>" +
            "<td style=\"border-width:0px;padding:0px;margin:0px\">"
            + "<input type=\"button\" class=\"updnbtn\" value=\"&#x25bc;\" title=\"Less\" "
            + "onclick=\"updateUnits(this.form.p")
        .append(index)
        .append(", -1, ")
        .append(index)
        .append(")\"></td></tr></table></form></td></tr>");
        temp_string.insert(0, "shopping_cart[" + index + "] = new webpki.ShopEntry(" 
                       + product_entry.priceX100 + ",'" + product_entry.name + "','" + sku + "'," + units + ");\n");        
        return s;
    }

    private static String price(int priceX100) {
        return (MerchantService.currency.symbolFirst ? MerchantService.currency.symbol : "")
               + String.valueOf(priceX100 / 100) + "."
               + String.valueOf((priceX100 % 100) / 10)
               + String.valueOf(priceX100 % 10)
               + (MerchantService.currency.symbolFirst ? "" : MerchantService.currency.symbol);
    }
    
    public static void merchantPage(HttpServletResponse response,
                                    SavedShoppingCart savedShoppingCart) throws IOException, ServletException {
        StringBuffer temp_string = new StringBuffer(
            "\nfunction checkOut() {\n" +
            "    if (getTotal()) {\n" +
            "        document.getElementById('shoppingCart').value = JSON.stringify(shopping_cart);\n" +
            "        document.forms.shoot.submit();\n" +           
            "    } else {\n" +
            "        document.getElementById('emptybasket').style.top = ((window.innerHeight - document.getElementById('emptybasket').offsetHeight) / 2) + 'px';\n" +
            "        document.getElementById('emptybasket').style.left = ((window.innerWidth - document.getElementById('emptybasket').offsetWidth) / 2) + 'px';\n" +
            "        document.getElementById('emptybasket').style.visibility = 'visible';\n" +
            "        setTimeout(function() {\n" +
            "            document.getElementById('emptybasket').style.visibility = 'hidden';\n" +
            "        }, 1000);\n" +
            "    }\n" +
            "}\n\n" +
            "function getTotal() {\n" +
            "    var total = 0;\n" +
            "    for (var i = 0; i < shopping_cart.length; i++) {\n" +
            "        total += shopping_cart[i].priceX100 * shopping_cart[i].units;\n" +
            "    }\n" +
            "    return total;\n"+
            "}\n\n" +
            "function getPriceString() {\n" +
            "    var priceX100 = getTotal();\n" +
            "    return ");
        if (MerchantService.currency.symbolFirst) {
            temp_string.append('\'')
                       .append(MerchantService.currency.symbol)
                       .append("' + ");
        }
        temp_string.append("Math.floor(priceX100 / 100) + '.' +  Math.floor((priceX100 % 100) / 10) +  Math.floor(priceX100 % 10)");
        if (!MerchantService.currency.symbolFirst) {
            temp_string.append(" + '")
                       .append(MerchantService.currency.symbol)
                       .append('\'');
        }
        temp_string.append(";\n" +
            "}\n\n" +
            "function updateTotal() {\n" +
            "    document.getElementById('total').innerHTML = getPriceString();\n" +
            "}\n\n" +
            "function updateInput(index, control) {\n" +
            "    if (!numeric_only.test(control.value)) control.value = '0';\n" +
            "    while (control.value.length > 1 && control.value.charAt(0) == '0') control.value = control.value.substring(1);\n" +
            "    shopping_cart[index].units = parseInt(control.value);\n" +
            "    updateTotal();\n" +
            "}\n\n" +
            "function updateUnits(control, value, index) {\n" +
            "    control.value = parseInt(control.value) + value;\n" +
            "    updateInput(index, control);\n" +
            "}\n");

        StringBuffer page_data = new StringBuffer(
            "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
            "<table>" +
            "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">" +
            ShoppingServlet.COMMON_NAME +
            "<br>&nbsp;</td></tr>" +
            "<tr><td id=\"result\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\">" +
            "<tr><th>Image</th><th>Description</th><th>Price</th><th>Units</th></tr>");
        int q = 0;
        for (String sku : ShoppingServlet.products.keySet()) {
            page_data.append(productEntry(temp_string, ShoppingServlet.products.get(sku), sku, savedShoppingCart, q++));
        }
        page_data.append(
            "</table></tr></td><tr><td style=\"padding-top:10pt\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\"><tr><th style=\"text-align:center\">Amount to Pay</th><td style=\"text-align:right\" id=\"total\">")
            .append(price(savedShoppingCart.total))
            .append("</td></tr>" +
            "</table></td></tr>" +
            "<tr><td style=\"text-align:center;padding-top:10pt\" id=\"pay\"><input class=\"stdbtn\" type=\"button\" value=\"Checkout..\" title=\"Paying time has come...\" onclick=\"checkOut()\"></td></tr>" +
            "</table>" +
            "<form name=\"shoot\" method=\"POST\" action=\"checkout\">" +
            "<input type=\"hidden\" name=\"shoppingCart\" id=\"shoppingCart\">" +
            "</form></td></tr>");
         temp_string.insert(0,
            "\n\n\"use strict\";\n\n" +
            "var numeric_only = new RegExp('^[0-9]{1,6}$');\n\n" +
            "var webpki = {};\n\n" +
            "webpki.ShopEntry = function(priceX100, name,sku, units) {\n" +
            "    this.priceX100 = priceX100;\n" +
            "    this.name = name;\n" +
            "    this.sku = sku;\n" +
            "    this.units = units;\n" +
            "};\n\n" +
            "var shopping_cart = [];\n");

        HTML.output(response, HTML.getHTML(temp_string.toString(), 
            "><div id=\"emptybasket\" style=\"border-color:grey;border-style:solid;border-width:3px;text-align:center;font-family:"
            + FONT_ARIAL+ ";z-index:3;background:#f0f0f0;position:absolute;visibility:hidden;padding:5pt 10pt 5pt 10pt\">Nothing ordered yet...</div",
            page_data.toString()));
    }

    public static void checkoutPage(HttpServletResponse response,
                                    SavedShoppingCart savedShoppingCart, 
                                    boolean pullPaymentMode,
                                    String invoke_json) throws IOException, ServletException {
        StringBuffer s = new StringBuffer(
            "<tr><td width=\"100%\" align=\"center\" valign=\"middle\">" +
            "<table>" +
            "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:"
            + FONT_ARIAL + "\">Current Order<br>&nbsp;</td></tr>" +
            "<tr><td id=\"result\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\">" +
            "<tr><th>Description</th><th>Price</th><th>Units</th></tr>");
        for (String sku : savedShoppingCart.items.keySet()) {
            ProductEntry product_entry = ShoppingServlet.products.get(sku);
            s.append("<tr style=\"text-align:center\"><td>")
             .append(product_entry.name)
             .append("</td><td style=\"text-align:right\">")
             .append(price(product_entry.priceX100))
             .append("</td><td>")
             .append(savedShoppingCart.items.get(sku).intValue())
             .append("</td></tr>");                
        }
        s.append(
            "</table></td></tr><tr><td style=\"padding-top:10pt\"><table style=\"margin-left:auto;margin-right:auto\" class=\"tftable\"><tr><th style=\"text-align:center\">Amount to Pay</th><td style=\"text-align:right\" id=\"total\">")
         .append(price(savedShoppingCart.total))
         .append("</td></tr>" +
                 "</table></td></tr>" +
                 "<tr><td style=\"text-align:center;padding-top:10pt\" id=\"pay\">")
         .append("Wallet")
         .append("</td></tr></table>" +
                  "<form name=\"shoot\" method=\"POST\" action=\"")
         .append(pullPaymentMode ? "pullpay" : "pushpay")
         .append("\"><input type=\"hidden\" name=\"authreq\" id=\"authreq\">" +
                  "</form>" +
                  "<form name=\"restore\" method=\"POST\" action=\"shop\">" +
                  "</form></td></tr>");
        
        StringBuffer temp_string = new StringBuffer("\n\n\"use strict\";\n\nvar invokeRequest =\n")
            .append(invoke_json)
            .append(";\n\n" +

                    "var nativePort = null;\n\n" +

                    "function closeWallet() {\n" +
                    "    if (nativePort) {\n" +
                    "        nativePort.disconnect();\n" +
                    "        nativePort = null;\n" +
                    "    }\n" +
                    "}\n\n" +

                    "function setString(message) {\n" +
                    "    closeWallet();\n" +
                    "    console.debug(message);\n" +
                    "}\n\n" +

                    "function activateWallet() {\n" +
                    "    var initMode = true;\n" +
                    "    if (!navigator.nativeConnect) {\n" +
                    "        setString('\"navigator.nativeConnect\" not found, \\ncheck Chrome Web2Native Bridge extension settings');\n" +
                    "        return;\n" +
                    "    }\n" +
                    "    navigator.nativeConnect(\"org.webpki.w2nb.webpayment.client\").then(function(port) {\n" +
                    "        nativePort = port;\n" +
                    "        port.addMessageListener(function(message) {\n" +
                    "            if (message[\"@context\"] != \"" + BaseProperties.W2NB_PAY_DEMO_CONTEXT_URI + "\") {\n" +
                    "                setString(\"Missing or wrong \\\"@context\\\"\");\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            var qualifier = message[\"@qualifier\"];\n" +
                    "            if ((initMode && qualifier != \"" + Messages.WALLET_IS_READY.toString() + "\")  ||\n" +
                    "                (!initMode && qualifier != \"")
             .append(pullPaymentMode ? 
                     Messages.PAYER_PULL_AUTH_REQ.toString() 
                                     :
                     Messages.PROVIDER_GENERIC_AUTH_RES.toString())
             .append("\")) {\n" +  
                    "                setString(\"Wrong or missing \\\"@qualifier\\\"\");\n" +
                    "                return;\n" +
                    "            }\n" +
                    "            if (initMode) {\n" +
                    "                initMode = false;\n" +
                    "                nativePort.postMessage(invokeRequest);\n" +
                    "            } else {\n" +
                    "                document.getElementById(\"authreq\").value = JSON.stringify(message);\n" +
                    "                document.forms.shoot.submit();\n" +
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
                    "    closeWallet();\n" +
                    "});\n\n");
        HTML.output(response, HTML.getHTML(temp_string.toString(),
                              "onload=\"activateWallet()\"",
                              s.toString()));
    }

    public static void resultPage(HttpServletResponse response,
                                  String error_message,
                                  PaymentRequest paymentRequest, 
                                  String cardType,
                                  String cardReference) throws IOException, ServletException {
        StringBuffer s = new StringBuffer("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">");
        if (error_message == null) {
            s.append("<table>" +
             "<tr><td style=\"text-align:center;font-weight:bolder;font-size:10pt;font-family:" + FONT_ARIAL + "\">Order Status<br>&nbsp;</td></tr>" +
             "<tr><td style=\"text-align:center;padding-bottom:15pt;font-size:10pt\">Dear customer, your order has been successfully processed!</td></tr>" +
             "<tr><td><table class=\"tftable\"><tr><th>Our Reference</th><th>Amount</th><th>Card Type</th><th>Card Number</th></tr>" +
             "<tr><td style=\"text-align:center\">")
            .append(paymentRequest.getReferenceId())
            .append("</td><td style=\"text-align:center\">")
            .append(paymentRequest.getCurrency().convertAmountToString(paymentRequest.getAmount()))
            .append("</td><td style=\"text-align:center\">")
            .append(cardType)
            .append("</td><td style=\"text-align:center\">")
            .append(cardReference)
            .append("</td></tr></table></td></tr></table>");
        } else {
            s.append("There was a problem with your order: " + error_message);
        }
        s.append("</td></tr>");
        HTML.output(response, 
                    HTML.getHTML("history.pushState(null, null, 'payment');\n" +
                                 "window.addEventListener('popstate', function(event) {\n" +
                                 "    history.pushState(null, null, 'payment');\n" +
                                 "});",
                                 null,
                                 s.toString()));
    }
}
