/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
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

package org.webpki.w2nbproxy;

import java.io.IOException;

import org.webpki.json.JSONObjectWriter;
import org.webpki.json.JSONOutputFormats;

public class StdoutJSONPipe {

    public String writeJSONObject (JSONObjectWriter ow) throws IOException {
        byte[] utf8 = ow.serializeToBytes(JSONOutputFormats.NORMALIZED);
        int l = utf8.length;
        // Code only works for little-endian machines
        // Network order, heard of that Google?
        byte[] blob = new byte[l + 4];
        blob[0] = (byte) l;
        blob[1] = (byte) (l >>> 8);
        blob[2] = (byte) (l >>> 16);
        blob[3] = (byte) (l >>> 24);
        for (int i = 0; i < l; i++) {
            blob[4 + i] = utf8[i];
        }
        System.out.write(blob);
        return new String(utf8, "UTF-8");
    }

}
