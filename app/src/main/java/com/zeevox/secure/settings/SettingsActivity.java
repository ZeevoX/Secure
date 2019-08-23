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

package com.zeevox.secure.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.zeevox.secure.BuildConfig;
import com.zeevox.secure.R;
import com.zeevox.secure.core.SecureAppCompatActivity;
import com.zeevox.secure.ui.FeedbackActivity;
import com.zeevox.secure.ui.PassgenActivity;

public class SettingsActivity extends SecureAppCompatActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate layout
        setContentView(R.layout.activity_settings);

//        final Toolbar toolbar = findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);
//        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_menu);
        setSupportActionBar(toolbar);

        final DrawerLayout mDrawerLayout = findViewById(R.id.drawer_layout);
        toolbar.setNavigationOnClickListener(v -> {
            // Handle the navigation click by showing a BottomDrawer etc.
            mDrawerLayout.openDrawer(GravityCompat.START);
        });

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    // close drawer when item is tapped
                    mDrawerLayout.closeDrawers();

                    switch (menuItem.getItemId()) {
                        case R.id.nav_home:
                            finish();
                            break;
                        case R.id.nav_passgen:
                            startActivity(new Intent(SettingsActivity.this, PassgenActivity.class));
                            break;
                        case R.id.nav_help:
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://zeevox.net/secure/")));
                            break;
                        case R.id.nav_send_feedback:
                            startActivity(new Intent(SettingsActivity.this, FeedbackActivity.class));
                            break;
                        case R.id.nav_tos:
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://zeevox.net/secure/privacy.html")));
                            break;
                    }

                    // Add code here to update the UI based on the item selected
                    // For example, swap UI fragments here

                    return true;
                });
        navigationView.setCheckedItem(R.id.nav_settings);

        TextView navHeaderAppVersionText = navigationView.getHeaderView(0).findViewById(R.id.nav_header_app_version);
        navHeaderAppVersionText.setText(String.format(getString(R.string.nav_header_app_version), BuildConfig.VERSION_NAME));
    }
}
