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
package org.webpki.w2nb.webpayment.common;

import java.io.IOException;

import org.webpki.json.JSONObjectReader;
import org.webpki.json.JSONObjectWriter;

public class Software implements BaseProperties {

    String id;
    String version;
    
    public Software (JSONObjectReader rd) throws IOException {
        rd = rd.getObject(SOFTWARE_JSON);
        id = rd.getString(ID_JSON);
        version = rd.getString(VERSION_JSON);
    }

    public String getSoftwareId() {
        return id;
    }

    public String getSoftwareVersion() {
        return version;
    }

    public static JSONObjectWriter encode(String id, String version) throws IOException {
        return new JSONObjectWriter()
            .setString(ID_JSON, id)
            .setString(VERSION_JSON, version);
    }
}
