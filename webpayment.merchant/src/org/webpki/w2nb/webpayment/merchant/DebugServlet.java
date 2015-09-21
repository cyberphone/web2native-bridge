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
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nb.webpayment.common.Version;

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

    class Point {
        int i = 1;
        public String toString() {
            return "<div id=\"p" + i + "\" class=\"point\">" + (i++) + "</div>";
        }
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
            Point point = new Point();
            
            s.append(description("<p>The following page shows the messages exchanged between the " +
                                 "<b>Merchant</b> (Payee), the <b>Wallet</b> (Payer), and the user's <b>Bank</b> (Payment provider).&nbsp;&nbsp;" +
                                 "For traditional card payments there is also an <b>Acquirer</b> (aka &quot;card processor&quot;) involved.</p><p>Current mode: <i>" +
                                 (debugData.acquirerMode ? "Card payment" : "Account-2-Account payment using " + (debugData.directDebit ? "direct debit" : "reserve+finalize")) +
                                 "</i></p>" +
                                 point+
                                 "<p>The user performs &quot;Checkout&quot; (after <i>optionally</i> selecting payment method), " +
                                 "causing the <b>Merchant</b> server returning a " +
                                 "<b>Wallet</b> invocation Web-page featuring a call to the " +
                                 keyWord("navigator.nativeConnect()") + 
                                 " <a target=\"_blank\" href=\"https://github.com/cyberphone/web2native-bridge#api\">[CONNECT]</a> browser API.</p>" +
                                 point +
                                 "<p>Then the invoking Web-page waits for a ready signal from the <b>Wallet</b>:</p>"));
            s.append(fancyBox(debugData.WalletInitialized));
 
            s.append(descriptionStdMargin("The " + keyWord(WINDOW_JSON) + " object provides the invoking <b>Merchant</b> Web-page with the size "+
                                 "of the <b>Wallet</b> which is used to adapt the Web-page so that this <i>external</i> " +
                                 "application does not hide important buyer information.&nbsp;&nbsp;Note: The Web2Native Bridge " +
                                 "invocation provides additional (here not shown) information to make positioning and alignment feasible.<p>" + 
                                 point +
                                 "</p><p>When the message above has been received the <b>Merchant</b> Web-page sends a " +
                                 "list of accepted account types (aka payment instruments) and a <i>signed</i> " + 
                                 "<a target=\"_blank\" href=\"https://cyberphone.github.io/openkeystore/resources/docs/jcs.html\">[SIGNATURE]</a> " +
                                 keyWord(PAYMENT_REQUEST_JSON) + " to the <b>Wallet</b>:</p>"));
            s.append(fancyBox(debugData.InvokeWallet));

            s.append(description(point +
                    "<p>After an <i>optional</i> selection of account (card) in the <b>Wallet</b> UI, the user " +
                    "authorizes the payment request (typically using a PIN):</p>" +
                    "<img style=\"display:block;margin-left:auto;margin-right:auto;height:33%;width:33%\" src=\"" +
                    (debugData.acquirerMode ? MerchantService.wallet_supercard_auth : MerchantService.wallet_bankdirect_auth) + 
                    "\"><p>" +
                    point +
                    "</p><p>The result of this process is not supposed be " +
                    "directly available to the <b>Merchant</b> since it contains potentially sensitive user data.&nbsp;&nbsp;" +
                    "For an example turn to <a href=\"#secretdata\">Unecrypted User Authorization</a>.</p><p>" +
                    point +
                    "</p><p>Therefore the result is <i>encrypted</i> (using a key supplied by the <b>Bank</b> as a part of the " +
                    "payment credential) before it is returned to the <b>Merchant</b>:</p>"));
            s.append(fancyBox(debugData.walletResponse));
            s.append(description(point +
                    "<p>After receiving the <b>Wallet</b> response, the <b>Merchant</b> uses the supplied " +
                     keyWord(PROVIDER_AUTHORITY_URL_JSON) + " to retrieve the associated " + keyWord(Messages.AUTHORITY.toString()) +
                     " object of the <b>Bank</b> claimed to be the user's account holder for the selected card:</p>"));
            s.append(fancyBox(debugData.providerAuthority));
            s.append(descriptionStdMargin("An " + keyWord(Messages.AUTHORITY.toString()) + 
                    " is a long-lived object that typically would be <i>cached</i>.&nbsp;&nbsp;It " +
                    "has the following tasks:<ul>" +
                    "<li>Provide credentials of an entity allowing relying parties verifying such before interacting with the entity</li>" +
                    "<li>Through a signature attest the authenticy of " +
                    keyWord(AUTHORITY_URL_JSON) + " and " + keyWord(TRANSACTION_URL_JSON) + "</li>" +
                    "<li>Publish and attest the entity's current encryption key and parameters</li></ul>" +
                    point +
                    "<p>Now the <b>Merchant</b> creates a <i>signed</i> request and sends it to the " + keyWord(TRANSACTION_URL_JSON) +
                    " extracted from the " + keyWord(Messages.AUTHORITY.toString()) + " object.&nbsp;&nbsp;" +
                    "Since the <b>Wallet</b> response is encrypted, the <b>Merchant</b> needs to prove to the <b>Bank</b> " +
                    "that it knows the embedded " + keyWord(PAYMENT_REQUEST_JSON) + " which it does through the " + keyWord(REQUEST_HASH_JSON) +
                    " construct.&nbsp;&nbsp;Since this particular session was " + (debugData.acquirerMode ? "a card transaction, a pre-configured " + 
                    keyWord(ACQUIRER_AUTHORITY_URL_JSON) : "an account-2-account transaction, " +
                    keyWord(PAYEE_ACCOUNTS_JSON) + "holding an array [1..n] of <b>Merchant</b> receiver accounts") + " is also supplied:</p>"));
            s.append(fancyBox(debugData.reserveOrDebitRequest));
            if (debugData.acquirerMode) {
                s.append(description(point +
                                     "<p>In the <b>Acquirer</b> mode the received " + keyWord(ACQUIRER_AUTHORITY_URL_JSON) + " is used by the <b>Bank</b> " +
                                     "to retrieve the designated card processor's encryption keys:</p>"));
                s.append(fancyBox(debugData.acquirerAuthority));
            }
            s.append(description("<p>After retrieving the <a href=\"#secretdata\">Unecrypted User Authorization</a>, " +
                    "the called <b>Bank</b> invokes the local payment backend (to verify the account, check funds, etc.) " +
                    "<i>which is outside of this specification and implementation</i>.</p><p>" +
                    point +
                    "</p><p>" + (debugData.softError? "The operation failed causing the <b>Bank</b> to return a standardized error code.&nbsp;&nbsp;This " +
                    "response is <i>unsigned</i> since the associated request is assumed to be <i>rolled-back</i>.":
                    "If the operation is successful, the <b>Bank</b> responds with a <i>signed</i> message containing both the original <b>Merchant</b> " +
                    keyWord(PAYMENT_REQUEST_JSON) + " as well as a minimal set of user account data.</p>" +
                    (debugData.acquirerMode ?
                               "<p>Also note the inclusion of " +
                               keyWord(PROTECTED_ACCOUNT_DATA_JSON) + " which only the <b>Acquirer</b> can decrypt.</p>" :
                               "<p>Also note the inclusion of the (by the <b>Bank</b>) selected <b>Merchant</b> receiver account (" +
                               keyWord(PAYEE_ACCOUNT_JSON) + ").</p>") +
                               (debugData.directDebit? "This is the final interaction in the direct debit mode.":""))));
            s.append(fancyBox(debugData.reserveOrDebitResponse));
            if (!debugData.softError) {
               if (!debugData.directDebit) {
                    s.append(description(point +
                             "<p>For finalization of the payment, the <b>Merchant</b> sets an " + keyWord(AMOUNT_JSON) + 
                             " which must be <i>equal or lower</i> than in the original request, <i>counter-signs</i> the request, " +
                             "and sends it to the " + (debugData.acquirerMode ? keyWord(TRANSACTION_URL_JSON) +
                             " retrievable from the <b>Acquirer</b> " + keyWord(Messages.AUTHORITY.toString()) + " object:" :
                             "<b>Bank</b> again:</p>")));
                    s.append(fancyBox(debugData.finalizeRequest));
                    String finalDescription = null;
                    if (debugData.acquirerMode) {
                        s.append(descriptionStdMargin("After receiving the request, the " +
                                 keyWord(PROTECTED_ACCOUNT_DATA_JSON) + " object is <i>decrypted</i>.&nbsp;&nbsp;" +
                                "This mechanism effectively replaces a <b>Merchant</b>-based &quot;tokenization&quot; scheme with the added advantage "+
                                "that the <b>Acquirer</b> also can be included in a protection model by " +
                                "for example randomizing CCVs per request (&quot;upstreams tokenization&quot;).<p>" +
                                point +
                                "</p><p>The following printout " +
                                "shows a <i>sample</i> of protected account data:</p>"));
                        s.append(fancyBox(MerchantService.protected_account_data));
                        
                        finalDescription = "<p>After this step the card network is invoked <i>which is outside of this specification and implementation</i>.</p>";
                    } else {
                        finalDescription = "<p>After receiving the request, the actual payment operation is performed " +
                                "<i>which is outside of this specification and implementation</i>.</p>";
                    }
                    s.append(descriptionStdMargin(finalDescription + 
                             point + "<p>If the operation is successful, a <i>signed</i> hash of the request is returned:</p>"));
                    s.append(fancyBox(debugData.finalizeResponse));
                    s.append(descriptionStdMargin("Not embedding the request in the response may appear illogical but since<ul>" +
                            "<li>the communication is assumed to be <i>synchronous</i> (using HTTP)</li>" +
                            "<li>there is no additional information needed by the transaction, only a sender-unique " +
                            keyWord(REFERENCE_ID_JSON) +
                            "</li><li>the 256-bit hash + signature fully bind the response to the request</li></ul>this would not add any security, " +
                            "assuming that logging is working."));
                }
            }
            s.append(description("<p id=\"secretdata\" style=\"text-align:center;font-weight:bold;font-size:10pt;font-family:" + HTML.FONT_ARIAL + "\">Unencrypted User Authorization</p>" +
                     "The following printout shows a sample of <i>internal</i> <b>Wallet</b> data <i>before</i> it is encrypted.&nbsp;&nbsp;As you can see it contains " +
                     "user account data and identity which <i>usually</i> is of no importance for the <b>Merchant</b>:"));
            s.append(fancyBox(MerchantService.user_authorization));
            s.append(descriptionStdMargin("Protocol version: <i>" + Version.PROTOCOL + "</i><br>Date: <i>" + Version.DATE + "</i>"));
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
