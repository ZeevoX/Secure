/*
 * Copyright (C) 2019 Timothy "ZeevoX" Langer
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

package com.zeevox.secure.cryptography;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.zeevox.secure.App;
import com.zeevox.secure.util.StringUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Entry {

    final public String key;
    final public String name, pass, notes; // name and pass are stored encrypted
    public byte[] salt = new byte[8];

    public Entry(String k, String n, String p, byte[] salt) {
        key = k;
        name = n;
        pass = p;
        notes = null;
        this.salt = salt;
    }

    public Entry(String k, String n, String p, String t, byte[] salt) {
        key = k;
        name = n;
        pass = p;
        notes = t;
        this.salt = salt;
    }

    // Constructor: de-serialize Entry from XML
    public Entry(Node node) {
        NodeList list = node.getChildNodes();
        String k = null, n = null, p = null, t = null;
        byte[] s = null;
        for (int i = 0; i < list.getLength(); i++) {
            Node subnode = list.item(i);
            String subNodeName = subnode.getNodeName();
            switch (subNodeName) {
                case "key":
                    if (k != null) throw new RuntimeException("key was already set");
                    k = extractValue(subnode);
                    break;
                case "name":
                    if (n != null) throw new RuntimeException("name was already set");
                    n = StringUtils.fromHex(extractValue(subnode));
                    break;
                case "pass":
                    if (p != null) throw new RuntimeException("pass was already set");
                    p = StringUtils.fromHex(extractValue(subnode));
                    break;
                case "notes":
                    if (t != null) throw new RuntimeException("notes was already set");
                    t = StringUtils.fromHex(extractValue(subnode));
                    break;
                case "salt":
                    if (s != null) throw new RuntimeException("salt was already set");
                    String value = extractValue(subnode);
                    s = value == null ? Encryptor.default_salt : StringUtils.fromHex(value).getBytes(StandardCharsets.UTF_8);
                    break;
            }
        }
        key = k;
        name = n;
        pass = p;
        notes = t;
        salt = s;
    }

    @NonNull
    public String toString() {
        return key;
    }

    // Serialize Entry to XML
    public Element save(Document document) {
        Element docEntry = document.createElement("entry");

        Element field = document.createElement("key");
        field.setAttribute("value", key);
        docEntry.appendChild(field);

        if (salt != Encryptor.default_salt && salt != null) {
            field = document.createElement("salt");
            field.setAttribute("value", StringUtils.toHex(new String(salt, StandardCharsets.UTF_8)));
            docEntry.appendChild(field);
        }

        field = document.createElement("name");
        field.setAttribute("value", StringUtils.toHex(name));
        docEntry.appendChild(field);

        field = document.createElement("pass");
        field.setAttribute("value", StringUtils.toHex(pass));
        docEntry.appendChild(field);

        if (notes != null) {
            field = document.createElement("notes");
            field.setAttribute("value", StringUtils.toHex(notes));
            docEntry.appendChild(field);
        }

        return docEntry;
    }

    private String extractValue(Node n) {
        NamedNodeMap nodeMap = n.getAttributes();
        Attr attr = (Attr) nodeMap.getNamedItem("value");
        return attr.getValue();
    }

    /**
     * Decrypt this entry for use within the application
     * @param masterKey The password database master key, used to decrypt the entry contents
     * @return an instance of Entry.Decrypted or null if an error occured
     */
    @Nullable
    public final Decrypted unlock(@NonNull char[] masterKey) {
        try {
            return new Decrypted(
                    key,
                    Encryptor.decrypt(name, masterKey, salt),
                    Encryptor.decrypt(pass, masterKey, salt),
                    Encryptor.decrypt(notes, masterKey, salt)
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static class Decrypted {
        public String key, name, pass, notes;
        public Decrypted(String key, String name, String pass, @Nullable String notes) {
            this.key = key;
            this.name = name;
            this.pass = pass;
            this.notes = notes;
        }
    }
}

