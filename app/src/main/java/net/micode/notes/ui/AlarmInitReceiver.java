/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;

import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
/*
  广播接收器，用于初始化笔记提醒。
 */
public class AlarmInitReceiver extends BroadcastReceiver {

    // 查询列名数组
    private static final String[] PROJECTION = new String[] {
            NoteColumns.ID,
            NoteColumns.ALERTED_DATE
    };

    // 列索引常量
    private static final int COLUMN_ID = 0;
    private static final int COLUMN_ALERTED_DATE = 1;

    /*
     当接收到广播时执行的方法。
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        // 获取当前时间戳
        long currentDate = System.currentTimeMillis();

        // 查询所有未提醒且类型为笔记的数据
        Cursor cursor = context.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                PROJECTION,
                NoteColumns.ALERTED_DATE + ">? AND " + NoteColumns.TYPE + "=" + Notes.TYPE_NOTE,
                new String[]{String.valueOf(currentDate)},
                null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                do {
                    // 获取每条记录的提醒日期和 ID
                    long alertDate = cursor.getLong(COLUMN_ALERTED_DATE);
                    long noteId = cursor.getLong(COLUMN_ID);

                    // 创建 Intent 用于发送广播
                    Intent sender = new Intent(context, AlarmReceiver.class);
                    sender.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, noteId));

                    // 创建 PendingIntent 用于发送广播
                    PendingIntent pendingIntent = PendingIntent.getBroadcast(
                            context,
                            0,
                            sender,
                            PendingIntent.FLAG_UPDATE_CURRENT
                    );

                    // 获取 AlarmManager 对象
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

                    // 设置定时提醒
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            alertDate,
                            pendingIntent
                    );
                } while (cursor.moveToNext());
            }
            cursor.close(); // 关闭 Cursor
        }
    }
}
