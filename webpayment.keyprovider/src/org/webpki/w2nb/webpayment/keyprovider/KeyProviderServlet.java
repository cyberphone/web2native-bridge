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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.security.interfaces.RSAPublicKey;
import java.net.URLEncoder;
import java.net.InetAddress;
import java.net.Inet4Address;
import java.net.NetworkInterface;

import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.webpki.crypto.AlgorithmPreferences;
import org.webpki.crypto.AsymSignatureAlgorithms;
import org.webpki.crypto.KeyStoreReader;
import org.webpki.crypto.CertificateUtil;
import org.webpki.crypto.KeyAlgorithms;
import org.webpki.crypto.HashAlgorithms;
import org.webpki.crypto.CustomCryptoProvider;
import org.webpki.keygen2.ServerState;
import org.webpki.keygen2.KeySpecifier;
import org.webpki.keygen2.KeyGen2URIs;
import org.webpki.keygen2.InvocationResponseDecoder;
import org.webpki.keygen2.ProvisioningInitializationResponseDecoder;
import org.webpki.keygen2.CredentialDiscoveryResponseDecoder;
import org.webpki.keygen2.KeyCreationResponseDecoder;
import org.webpki.keygen2.ProvisioningFinalizationResponseDecoder;
import org.webpki.keygen2.InvocationRequestEncoder;
import org.webpki.keygen2.ProvisioningInitializationRequestEncoder;
import org.webpki.keygen2.CredentialDiscoveryRequestEncoder;
import org.webpki.keygen2.KeyCreationRequestEncoder;
import org.webpki.keygen2.ProvisioningFinalizationRequestEncoder;
import org.webpki.sks.Grouping;
import org.webpki.sks.AppUsage;
import org.webpki.sks.PassphraseFormat;
import org.webpki.sks.PatternRestriction;
import org.webpki.sks.SecureKeyStore;
import org.webpki.util.ArrayUtil;
import org.webpki.util.Base64;
import org.webpki.util.Base64URL;
import org.webpki.util.MIMETypedObject;
import org.webpki.w2nb.webpayment.common.AccountDescriptor;
import org.webpki.w2nb.webpayment.common.BaseProperties;
import org.webpki.w2nb.webpayment.common.Encryption;
import org.webpki.webutil.ServletUtil;
import org.webpki.json.JSONDecoderCache;
import org.webpki.json.JSONEncoder;
import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;


public class KeyProviderServlet extends HttpServlet implements BaseProperties {

    private static final long serialVersionUID = 1L;

    static Logger log = Logger.getLogger (KeyProviderServlet.class.getCanonicalName());

    
    boolean debug;

    static String pin_type;                     // Optional
    
    static boolean pin_hidden;

    static int pin_retries;
    
    static int pin_min_length;
    
    static int pin_max_length;
    
    static String success_image_and_message;
    
    void returnKeyGen2Error (HttpServletResponse response, String errorMessage) throws IOException, ServletException {
        ////////////////////////////////////////////////////////////////////////////////////////////
        // Server errors are returned as HTTP redirects taking the client out of its KeyGen2 mode
        ////////////////////////////////////////////////////////////////////////////////////////////
        response.sendRedirect(KeyProviderService.keygen2EnrollmentUrl + 
                              "?" +
                              KeyProviderInitServlet.ERROR_TAG +
                              "=" +
                              URLEncoder.encode (errorMessage, "UTF-8"));
    }
    
    void keygen2JSONBody (HttpServletResponse response, JSONEncoder object) throws IOException {
        byte[] jsonData = object.serializeJSONDocument (JSONOutputFormats.PRETTY_PRINT);
        if (KeyProviderService.isDebug ()) {
            log.info ("Sent message\n" + new String (jsonData, "UTF-8"));
        }
        response.setContentType(JSON_CONTENT_TYPE);
        response.setHeader("Pragma", "No-Cache");
        response.setDateHeader("EXPIRES", 0);
        response.getOutputStream().write(jsonData);
    }

    void requestKeyGen2KeyCreation (HttpServletResponse response, ServerState keygen2State)
            throws IOException {
        ServerState.PINPolicy pinPolicy = 
            keygen2State.createPINPolicy (PassphraseFormat.NUMERIC,
                                          4,
                                          8,
                                          3,
                                          null);
        pinPolicy.setGrouping (Grouping.SHARED);
    
        for (KeyProviderService.PaymentCredential paymentCredential : KeyProviderService.paymentCredentials) {
            ServerState.Key key = keygen2State.createKey (AppUsage.SIGNATURE,
                                                          new KeySpecifier (KeyAlgorithms.NIST_P_256),
                                                          pinPolicy);
            AsymSignatureAlgorithms signAlg =
                paymentCredential.signatureKey.getPublicKey() instanceof RSAPublicKey ?
                    AsymSignatureAlgorithms.RSA_SHA256 : AsymSignatureAlgorithms.ECDSA_SHA256;
            key.setEndorsedAlgorithms(new String[]{signAlg.getAlgorithmId(AlgorithmPreferences.SKS)});
            key.setCertificatePath(paymentCredential.signatureKey.getCertificatePath());
            key.setPrivateKey(paymentCredential.signatureKey.getPrivateKey().getEncoded());

            JSONObjectWriter ow = new JSONObjectWriter()
                .setObject(BaseProperties.PAYER_ACCOUNT_JSON, 
                           new AccountDescriptor(paymentCredential.accountType,
                                                 paymentCredential.accountId).write())
                .setBoolean(BaseProperties.CARD_FORMAT_ACCOUNT_ID_JSON,
                            paymentCredential.cardFormatted)
                .setString(BaseProperties.PROVIDER_AUTHORITY_URL_JSON,
                           KeyProviderService.bankAuthorityUrl)
                .setString(BaseProperties.SIGNATURE_ALGORITHM_JSON,
                           signAlg.getAlgorithmId(AlgorithmPreferences.JOSE))
                .setObject(BaseProperties.ENCRYPTION_PARAMETERS_JSON)
                    .setString(BaseProperties.DATA_ENCRYPTION_ALGORITHM_JSON,
                               Encryption.JOSE_A128CBC_HS256_ALG_ID)
                    .setString(BaseProperties.KEY_ENCRYPTION_ALGORITHM_JSON,
                               paymentCredential.encryptionKey instanceof RSAPublicKey ?
                                   Encryption.JOSE_RSA_OAEP_256_ALG_ID 
                                                          : 
                                   Encryption.JOSE_ECDH_ES_ALG_ID)
                    .setPublicKey(paymentCredential.encryptionKey, AlgorithmPreferences.JOSE);
           key.addExtension(BaseProperties.W2NB_WEB_PAY_CONTEXT_URI,
                            ow.serializeJSONObject(JSONOutputFormats.NORMALIZED));

           key.addLogotype(KeyGen2URIs.LOGOTYPES.CARD, paymentCredential.cardImage);
        }
    
        keygen2JSONBody (response, 
                         new KeyCreationRequestEncoder (keygen2State,
                                                        KeyProviderService.keygen2EnrollmentUrl));
      }

    String certificateData (X509Certificate certificate) {
        return ", Subject='" + certificate.getSubjectX500Principal ().getName () +
               "', Serial=" + certificate.getSerialNumber ();
    }
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
           throws IOException, ServletException {
        executeRequest(request, response, null, false);
    }

    void executeRequest(HttpServletRequest request,
                        HttpServletResponse response,
                        String versionMacro,
                        boolean init)
         throws IOException, ServletException {
        String keygen2EnrollmentUrl = KeyProviderService.keygen2EnrollmentUrl;
        HttpSession session = request.getSession(false);
        try
          {
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Check that the request is properly authenticated
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (session == null) {
                throw new IOException("Session timed out");
             }
            ServerState keygen2State =
                (ServerState) session.getAttribute(KeyProviderInitServlet.KEYGEN2_SESSION_ATTR);
            if (keygen2State == null) {
                throw new IOException("Server state missing");
            }
            ////////////////////////////////////////////////////////////////////////////////////////////
            // Check if it is the first (trigger) message from the client
            ////////////////////////////////////////////////////////////////////////////////////////////
            if (init) {
                if (KeyProviderService.grantedVersions != null) {
                    boolean found = false;;
                    for (String version : KeyProviderService.grantedVersions) {
                        if (version.equals(versionMacro)) {
                            found = true;
                            break;
                          }
                    }
                    if (!found) {
                        throw new IOException("Wrong version of WebPKI, you need to update");
                    }
                }
                InvocationRequestEncoder invocationRequest =
                    new InvocationRequestEncoder(keygen2State,
                                                 keygen2EnrollmentUrl,
                                                 null);
                invocationRequest.setAbortUrl(keygen2EnrollmentUrl +
                                                  "?" +
                                                  KeyProviderInitServlet.ABORT_TAG +
                                                  "=true");
                keygen2State.addImageAttributesQuery(KeyGen2URIs.LOGOTYPES.LIST);
                keygen2JSONBody(response, invocationRequest);
                session.setAttribute(KeyProviderInitServlet.KEYGEN2_SESSION_ATTR, keygen2State);
                return;
              }

            ////////////////////////////////////////////////////////////////////////////////////////////
            // It should be a genuine KeyGen2 response.  Note that the order is verified!
            ////////////////////////////////////////////////////////////////////////////////////////////
            byte[] jsonData = ServletUtil.getData(request);
            if (!request.getContentType().equals(JSON_CONTENT_TYPE)) {
                throw new IOException("Wrong \"Content-Type\": " + request.getContentType());
            }
            if (KeyProviderService.isDebug()) {
                log.info("Received message:\n" + new String(jsonData, "UTF-8"));
            }
            JSONDecoder jsonObject = KeyProviderService.keygen2JSONCache.parse (jsonData);
            switch (keygen2State.getProtocolPhase ())
              {
                case INVOCATION:
                  InvocationResponseDecoder invocation_response = (InvocationResponseDecoder) jsonObject;
                  keygen2State.update (invocation_response);

                  // Now we really start doing something
                  ProvisioningInitializationRequestEncoder provisioningInitRequest =
                      new ProvisioningInitializationRequestEncoder(keygen2State,
                                                                   keygen2EnrollmentUrl,
                                                                   1000,
                                                                   (short)50);
                  provisioningInitRequest.setKeyManagementKey(
                          KeyProviderService.keyManagemenentKey.getPublicKey ());
                  keygen2JSONBody (response, provisioningInitRequest);
                  break;

                case PROVISIONING_INITIALIZATION:
                  ProvisioningInitializationResponseDecoder provisioning_init_response = (ProvisioningInitializationResponseDecoder) jsonObject;
                  keygen2State.update (provisioning_init_response, getTLSCertificate ());

                  log.info("Device Certificate=" + certificateData (keygen2State.getDeviceCertificate ()));
                  String expected_dev_id = enrollment_object.getOptionalAuthData ();
                  if (expected_dev_id != null)
                    {
                      String actual_dev_id = keygen2State.getDeviceIDString (keygen2_config.useLongDeviceID ());
                      if (expected_dev_id.equals (actual_dev_id))
                        {
                          log.info ("KeyGen2, Enrollment ID=" + enrollment_object.getEnrollID () + ", Device ID=" + actual_dev_id + " Successfully Authenticated");
                        }
                      else
                        {
                          log.info ("KeyGen2 failure, Enrollment ID=" + enrollment_object.getEnrollID () +
                                    ", Device ID mismatch, Expected=" + expected_dev_id + ", Received=" + actual_dev_id);
                          enrollment_object.setEnrollmentStatus (EnrollmentStatus.ABORTED);
                          return returnKeyGen2Error ("Expected Device ID:\n" + expected_dev_id + "\n\nReceived Device ID:\n" + actual_dev_id);
                        }
                    }

                  if (keygen2_config.getKeyManagementPublicKey () != null && keygen2_config.isUpdateMode ())
                    {
                      CredentialDiscoveryRequestEncoder cred_disc_request =
                          new CredentialDiscoveryRequestEncoder (keygen2State, keygen2EnrollmentUrl);
                      cred_disc_request.addLookupDescriptor (keygen2_config.getKeyManagementPublicKey ());
                      return keygen2JSONBody (cred_disc_request);
                    }
                  requestKeyGen2KeyCreation (response, keygen2State);
                  break;

                case CREDENTIAL_DISCOVERY:
                  CredentialDiscoveryResponseDecoder cred_disc_response = (CredentialDiscoveryResponseDecoder) jsonObject;
                  keygen2State.update (cred_disc_response);
                  for (CredentialDiscoveryResponseDecoder.LookupResult lookup_result : cred_disc_response.getLookupResults ())
                    {
                      for (CredentialDiscoveryResponseDecoder.MatchingCredential matching_credential : lookup_result.getMatchingCredentials ())
                        {
                          X509Certificate end_entity_certificate =  matching_credential.getCertificatePath ()[0];
                          keygen2State.addPostDeleteKey (matching_credential.getClientSessionId (), 
                                                          matching_credential.getServerSessionId (),
                                                          end_entity_certificate,
                                                          keygen2_config.getKeyManagementPublicKey ());
                          log.info ("Deleting key, Enrollment ID=" + enrollment_object.getEnrollID () + certificateData (end_entity_certificate));
                        }
                    }
                  requestKeyGen2KeyCreation (response, keygen2State);
                  break;

                case KEY_CREATION:
                  KeyCreationResponseDecoder key_creation_response = (KeyCreationResponseDecoder) jsonObject;
                  keygen2State.update (key_creation_response);
                  Iterator <KeyGen2Config.KeyConfiguration> key_config_iterator = keygen2_config.getConfiguredKeys ().iterator ();
                  for (ServerState.Key key : keygen2State.getKeys ())
                    {
                      KeyGen2Config.KeyConfiguration key_config = key_config_iterator.next ();
                      UserData user_data = enrollment_object.getUserData ().clone ();
                      user_data.optionallySetCAName (key_config.getCAName ())
                               .optionallySetCertificateProfile (key_config.getCertificateProfile ())
                               .optionallySetEndEntityProfile (key_config.getEndEntityProfile ());
                      SimpleCertificateResponse cert_response =
                          (SimpleCertificateResponse)ca_service.processCall (new SimpleCertificateRequest (user_data, key.getPublicKey ()));
                      if (cert_response.getError () != null)
                        {
                          return returnKeyGen2Error (cert_response.getError ());
                        }
                      X509Certificate[] certificate_path = CertificateUtil.getSortedPathFromBlobs (cert_response.getCertificatePath ());
                      key.setCertificatePath (certificate_path);
                      enrollment_object.addIssuedCertificate (certificate_path[0]);
                      for (KeyGen2Config.Extension extension : key_config.getExtensions ())
                        {
                          key.addExtension (extension.type, extension.data);
                        }
                      for (KeyGen2Config.EncryptedExtension encrypted_extension : key_config.getEncryptedExtensions ())
                        {
                          key.addEncryptedExtension (encrypted_extension.type, encrypted_extension.data);
                        }
                      HashMap<String,Vector<KeyGen2Config.Property>> property_bags = key_config.getPropertyBags ();
                      for (String type : property_bags.keySet ())
                        {
                          ServerState.PropertyBag property_bag = key.addPropertyBag (type);
                          for (KeyGen2Config.Property property : property_bags.get (type))
                            {
                              property_bag.addProperty (property.name, property.value, property.writable); 
                            }
                        }
                      Vector<KeyGen2Config.Logotype> logotypes = key_config.getLogotypes (KeyGen2URIs.LOGOTYPES.LIST);
                      ServerState.ImagePreference image_pref = keygen2State.getImagePreference (KeyGen2URIs.LOGOTYPES.LIST);
                      if (image_pref != null && logotypes != null)
                        {
                          log.info ("Image preferences, Type=" + image_pref.getType () + 
                              ", Mime=" + image_pref.getMimeType () +
                              ", Width=" + image_pref.getWidth () +
                              ", Height=" + image_pref.getHeight ());
                          boolean not_found = true;
                          for (KeyGen2Config.Logotype logotype : logotypes)
                            {
                              if (logotype.match (image_pref))
                                {
                                  log.info ("Logotype added, Match=true, " + logotype.toString ());
                                  key.addLogotype (KeyGen2URIs.LOGOTYPES.LIST, logotype);
                                  not_found = false;
                                  break;
                                }
                            }
                          if (not_found)
                            {
                              for (KeyGen2Config.Logotype logotype : logotypes)
                                {
                                  if (logotype.getMimeType ().equals ("image/png"))
                                    {
                                      log.info ("Logotype added, Match=false, " + logotype.toString ());
                                      key.addLogotype (KeyGen2URIs.LOGOTYPES.LIST, logotype);
                                      break;
                                    }
                                }
                            }
                        }
                    }

                  keygen2JSONBody (response,
                                   new ProvisioningFinalizationRequestEncoder (keygen2State,
                                                                               keygen2EnrollmentUrl));
                  break;

                case PROVISIONING_FINALIZATION:
                  ProvisioningFinalizationResponseDecoder prov_final_response = (ProvisioningFinalizationResponseDecoder) jsonObject;
                  keygen2State.update (prov_final_response);
                  log.info ("Successful KeyGen2 run, Enrollment ID=" + enrollment_object.getEnrollID () + ", Device ID=" + keygen2State.getDeviceIDString (keygen2_config.useLongDeviceID ()));

                  ////////////////////////////////////////////////////////////////////////////////////////////
                  // We are done, return an HTTP redirect taking the client out of its KeyGen2 mode
                  ////////////////////////////////////////////////////////////////////////////////////////////
                  response.sendRedirect(keygen2EnrollmentUrl);
                  return;

                default:
                  throw new IOException ("Unxepected state");
              }
          }
        catch (Exception e)
          {
            log.log(Level.SEVERE, "KeyGen2 failure", e);
            try
              {
                ByteArrayOutputStream baos = new ByteArrayOutputStream ();
                PrintWriter printer_writer = new PrintWriter (baos);
                e.printStackTrace (printer_writer);
                printer_writer.flush ();
                return returnKeyGen2Error (baos.toString ("UTF-8"));
              }
            catch (IOException iox)
              {
                return returnKeyGen2Error ("This shouldn't happen, EVER!");
              }
          }
      }


    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
           throws IOException, ServletException {
        if (request.getParameter(KeyProviderInitServlet.INIT_TAG) != null) {
            executeRequest(request,
                           response,
                           request.getParameter(KeyProviderInitServlet.ANDROID_WEBPKI_VERSION_TAG),
                           true);
            return;
        }
        StringBuffer html = new StringBuffer ("<tr><td width=\"100%\" align=\"center\" valign=\"middle\">");
        switch (request_object.getMessageType ()) {
            case KeyProviderInitServlet.SUCCESS_MESSAGE:
              html.append (success_image_and_message);
              break;

            case KeyProviderInitServlet.ABORT_MESSAGE:
              log.info ("KeyGen2 run aborted by the user, Enrollment ID=" + request_object.getStringData ());
              html.append ("<b>Aborted by the user!</b>");
              break;

            case KeyProviderInitServlet.PARAM_MESSAGE:
              html.append (request_object.getStringData ());
              break;

            case KeyProviderInitServlet.ERROR_MESSAGE:
              html.append ("<table><tr><td><b>Failure Report:</b></td></tr><tr><td><pre><font color=\"red\">")
                  .append (request_object.getStringData ())
                  .append ("</font></pre></td></tr></table>");
              break;

            default:
              html.append ("<b>This shouldn't happen, EVER...</b>");
        }
        output(response, getHTML (null, null, html.append ("</td></tr>").toString ()));
    }

}
