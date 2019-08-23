/**
 * Copyright 2018 Google LLC
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zeevox.secure.backup;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.zeevox.secure.R;
import com.zeevox.secure.cryptography.Entries;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;

    DriveServiceHelper(Drive driveService) {
        mDriveService = driveService;
    }

    /**
     * Creates a text file in the user's My Drive folder and returns its file ID.
     */
    Task<String> createFile(Context context) {
        return Tasks.call(mExecutor, () -> {
            File metadata = new File()
                    .setParents(Collections.singletonList("root"))
                    .setOriginalFilename(Entries.FILENAME)
                    .setName(context.getString(R.string.google_backup_friendly_name))
                    .setMimeType("application/xml");
            FileContent content = new FileContent("application/xml", new java.io.File(context.getFilesDir(), Entries.FILENAME));
            File googleFile = mDriveService.files().create(metadata, content).execute();
            if (googleFile == null) {
                throw new IOException("Null result when requesting file creation.");
            }

            return googleFile.getId();
        });
    }

    /**
     * Updates the file identified by {@code fileId} with the given {@code name} and {@code
     * content}.
     */
    Task<Void> updateFile(Context context, String fileId) {
        return Tasks.call(mExecutor, () -> {
            // Create a File containing any metadata changes.
            File metadata = new File()
                    .setName(context.getString(R.string.google_backup_friendly_name))
                    .setMimeType("application/xml")
                    .setOriginalFilename(Entries.FILENAME);
            FileContent content = new FileContent("application/xml", new java.io.File(context.getFilesDir(), Entries.FILENAME));
            File file = mDriveService.files().update(fileId, metadata, content).execute();
            Log.d(getClass().getSimpleName(), "Uploaded file with ID " + file.getId());
            return null;
        });
    }

    Task<File> getFile(String fileID) {
        return Tasks.call(mExecutor, () -> {
            try {
                return mDriveService.files().get(fileID).setFields("id, name, size, modifiedTime").execute();
            } catch (GoogleJsonResponseException e) {
                return null;
            }
        });
    }

}
