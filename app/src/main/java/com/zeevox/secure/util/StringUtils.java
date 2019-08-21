/*
 * Copyright (C) 2018 Timothy "ZeevoX" Langer
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.zeevox.secure.util;

public class StringUtils {

    // NB: due to the fact that not always "new String (s.getBytes()).equals (s)"
    // I had to resort to these conversion routines:
    public static byte[] str2bytes (String s) {
        byte[] bytes = new byte[s.length() * 2];
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt (i);
            // little-endian
            bytes[2 * i] = (byte) (ch & 0xFF);
            bytes[2 * i + 1] = (byte) (ch >> 8);
        }
        return bytes;
    }

    public static String bytes2str (byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        // NB: & 0xFF is necessary to get rid of the sign extension
        for (int i = 0; i < bytes.length; i += 2) {
            char ch = (char) ((bytes[i + 1] & 0xFF) << 8 | (bytes[i] & 0xFF));
            sb.append (ch);
        }
        return sb.toString();
    }

    public static String toHex(String s) throws Exception {
        StringBuilder sb = new StringBuilder();
        byte[] bytes = StringUtils.str2bytes(s);
        for (byte aByte : bytes) {
            // NB: & 0xFF to strip the sign bit extension
            String h = Integer.toHexString(aByte & 0xFF);
            if (h.length() < 2) {
                sb.append("0");
            }
            sb.append(h.toUpperCase());
        }
        return sb.toString();
    }

    public static String fromHex(String s) throws Exception {
        int n = s.length();
        byte[] bytes = new byte[n / 2];
        for (int i = 0; i < n; i += 2) {
            String h = s.substring(i, i + 2);
            bytes[i / 2] = Integer.valueOf(h, 16).byteValue();
        }
        return StringUtils.bytes2str(bytes);
    }
}
