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

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;
import org.webpki.json.JSONParser;

import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.CardTypes;
import org.webpki.w2nb.webpayment.common.Messages;
import org.webpki.w2nb.webpayment.common.PaymentRequest;

public class CheckoutServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;
    
    static final String REQUEST_HASH_ATTR = "REQHASH";
    
    static Logger logger = Logger.getLogger(CheckoutServlet.class.getName());
    
    static int nextReferenceId = 1000000;
    
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        JSONArrayReader ar = JSONParser.parse(request.getParameter("shoppingCart")).getJSONArrayReader();
        SavedShoppingCart savedShoppingCart = new SavedShoppingCart();
        int total = 0;
        while (ar.hasMore()) {
            JSONObjectReader or = ar.getObject();
            int units = or.getInt("units");
            if (units != 0) {
                String sku = or.getString("sku");
                savedShoppingCart.items.put(sku, units);
                logger.info("SKU=" + sku + " Units=" + units);
                total += units * or.getInt("priceX100");
            }
        }
        savedShoppingCart.total = total;
        request.getSession(true).setAttribute(SavedShoppingCart.SAVED_SHOPPING_CART, savedShoppingCart);

        JSONObjectWriter paymentRequest = PaymentRequest.encode("Demo Merchant",
                                                                new BigDecimal(BigInteger.valueOf(total), 2),
                                                                MerchantService.currency,
                                                                "#" + (nextReferenceId++),
                                                                MerchantService.merchantKey);

        request.getSession(true).setAttribute(REQUEST_HASH_ATTR, PaymentRequest.getRequestHash(paymentRequest));

        Vector<String> acceptedCards = new Vector<String>();
        for (CardTypes card : MerchantService.acceptedCards) {
            acceptedCards.add(card.toString());
        }
        acceptedCards.add("NoSuchCard");
        JSONObjectWriter invokeRequest = Messages.createBaseMessage(Messages.INVOKE_WALLET)
            .setStringArray(ACCEPTED_CARD_TYPES_JSON, acceptedCards.toArray(new String[0]))
            .setBoolean(PULL_PAYMENT_JSON, false)
            .setObject(PAYMENT_REQUEST_JSON, paymentRequest);
        
        HTML.checkoutPage(response,
                          savedShoppingCart,
                          new String(invokeRequest.serializeJSONObject(JSONOutputFormats.JS_NATIVE), "UTF-8"));
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        response.sendRedirect("home");
    }
}
