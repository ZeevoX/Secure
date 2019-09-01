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

package com.zeevox.secure.core;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.Stream;

/**
 * A basic randomised password generator
 * To be used in a future update
 * @version v2.0
 */
@RequiresApi(api = Build.VERSION_CODES.N)
class PasswordGenerator {

    private final Stream<String> numbers = Stream.of("0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    private final Stream<String> lettersLowercase = Stream.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z");
    private final Stream<String> lettersUppercase = Stream.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z");
    private final Stream<String> punctuationArray = Stream.of(".", ",", ":", "-", "_", "$", "%", "&");

    protected String[] charArray = null;

    private Random random;

    private boolean useNumbers;
    private boolean useLowerCase;
    private boolean useUpperCase;
    private boolean usePunctuation;

    /**
     * Initialise PasswordGenerator the default way
     */
    public PasswordGenerator() throws NoSuchAlgorithmException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            random = SecureRandom.getInstanceStrong();
        } else {
            random = new SecureRandom();
        }

    }

    /**
     * The purpose of this class : generate a random password.
     *
     * @param length length of the passcode to generate
     * @return the generated passcode
     */
    public char[] generatePassword(int length) {
        Stream<String> temp = lettersLowercase;
        if (useNumbers)
            temp = Stream.concat(numbers, temp);
        if (useUpperCase)
            temp = Stream.concat(lettersUppercase, temp);
        if (usePunctuation)
            temp = Stream.concat(punctuationArray, temp);

        assert temp != null;
        String[] symbols = temp.toArray(String[]::new);

        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int indexRandom = random.nextInt(symbols.length);
            sb.append(symbols[indexRandom]);
        }

        return sb.toString().toCharArray();
    }

    public void setSeed(long seed) {
        random.setSeed(seed);
    }

    public boolean isUseNumbers() {
        return useNumbers;
    }

    public void setUseNumbers(boolean useNumbers) {
        this.useNumbers = useNumbers;
    }

    public boolean isUseUpperCase() {
        return useUpperCase;
    }

    public void setUseUpperCase(boolean useUpperCase) {
        this.useUpperCase = useUpperCase;
    }

    public boolean isUseLowerCase() {
        return useLowerCase;
    }

    public void setUseLowerCase(boolean useLowerCase) {
        this.useLowerCase = useLowerCase;
    }

    public boolean isUsePunctuation() {
        return usePunctuation;
    }

    public void setUsePunctuation(boolean usePunctuation) {
        this.usePunctuation = usePunctuation;
    }
}
