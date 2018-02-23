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

package com.zeevox.secure.cryptography;

import com.zeevox.secure.util.StringUtils;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class Entry {

    final public String key;
    final public String name, pass; // name and pass are stored encrypted

    public Entry(String k, String n, String p) throws Exception {
        key = k;
        name = n;
        pass = p;
    }

    // Constructor: de-serialize Entry from XML
    public Entry(Node node) throws Exception {
        NodeList list = node.getChildNodes();
        String k = null, n = null, p = null;
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
            }
        }
        key = k;
        name = n;
        pass = p;
    }

    public String toString() {
        return key;
    }

    // Serialize Entry to XML
    public Element save(Document document) throws Exception {
        Element docEntry = document.createElement("entry");
        Element field = document.createElement("key");
        field.setAttribute("value", key);
        docEntry.appendChild(field);
        field = document.createElement("name");
        field.setAttribute("value", StringUtils.toHex(name));
        docEntry.appendChild(field);
        field = document.createElement("pass");
        field.setAttribute("value", StringUtils.toHex(pass));
        docEntry.appendChild(field);
        return docEntry;
    }

    private String extractValue(Node n) {
        NamedNodeMap nodeMap = n.getAttributes();
        Attr attr = (Attr) nodeMap.getNamedItem("value");
        return attr.getValue();
    }
}

