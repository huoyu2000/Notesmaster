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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;


public class NotesPreferenceActivity extends PreferenceActivity {
    // 定义偏好设置的名称
    public static final String PREFERENCE_NAME = "notes_preferences";

    // 定义同步账户名称的偏好设置键
    public static final String PREFERENCE_SYNC_ACCOUNT_NAME = "pref_key_account_name";

    // 定义上次同步时间的偏好设置键
    public static final String PREFERENCE_LAST_SYNC_TIME = "pref_last_sync_time";

    // 定义设置背景颜色的偏好设置键
    public static final String PREFERENCE_SET_BG_COLOR_KEY = "pref_key_bg_random_appear";

    // 定义同步账户的内部键
    private static final String PREFERENCE_SYNC_ACCOUNT_KEY = "pref_sync_account_key";

    // 定义权限过滤键
    private static final String AUTHORITIES_FILTER_KEY = "authorities";

    // 用于存储账户分类的偏好设置类别
    private PreferenceCategory mAccountCategory;

    // 用于接收任务的接收器
    private GTaskReceiver mReceiver;

    // 原始账户数组
    private Account[] mOriAccounts;

    // 标记是否已添加账户
    private boolean mHasAddedAccount;


    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // 使用应用图标作为导航按钮
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // 从资源文件加载偏好设置
        addPreferencesFromResource(R.xml.preferences);

        // 找到同步账户的偏好设置类别
        mAccountCategory = (PreferenceCategory) findPreference(PREFERENCE_SYNC_ACCOUNT_KEY);

        // 创建任务接收器
        mReceiver = new GTaskReceiver();

        // 创建意图过滤器并添加广播动作
        IntentFilter filter = new IntentFilter();
        filter.addAction(GTaskSyncService.GTASK_SERVICE_BROADCAST_NAME);

        // 注册接收器
        registerReceiver(mReceiver, filter);

        // 初始化原始账户数组
        mOriAccounts = null;

        // 加载设置头部布局
        View header = LayoutInflater.from(this).inflate(R.layout.settings_header, null);
        getListView().addHeaderView(header, null, true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 如果用户已添加新账户，则自动设置同步账户
        if (mHasAddedAccount) {
            Account[] accounts = getGoogleAccounts();
            if (mOriAccounts != null && accounts.length > mOriAccounts.length) {
                for (Account accountNew : accounts) {
                    boolean found = false;
                    for (Account accountOld : mOriAccounts) {
                        if (TextUtils.equals(accountOld.name, accountNew.name)) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        setSyncAccount(accountNew.name);
                        break;
                    }
                }
            }
        }

        // 刷新界面
        refreshUI();
    }

    @Override
    protected void onDestroy() {
        // 注销接收器
        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }
        super.onDestroy();
    }

    private void loadAccountPreference() {
        // 清空账户偏好设置类别中的所有项
        mAccountCategory.removeAll();

        // 创建账户偏好设置对象
        Preference accountPref = new Preference(this);
        final String defaultAccount = getSyncAccountName(this);

        // 设置账户偏好设置的标题和摘要
        accountPref.setTitle(getString(R.string.preferences_account_title));
        accountPref.setSummary(getString(R.string.preferences_account_summary));

        // 设置账户偏好设置的点击监听器
        accountPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                if (!GTaskSyncService.isSyncing()) {
                    if (TextUtils.isEmpty(defaultAccount)) {
                        // 第一次设置账户
                        showSelectAccountAlertDialog();
                    } else {
                        // 如果账户已经设置，需要提示用户风险
                        showChangeAccountConfirmAlertDialog();
                    }
                } else {
                    // 同步正在进行，不能更改账户
                    Toast.makeText(NotesPreferenceActivity.this,
                                    R.string.preferences_toast_cannot_change_account, Toast.LENGTH_SHORT)
                            .show();
                }
                return true;
            }
        });

        // 将账户偏好设置添加到类别中
        mAccountCategory.addPreference(accountPref);
    }

    private void loadSyncButton() {
        // 获取同步按钮和最后一次同步时间的文本视图
        Button syncButton = (Button) findViewById(R.id.preference_sync_button);
        TextView lastSyncTimeView = (TextView) findViewById(R.id.prefenerece_sync_status_textview);

        // 设置按钮状态
        if (GTaskSyncService.isSyncing()) {
            // 如果正在同步
            syncButton.setText(getString(R.string.preferences_button_sync_cancel));
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // 取消同步
                    GTaskSyncService.cancelSync(NotesPreferenceActivity.this);
                }
            });
        } else {
            // 如果未在同步
            syncButton.setText(getString(R.string.preferences_button_sync_immediately));
            syncButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    // 立即同步
                    GTaskSyncService.startSync(NotesPreferenceActivity.this);
                }
            });
        }

        // 设置按钮是否可用
        syncButton.setEnabled(!TextUtils.isEmpty(getSyncAccountName(this)));

        // 设置最后一次同步时间
        if (GTaskSyncService.isSyncing()) {
            // 如果正在同步
            lastSyncTimeView.setText(GTaskSyncService.getProgressString());
            lastSyncTimeView.setVisibility(View.VISIBLE);
        } else {
            // 如果未在同步
            long lastSyncTime = getLastSyncTime(this);
            if (lastSyncTime != 0) {
                // 如果有最后一次同步时间
                lastSyncTimeView.setText(getString(R.string.preferences_last_sync_time,
                        DateFormat.format(getString(R.string.preferences_last_sync_time_format),
                                lastSyncTime)));
                lastSyncTimeView.setVisibility(View.VISIBLE);
            } else {
                // 如果没有最后一次同步时间
                lastSyncTimeView.setVisibility(View.GONE);
            }
        }
    }

    private void refreshUI() {
        // 重新加载账户偏好设置和同步按钮
        loadAccountPreference();
        loadSyncButton();
    }

    private void showSelectAccountAlertDialog() {
        // 创建对话框构建器
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 加载对话框标题布局
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        titleTextView.setText(getString(R.string.preferences_dialog_select_account_title));
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        subtitleTextView.setText(getString(R.string.preferences_dialog_select_account_tips));

        // 设置自定义标题
        dialogBuilder.setCustomTitle(titleView);
        dialogBuilder.setPositiveButton(null, null);  // 设置默认的正按钮为空

        // 获取所有 Google 账户
        Account[] accounts = getGoogleAccounts();
        String defAccount = getSyncAccountName(this);

        // 更新原始账户数组和标记
        mOriAccounts = accounts;
        mHasAddedAccount = false;

        if (accounts.length > 0) {
            // 创建账户选项数组
            CharSequence[] items = new CharSequence[accounts.length];
            final CharSequence[] itemMapping = items;
            int checkedItem = -1;
            int index = 0;

            // 遍历账户并设置选项
            for (Account account : accounts) {
                if (TextUtils.equals(account.name, defAccount)) {
                    checkedItem = index;
                }
                items[index++] = account.name;
            }

            // 设置单选列表项
            dialogBuilder.setSingleChoiceItems(items, checkedItem,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // 设置同步账户并刷新 UI
                            setSyncAccount(itemMapping[which].toString());
                            dialog.dismiss();
                            refreshUI();
                        }
                    });
        }

        // 加载添加账户的视图
        View addAccountView = LayoutInflater.from(this).inflate(R.layout.add_account_text, null);
        dialogBuilder.setView(addAccountView);

        // 显示对话框
        final AlertDialog dialog = dialogBuilder.show();

        // 设置添加账户视图的点击监听器
        addAccountView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mHasAddedAccount = true;
                Intent intent = new Intent("android.settings.ADD_ACCOUNT_SETTINGS");
                intent.putExtra(AUTHORITIES_FILTER_KEY, new String[] {"gmail-ls"});
                startActivityForResult(intent, -1);
                dialog.dismiss();
            }
        });
    }

    private void showChangeAccountConfirmAlertDialog() {
        // 创建对话框构建器
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        // 加载对话框标题布局
        View titleView = LayoutInflater.from(this).inflate(R.layout.account_dialog_title, null);
        TextView titleTextView = (TextView) titleView.findViewById(R.id.account_dialog_title);
        titleTextView.setText(getString(R.string.preferences_dialog_change_account_title,
                getSyncAccountName(this)));
        TextView subtitleTextView = (TextView) titleView.findViewById(R.id.account_dialog_subtitle);
        subtitleTextView.setText(getString(R.string.preferences_dialog_change_account_warn_msg));
        dialogBuilder.setCustomTitle(titleView);

        // 设置菜单项
        CharSequence[] menuItemArray = new CharSequence[] {
                getString(R.string.preferences_menu_change_account),
                getString(R.string.preferences_menu_remove_account),
                getString(R.string.preferences_menu_cancel)
        };

        // 设置菜单项点击监听器
        dialogBuilder.setItems(menuItemArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // 显示选择账户对话框
                    showSelectAccountAlertDialog();
                } else if (which == 1) {
                    // 移除同步账户并刷新 UI
                    removeSyncAccount();
                    refreshUI();
                }
            }
        });

        // 显示对话框
        dialogBuilder.show();
    }

    private Account[] getGoogleAccounts() {
        // 获取 AccountManager 并获取所有 Google 账户
        AccountManager accountManager = AccountManager.get(this);
        return accountManager.getAccountsByType("com.google");
    }

    private void setSyncAccount(String account) {
        // 检查新账户与当前账户是否相同
        if (!getSyncAccountName(this).equals(account)) {
            // 获取 SharedPreferences
            SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();

            // 设置新的同步账户
            if (account != null) {
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, account);
            } else {
                editor.putString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
            }

            // 提交更改
            editor.commit();

            // 清理最后一次同步时间
            setLastSyncTime(this, 0);

            // 清理本地 gtask 相关信息
            new Thread(new Runnable() {
                public void run() {
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.GTASK_ID, "");
                    values.put(NoteColumns.SYNC_ID, 0);
                    getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
                }
            }).start();

            // 显示成功设置账户的 Toast
            Toast.makeText(NotesPreferenceActivity.this,
                    getString(R.string.preferences_toast_success_set_accout, account),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void removeSyncAccount() {
        // 获取 SharedPreferences
        SharedPreferences settings = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();

        // 移除同步账户名称
        if (settings.contains(PREFERENCE_SYNC_ACCOUNT_NAME)) {
            editor.remove(PREFERENCE_SYNC_ACCOUNT_NAME);
        }

        // 移除最后一次同步时间
        if (settings.contains(PREFERENCE_LAST_SYNC_TIME)) {
            editor.remove(PREFERENCE_LAST_SYNC_TIME);
        }

        // 提交更改
        editor.commit();

        // 清理本地 gtask 相关信息
        new Thread(new Runnable() {
            public void run() {
                ContentValues values = new ContentValues();
                values.put(NoteColumns.GTASK_ID, "");
                values.put(NoteColumns.SYNC_ID, 0);
                getContentResolver().update(Notes.CONTENT_NOTE_URI, values, null, null);
            }
        }).start();
    }

    public static String getSyncAccountName(Context context) {
        // 获取 SharedPreferences
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        // 获取同步账户名称
        return settings.getString(PREFERENCE_SYNC_ACCOUNT_NAME, "");
    }

    public static void setLastSyncTime(Context context, long time) {
        // 获取 SharedPreferences
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        // 获取 SharedPreferences 编辑器
        SharedPreferences.Editor editor = settings.edit();

        // 设置最后一次同步时间
        editor.putLong(PREFERENCE_LAST_SYNC_TIME, time);

        // 提交更改
        editor.commit();
    }

    public static long getLastSyncTime(Context context) {
        // 获取 SharedPreferences
        SharedPreferences settings = context.getSharedPreferences(PREFERENCE_NAME,
                Context.MODE_PRIVATE);

        // 获取最后一次同步时间
        return settings.getLong(PREFERENCE_LAST_SYNC_TIME, 0);
    }

    private class GTaskReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 刷新 UI
            refreshUI();

            // 检查是否正在同步
            if (intent.getBooleanExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_IS_SYNCING, false)) {
                // 获取同步状态 TextView
                TextView syncStatus = (TextView) findViewById(R.id.prefenerece_sync_status_textview);

                // 设置同步状态文本
                syncStatus.setText(intent
                        .getStringExtra(GTaskSyncService.GTASK_SERVICE_BROADCAST_PROGRESS_MSG));
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        // 根据菜单项 ID 进行处理
        switch (item.getItemId()) {
            case android.R.id.home:
                // 创建 Intent，跳转到 NotesListActivity
                Intent intent = new Intent(this, NotesListActivity.class);

                // 设置 Intent 标志，清除顶部的 Activity 栈
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                // 启动 NotesListActivity
                startActivity(intent);

                // 返回 true 表示已处理此菜单项
                return true;

            default:
                // 默认情况下返回 false，表示未处理此菜单项
                return false;
        }
    }

}
