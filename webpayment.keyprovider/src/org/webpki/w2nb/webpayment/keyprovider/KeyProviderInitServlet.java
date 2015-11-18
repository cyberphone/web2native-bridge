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

package org.webpki.w2nb.webpayment.keyprovider;

import java.io.IOException;

import java.util.logging.Logger;

import java.net.URLEncoder;

import javax.servlet.ServletException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.webpki.keygen2.ServerState;

import org.webpki.util.Base64URL;


public class KeyProviderInitServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static Logger log = Logger.getLogger (KeyProviderInitServlet.class.getCanonicalName());

    static final String ANDROID_WEBPKI_VERSION_TAG     = "VER";
    static final String ANDROID_WEBPKI_VERSION_MACRO   = "$VER$";  // KeyGen2 Android PoC
    
    static final String KEYGEN2_SESSION_ATTR           = "keygen2";

    static final int SUCCESS_MESSAGE = 0;
    static final int ERROR_MESSAGE = 1;
    static final int PARAM_MESSAGE = 2;
    static final int ABORT_MESSAGE = 4;
    
    static final String INIT_TAG = "init";     // Note: This is currently also a part of the KeyGen2 client!
    static final String ABORT_TAG = "abort";
    static final String PARAM_TAG = "msg";
    static final String ERROR_TAG = "err";

    static final String HTML_INIT = 
            "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0//EN\">"+
            "<html><head><link rel=\"shortcut icon\" href=\"favicon.ico\">"+
            "<meta name=\"viewport\" content=\"initial-scale=1.0\"/>" +
            "<title>Mobile Device Certificate Enrollment</title>"+
            "<style type=\"text/css\">html {overflow:auto} html, body {margin:0px;padding:0px;height:100%} "+
            "body {font-size:8pt;color:#000000;font-family:verdana,arial;background-color:white} "+
            "h2 {font-weight:bold;font-size:12pt;color:#000000;font-family:arial,verdana,helvetica} "+
            "h3 {font-weight:bold;font-size:11pt;color:#000000;font-family:arial,verdana,helvetica} "+
            "a:link {font-weight:bold;font-size:8pt;color:blue;font-family:arial,verdana;text-decoration:none} "+
            "a:visited {font-weight:bold;font-size:8pt;color:blue;font-family:arial,verdana;text-decoration:none} "+
            "a:active {font-weight:bold;font-size:8pt;color:blue;font-family:arial,verdana} "+
            "input {font-weight:normal;font-size:8pt;font-family:verdana,arial} "+
            "td {font-size:8pt;font-family:verdana,arial} "+
            ".smalltext {font-size:6pt;font-family:verdana,arial} "+
            "button {font-weight:normal;font-size:8pt;font-family:verdana,arial;padding-top:2px;padding-bottom:2px} "+
            ".headline {font-weight:bolder;font-size:10pt;font-family:arial,verdana} "+
            ".dbTR {border-width:1px 1px 1px 0;border-style:solid;border-color:black;padding:4px} "+
            ".dbTL {border-width:1px 1px 1px 1px;border-style:solid;border-color:black;padding:4px} "+
            ".dbNL {border-width:0 1px 1px 1px;border-style:solid;border-color:black;padding:4px} "+
            ".dbNR {border-width:0 1px 1px 0;border-style:solid;border-color:black;padding:4px} "+
            "</style>";

    static String getHTML(String javascript, String bodyscript, String box) {
        StringBuffer s = new StringBuffer(HTML_INIT);
        if (javascript != null) {
            s.append("<script type=\"text/javascript\">").append(javascript)
                    .append("</script>");
        }
        s.append("</head><body");
        if (bodyscript != null) {
            s.append(' ').append(bodyscript);
        }
        s.append(
                ">" +
                "<table cellapdding=\"0\" cellspacing=\"0\" width=\"100%\" height=\"100%\">")
                .append(box).append("</table></body></html>");
        return s.toString();
    }
  
    static void output(HttpServletResponse response, String html) throws IOException, ServletException {
        response.setContentType("text/html; charset=utf-8");
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(html.getBytes("UTF-8"));
    }
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        HttpSession session = request.getSession(true);
        session.setAttribute(KEYGEN2_SESSION_ATTR,
                             new ServerState(new KeyGen2SoftHSM(KeyProviderService.keyManagemenentKey)));

        ////////////////////////////////////////////////////////////////////////////////////////////
        // The following is the actual contract between an issuing server and a KeyGen2 client.
        // The "cookie" element is optional while the HTTP GET "url" argument is mandatory.
        // The "url" argument bootstraps the protocol.
        //
        // The "init" element on the bootstrap URL is a local Mobile RA convention.
        // The purpose of the random element is suppressing caching of bootstrap data.
        ////////////////////////////////////////////////////////////////////////////////////////////
        String url = "webpkiproxy://keygen2?cookie=JSESSIONID%3D" +
                     session.getId() +
                     "&url=" + URLEncoder.encode(KeyProviderService.keygen2EnrollmentUrl + "?" +
                     INIT_TAG + "=" + Base64URL.generateURLFriendlyRandom(8) +
                     (KeyProviderService.grantedVersions == null ? "" : "&" + ANDROID_WEBPKI_VERSION_TAG + "=" + ANDROID_WEBPKI_VERSION_MACRO), "UTF-8");
        output(response, 
               getHTML(null,
                       "onload=\"document.location.href='" + url + "'\"" ,
                          "<tr><td width=\"100%\" align=\"center\" valign=\"middle\"><b>Please wait while enrollment plugin starts...</b></td></tr>"));
    }
}
