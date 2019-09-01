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

import android.content.Context;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

public class Entries {

    // for xml
    public static final String FILENAME = "secure.xml";

    private final File data;
    private final ArrayList<Entry> entries = new ArrayList<>();
    private DocumentBuilder documentBuilder;

    public Entries(Context context) throws Exception {
        data = new File(context.getFilesDir(), FILENAME);
        documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        if (!data.exists()) {
            save();
        }
        load();
    }

    private static boolean lessThanEQ(Entry a, Entry b) {
        return a.key.compareToIgnoreCase(b.key) <= 0;
    }

    public ArrayList<Entry> getEntries() {
        return entries;
    }

    public Entry getEntryAt(int ind) {
        return entries.get(ind);
    }

    private int addEntrySortedNoSave(Entry e) {
        int i = 0;
        int n = entries.size();
        while (i < n &&
                !lessThanEQ(e, getEntryAt(i))) {
            i++;
        }
        if (i < n && lessThanEQ(getEntryAt(i), e))
            throw new RuntimeException("Duplicate key '" + e.key + "'");
        entries.add(i, e);
        return i;
    }

    public int addEntrySorted(Entry e) throws FileNotFoundException, TransformerException {
        int pos = addEntrySortedNoSave(e);
        save();
        return pos;
    }

    public void removeEntryAt(int ind) throws FileNotFoundException, TransformerException {
        entries.remove(ind);
        save();
    }

    public void replaceEntryAt(int ind, Entry e) throws FileNotFoundException, TransformerException {
        entries.remove(ind);
        addEntrySortedNoSave(e);
        save();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public File getData() {
        return data;
    }

    private void load() throws IOException, SAXException {
        Document document = documentBuilder.parse(data);
        NodeList list = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            addEntrySortedNoSave(new Entry(list.item(i)));
        }
    }

    private void save() throws FileNotFoundException, TransformerException {
        // build the document
        Document document = documentBuilder.newDocument();
        Element root = document.createElement("entries");
        document.appendChild(root);
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            root.appendChild(entry.save(document));
        }

        DOMSource source = new DOMSource(document);
        PrintStream out = new PrintStream(new BufferedOutputStream
                (new FileOutputStream(data)));
        StreamResult result = new StreamResult(out);
        Transformer transformer = TransformerFactory.newInstance().
                newTransformer();
        transformer.transform(source, result);
        out.close();
    }
}