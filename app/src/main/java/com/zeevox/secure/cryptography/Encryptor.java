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

import androidx.annotation.Nullable;

import com.zeevox.secure.util.StringUtils;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

public class Encryptor {

    // configuration parameters
    private static final String algorithm = "PBEWithMD5AndDES"; // PKCS #5

    // state
    private static PBEParameterSpec pbeParamSpec;
    private static SecretKeyFactory secretKeyFactory;
    private static Cipher cipher;
    private static boolean initialized = false;

    public final static byte[] default_salt = new byte[]{
            (byte) 0xc7, (byte) 0x73, (byte) 0x21, (byte) 0x8c,
            (byte) 0x7e, (byte) 0xc8, (byte) 0xee, (byte) 0x99
    };

    private static void initialize(@Nullable byte[] salt) throws NoSuchPaddingException, NoSuchAlgorithmException {
        if (!initialized) {
            if (salt == null) salt = default_salt;

            pbeParamSpec = new PBEParameterSpec(salt, 20);
            secretKeyFactory = SecretKeyFactory.getInstance(algorithm);
            cipher = Cipher.getInstance(algorithm);
            initialized = true;
        }
    }

    private static SecretKey getSecretKey(char[] password) throws InvalidKeySpecException {
        return secretKeyFactory.generateSecret(new PBEKeySpec(password));
    }

    public static String encrypt(String s, char[] password, @Nullable byte[] salt) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        initialize(salt);
        cipher.init(Cipher.ENCRYPT_MODE,
                getSecretKey(password),
                pbeParamSpec);
        return StringUtils.bytes2str(cipher.doFinal(StringUtils.str2bytes(s)));
    }

    public static String decrypt(String c, char[] password, @Nullable byte[] salt) throws BadPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (c == null) return null;
        initialize(salt);
        cipher.init(Cipher.DECRYPT_MODE,
                getSecretKey(password),
                pbeParamSpec);
        return StringUtils.bytes2str(cipher.doFinal(StringUtils.str2bytes(c)));
    }

}
