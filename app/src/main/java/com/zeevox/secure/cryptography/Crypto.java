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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

import javax.crypto.BadPaddingException;

public class Crypto {
    /**
     * Tag for logging info to the logcat
     */
    private static final String TAG = "Crypto";
    private Entries mEntries;

    /**
     * Initialise Crypto
     *
     * @throws Exception since Entries() throws an Exception too
     */
    public Crypto(Context context) throws Exception {
        mEntries = new Entries(context);
    }

    /**
     * This method creates a new entry that can be saved to the user's password database.
     *
     * @param entryName   The parameter that specifies the visible, unencrypted
     *                    tag or title for the key (e.g. the website to which
     *                    these credentials are related to)
     * @param keyUsername The parameter that specifies the username of the key
     * @param keyPassword The parameter that specifies the password of the key
     * @param keyNotes    The parameter that specifies any notes related to the
     *                    key (e.g. security questions)
     * @throws Exception Throws an exception due to the fact that
     *                   Entries.addEntrySorted(Entry entry) throws an exception too.
     */
    public static Entry newEntry(@NonNull String entryName, @NonNull String keyUsername, @NonNull String keyPassword,
                                 @Nullable String keyNotes, @NonNull String masterPass) throws Exception {
        byte[] salt = new byte[8];
        new SecureRandom().nextBytes(salt);

        String notesEnc = null;
        if (keyNotes != null) {
            notesEnc = Encryptor.encrypt(
                    keyNotes,
                    masterPass.toCharArray(),
                    salt);
        }
        return new Entry(
                entryName,
                Encryptor.encrypt(keyUsername, masterPass.toCharArray(), salt),
                Encryptor.encrypt(keyPassword, masterPass.toCharArray(), salt),
                notesEnc,
                salt);
    }

    /**
     * This method checks whether a user-supplied master password is correct by
     * attempting to decrypt a random entry in the user's password database and
     * returns true if successful. Note that we rely on the BadPaddingException
     * to return an e if the password supplied is incorrect.
     *
     * @param masterPass The user-supplied master password that we will be checking.
     *                   This is entered in MainActivity.showMasterDialog
     * @return Returns whether the master password that was supplied was successfully
     * used to decrypt a random key in the database.
     */
    public boolean verifyMasterPass(String masterPass) {
        if (mEntries.isEmpty()) {
            Log.w(TAG, "New user, allowing access.");
            return true;
        }
        Entry randomEntry = mEntries.getEntryAt(ThreadLocalRandom.current().nextInt(0, mEntries.getEntries().size()));
        Entry.Decrypted result = randomEntry.unlock(masterPass.toCharArray());
        return result != null; // if the master password is incorrect Entry.unlock returns null
    }

    /**
     * The method allows direct access to mEntries
     *
     * @return Returns the value of mEntries
     */
    public Entries getEntries() {
        return mEntries;
    }

    /**
     * Gives a reference to the password database file
     *
     * @return Returns the secure.xml File.
     */
    public File getFile() {
        return mEntries.getData();
    }
}
