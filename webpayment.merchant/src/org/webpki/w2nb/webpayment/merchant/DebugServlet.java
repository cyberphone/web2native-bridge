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

public class DebugServlet extends HttpServlet {

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
                                 "Merchant (Payee) the Wallet (Payer).</p>" +
                                 "In both modes (direct and indirect) the Wallet sends (after invocation by the " +
                                 "merchant's checkout page) a &quot;ready&quot; signal that also holds the " +
                                 "size of the payment application window which the demo payment application " +
                                 "uses to adapt the the layout so that the Wallet doesn't cover the " +
                                 "order summary."));
            s.append(fancyBox(debugData.initMessage));
 
            s.append(description("In both modes (direct and indirect) the Merchant now responds with a " +
                                 "list of accepted card types and a <i>digitally signed</i> payment request." +
                                 "<p>The mode is also supplied since this governs what the Wallet should do " +
                                 "with the payment request.</p>"));
            s.append(fancyBox(debugData.paymentRequest));

            s.append(description("In the indirect mode encrypted."));
            s.append(fancyBox(debugData.walletResponse));
            s.append(description("In the indirect mode encrypted."));
            s.append(fancyBox(debugData.bankReserveFundsRequest));
            s.append(description("The following message is <i>NOT</i> exchange between the " +
                        "Wallet and Merchant but is the response from the Payment Provider " +
                        "to the the indirect mode." +
                        "<p>As can been seen the authorization is <i>digitally signed</i> by the " +
                        "Payment Provider and contains both the original Merchant payment request " +
                        "as well as a minimal set of card data.</p>"));
            s.append(fancyBox(debugData.bankReserveFundsResponse));
            HTML.debugPage(response, s.toString());
            
         } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            response.setContentType("text/plain; charset=utf-8");
            response.setHeader("Pragma", "No-Cache");
            response.setDateHeader("EXPIRES", 0);
            response.getOutputStream().println("Error: " + e.getMessage());
        }
    }

}
