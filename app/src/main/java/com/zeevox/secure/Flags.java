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

package com.zeevox.secure;

/**
 * Not flags as per the historic name, but a class of constants that help prevent typos when writing new code
 */
@SuppressWarnings({"PointlessBooleanExpression", "WeakerAccess"})
public class Flags {
    public static final int MAX_ATTEMPTS = 3;
    //    public static final String ASK_MASTER_PASS_EACH_TIME = "master_pass_each_time";
    public static final String CLEAR_DATABASE = "delete_database";
    //    public static final String ENABLE_APPLICATION_UNLOCK = "application_unlock";
    public static final String SECURITY_LEVEL = "security_level";
    public static final String EXPORT_PASSWORDS = "export_passwords";
    public static final String BACKUP_RESTORE = "google_drive_backup";
    public static final String BACKUP_RESTORE_NOW = "google_drive_backup_now";
    public static final String ENCRYPTION_ALGORITHM = "encryption_algorithm";
    public static final String SELEV_NONE = "none";
    public static final String SELEV_FINGERPRINT = "fingerprint";
    public static final String SELEV_PASSWORD = "password";
    public static final float DIALOG_BACKGROUND_DIM = 0.3f;
}
