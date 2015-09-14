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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;
import org.webpki.w2nb.webpayment.common.BaseProperties;

public class DebugServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static Logger logger = Logger.getLogger(DebugServlet.class.getName());

    static final String STATIC_BOX = "font-size:8pt;word-wrap:break-word;width:800pt;background:#F8F8F8;";
    static final String COMMON_BOX = "border-width:1px;border-style:solid;border-color:grey;padding:10pt;box-shadow:3pt 3pt 3pt #D0D0D0";

    String fancyBox (byte[] json) throws IOException {
      return "<div style=\"" + STATIC_BOX + COMMON_BOX + "\">" +
              new String(JSONParser.parse(json).serializeJSONObject(JSONOutputFormats.PRETTY_HTML), "UTF-8") +
              "</div>";
    }

    String description(String string) {
        return "<div style=\"word-wrap:break-word;width:800pt;margin-bottom:10pt;margin-top:20pt\">" + string + "</div>";
    }
    
    String descriptionStdMargin(String string) {
        return "<div style=\"word-wrap:break-word;width:800pt;margin-bottom:10pt;margin-top:10pt\">" + string + "</div>";
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            DebugData debugData = null;
            HttpSession session = request.getSession(false);
            if (session == null ||
                (debugData = (DebugData)session.getAttribute(UserPaymentServlet.DEBUG_DATA_SESSION_ATTR)) == null) {
                throw new IOException("Session timed out");
            }
            StringBuffer s = new StringBuffer( );
            
            s.append(description("<p>The following page shows the messages exchanged between the " +
                                 "<b>Merchant</b> (Payee), the <b>Wallet</b> (Payer), and the user's <b>Bank</b> (Payment provider).&nbsp;&nbsp;" +
                                 "For traditional card payments there is also an <b>Acquirer</b> (&quot;Card processor&quot;) involved.</p>" +
                                 "<p>After the <b>Wallet</b> has been invoked, the invoking <b>Merchant</b> Web-page waits for a ready signal from the Wallet:</p>"));
            s.append(fancyBox(debugData.WalletInitialized));
 
            s.append(descriptionStdMargin("The " + keyWord(WINDOW_JSON) + " object provides the invoking <b>Merchant</b> Web-page with the size "+
                                 "of the <b>Wallet</b>.<p>When the message above has been received the <b>Merchant</b> Web-page responds with a " +
                                 "list of accepted card types and a <i>digitally signed</i> payment request:</p>"));
            s.append(fancyBox(debugData.InvokeWallet));

            s.append(description("After <i>optional</i> selection of payment credentials (Cards), the user " +
                    "authorizes the payment request using a PIN.  The result of this process is not supposed be " +
                    "directly available to the <b>Merchant</b> since it contains potentially sensitive user data." +
                    "<p>Therefore the result is encrypted by a key supplied (as a part of the payment credential) by the " +
                    "associated <b>Bank</b> before it is returned to the <b>Merchant</b>:</p>"));
            s.append(fancyBox(debugData.walletResponse));
            s.append(description("In the indirect mode encrypted."));
            s.append(fancyBox(debugData.reserveOrDebitRequest));
            s.append(description("The following message is <i>NOT</i> exchange between the " +
                        "Wallet and Merchant but is the response from the Payment Provider " +
                        "to the the indirect mode." +
                        "<p>As can been seen the authorization is <i>digitally signed</i> by the " +
                        "Payment Provider and contains both the original Merchant payment request " +
                        "as well as a minimal set of card data.</p>"));
            s.append(fancyBox(debugData.reserveOrDebitResponse));
            HTML.debugPage(response, s.toString());
            
         } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            response.setContentType("text/plain; charset=utf-8");
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            response.getOutputStream().println("Error: " + e.getMessage());
        }
    }

    private String keyWord(String keyWord) {
        return "<code style=\"font-size:10pt\">&quot;" + keyWord + "&quot;</code>";
    }

}
