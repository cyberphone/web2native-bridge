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

import java.math.BigDecimal;
import java.math.BigInteger;

import java.util.Vector;

import java.util.logging.Logger;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.CardTypes;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nb.webpayment.common.PaymentRequest;

public class UserPaymentServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static final String REQUEST_HASH_SESSION_ATTR  = "REQHASH";
    static final String REQUEST_REFID_SESSION_ATTR = "REQREFID";
    static final String DEBUG_DATA_SESSION_ATTR    = "DBGDATA";
    static final String SHOPPING_CART_SESSION_ATTR = "SHOPCART";

    
    static final String AUTHREQ_FORM_ATTR          = "authreq";
    static final String INITMSG_FORM_ATTR          = "initmsg";
    static final String SHOPPING_CART_FORM_ATTR    = "shoppingCart";
    
    static Logger logger = Logger.getLogger(UserPaymentServlet.class.getName());
    
    static int nextReferenceId = 1000000;
    
    static boolean getOption(HttpSession session, String name) {
        return session.getAttribute(name) != null && (Boolean)session.getAttribute(name);
    }
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONArrayReader ar = JSONParser.parse(request.getParameter(SHOPPING_CART_FORM_ATTR)).getJSONArrayReader();
        SavedShoppingCart savedShoppingCart = new SavedShoppingCart();
        long total = 0;
        while (ar.hasMore()) {
            JSONObjectReader or = ar.getObject();
            int units = or.getInt("units");
            if (units != 0) {
                String sku = or.getString("sku");
                savedShoppingCart.items.put(sku, units);
                logger.info("SKU=" + sku + " Units=" + units);
                total += units * or.getLong("priceX100");
            }
        }
        savedShoppingCart.total = total;

        // We add a fictitious 10% sales tax as well
        savedShoppingCart.tax = total / 10;

        // Then we round up to the nearest 25 centimes, cents, or pennies
        savedShoppingCart.roundedPaymentAmount = ((savedShoppingCart.tax + total + 24) / 25) * 25;
        HttpSession session = request.getSession(true);
        boolean pullPaymentMode = getOption(session, HomeServlet.PULL_SESSION_ATTR);
        boolean debugMode = getOption(session, HomeServlet.DEBUG_SESSION_ATTR);
        DebugData debugData = null;
        if (debugMode) {
            session.setAttribute(DEBUG_DATA_SESSION_ATTR, debugData = new DebugData());
        }
        session.setAttribute(SHOPPING_CART_SESSION_ATTR, savedShoppingCart);

        String referenceID = "#" + (nextReferenceId++);
        JSONObjectWriter paymentRequest =
                PaymentRequest.encode("Demo Merchant",
                                      new BigDecimal(BigInteger.valueOf(savedShoppingCart.roundedPaymentAmount), 2),
                                      MerchantService.currency,
                                      referenceID,
                                      MerchantService.merchantKey);

        session.setAttribute(REQUEST_HASH_SESSION_ATTR, PaymentRequest.getRequestHash(paymentRequest));

        // Only used in pull mode
        session.setAttribute(REQUEST_REFID_SESSION_ATTR, referenceID);

        Vector<String> acceptedCards = new Vector<String>();
        for (CardTypes card : MerchantService.acceptedCards) {
            acceptedCards.add(card.toString());
        }
        acceptedCards.add("NoSuchCard");
        JSONObjectWriter invokeRequest = Messages.createBaseMessage(Messages.INVOKE_WALLET)
            .setStringArray(ACCEPTED_CARD_TYPES_JSON, acceptedCards.toArray(new String[0]))
            .setBoolean(PULL_PAYMENT_JSON, pullPaymentMode)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest);
   
        if (debugMode) {
            debugData.paymentRequest = invokeRequest.serializeJSONObject(JSONOutputFormats.NORMALIZED);
        }
        
        HTML.userPayPage(response,
                         savedShoppingCart,
                         pullPaymentMode,
                         debugMode,
                         new String(invokeRequest.serializeJSONObject(JSONOutputFormats.JS_NATIVE), "UTF-8"));
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
