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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Window;
import android.view.WindowManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;

import java.io.IOException;

/*
    这是用于处理闹钟提醒的一个 Activity，
    主要功能包括获取待办事项信息、控制屏幕显示、播放闹钟声音、弹出操作对话框以及处理用户的响应操作等。
 */
public class AlarmAlertActivity extends Activity implements OnClickListener, OnDismissListener {

    // 记录待办事项ID
    private long mNoteId;

    // 记录待办事项的摘要信息
    private String mSnippet;

    // 摘要预览的最大长度
    private static final int SNIPPET_PREW_MAX_LEN = 60;

    // 媒体播放器，用于播放闹铃声音
    MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 隐藏标题栏
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Window win = getWindow();
        // 设置窗口可以在锁屏状态下显示
        win.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        // 如果屏幕未亮起，设置相关标志以唤醒并保持屏幕开启
        if (!isScreenOn()) {
            win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR);
        }

        // 获取启动此活动的Intent
        Intent intent = getIntent();

        try {
            // 从Intent中解析出待办事项ID
            mNoteId = Long.valueOf(intent.getData().getPathSegments().get(1));
            // 根据ID获取待办事项的摘要
            mSnippet = DataUtils.getSnippetById(this.getContentResolver(), mNoteId);
            // 截断摘要，如果超过最大长度则追加省略信息
            mSnippet = mSnippet.length() > SNIPPET_PREW_MAX_LEN ? mSnippet.substring(0, SNIPPET_PREW_MAX_LEN)
                    + getResources().getString(R.string.notelist_string_info) : mSnippet;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return; // 如果处理Intent失败，则直接结束活动
        }

        // 初始化媒体播放器
        mPlayer = new MediaPlayer();
        // 如果数据库中有对应ID的普通类型笔记，则显示对话框和播放闹铃声音
        if (DataUtils.visibleInNoteDatabase(getContentResolver(), mNoteId, Notes.TYPE_NOTE)) {
            showActionDialog();
            playAlarmSound();
        } else {
            // 数据库中无对应ID的笔记，关闭活动
            finish();
        }
    }

    // 检查屏幕是否处于亮屏状态
    private boolean isScreenOn() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return pm.isScreenOn();
    }

    // 播放闹铃声音
    private void playAlarmSound() {
        // 获取系统默认的闹钟铃声Uri
        Uri url = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM);

        // 获取系统当前静音模式影响的音频流类型
        int silentModeStreams = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_RINGER_STREAMS_AFFECTED, 0);

        // 如果闹钟声音在静音模式下应被静音，则设置相应的音频流类型
        if ((silentModeStreams & (1 << AudioManager.STREAM_ALARM)) != 0) {
            mPlayer.setAudioStreamType(silentModeStreams);
        } else {
            mPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        }
        try {
            // 准备播放闹铃声音
            mPlayer.setDataSource(this, url);
            mPlayer.prepare();
            mPlayer.setLooping(true); // 设置循环播放
            mPlayer.start(); // 开始播放
        } catch (IllegalArgumentException | SecurityException | IllegalStateException | IOException e) {
            e.printStackTrace(); // 记录可能的异常信息
        }
    }

    // 显示操作对话框
    private void showActionDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        // 设置对话框标题为应用名称
        dialog.setTitle(R.string.app_name);
        // 设置对话框内容为摘要信息
        dialog.setMessage(mSnippet);
        // 设置确定按钮的点击事件
        dialog.setPositiveButton(R.string.notealert_ok, this);
        // 如果屏幕已亮起，设置取消按钮的点击事件
        if (isScreenOn()) {
            dialog.setNegativeButton(R.string.notealert_enter, this);
        }
        // 显示对话框，并设置消失监听
        dialog.show().setOnDismissListener(this);
    }

    // 对话框按钮点击事件处理
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_NEGATIVE: // 取消按钮点击
                // 跳转到编辑页面并传递待办事项ID
                Intent intent = new Intent(this, NoteEditActivity.class);
                intent.setAction(Intent.ACTION_VIEW);
                intent.putExtra(Intent.EXTRA_UID, mNoteId);
                startActivity(intent);
                break;
            default:
                break;
        }
    }

    // 对话框消失时停止播放闹铃声音并结束活动
    public void onDismiss(DialogInterface dialog) {
        stopAlarmSound();
        finish();
    }

    // 停止播放并释放资源
    private void stopAlarmSound() {
        if (mPlayer != null) {
            mPlayer.stop();
            mPlayer.release();
            mPlayer = null;
        }
    }
}


