/*
 * Copyright (c) 2015 Andrew Chow
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.achow101.bitcointalkforum;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

public class BootReciever extends BroadcastReceiver {
    public BootReciever() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String sessId = null;
        String mUsername = null;
        try {
            // Read local file for session id
            FileInputStream fis = context.openFileInput("sessionid.txt");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            sessId = sb.toString();

            // Read local file for username
            fis = context.openFileInput("username.txt");
            isr = new InputStreamReader(fis);
            bufferedReader = new BufferedReader(isr);
            sb = new StringBuilder();
            line = "";
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line);
            }

            mUsername = sb.toString();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int seconds = Integer.parseInt(prefs.getString("notifications_sync_freq", "60"));
        AlarmManager am = (AlarmManager)context.getSystemService(context.ALARM_SERVICE);
        Intent i = new Intent(context, NotificationService.class);
        i.putExtra("SESSION ID", sessId);
        i.putExtra("Username", mUsername);
        PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
        am.cancel(pi);

        if(prefs.getBoolean("notifications_new_message", true))
        {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + seconds*1000, seconds*1000, pi);
        }
    }
}
