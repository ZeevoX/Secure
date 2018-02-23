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

import android.util.Log;

public class LogUtils {

    /**
     * Log an exception's error message to Android's "logcat" in red error message text.
     *
     * @param tag       The activity or class' identifying tag; this is usually the class or method name.
     * @param exception The exception that has occurred in the code to log in red error text/style.
     */
    public static void error(String tag, Exception exception) {
        Log.e(tag, exception.getMessage());
    }

    /**
     * Log an error message help text to Android's "logcat"; often used together with the above method.
     *
     * @param tag     The activity or class' identifying tag; this is usually the class or method name.
     * @param message The actual error message help text, what the developer should do to fix the exception.
     */
    public static void error(String tag, String message) {
        Log.e(tag, message);
    }
}
