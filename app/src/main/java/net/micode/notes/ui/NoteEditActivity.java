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
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.model.WorkingNote.NoteSettingChangedListener;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.tool.ResourceParser.TextAppearanceResources;
import net.micode.notes.ui.DateTimePickerDialog.OnDateTimeSetListener;
import net.micode.notes.ui.NoteEditText.OnTextViewChangeListener;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NoteEditActivity extends Activity implements OnClickListener,
        NoteSettingChangedListener, OnTextViewChangeListener {
    private class HeadViewHolder {
        // TextView 显示修改时间
        public TextView tvModified;
        // ImageView 显示提醒图标
        public ImageView ivAlertIcon;
        // TextView 显示提醒日期
        public TextView tvAlertDate;
        // ImageView 用于设置背景颜色
        public ImageView ibSetBgColor;
    }


    private static final Map<Integer, Integer> sBgSelectorBtnsMap = new HashMap<Integer, Integer>();
    static {
        // 将背景颜色按钮 ID 映射到对应的资源标识符
        sBgSelectorBtnsMap.put(R.id.iv_bg_yellow, ResourceParser.YELLOW);
        sBgSelectorBtnsMap.put(R.id.iv_bg_red, ResourceParser.RED);
        sBgSelectorBtnsMap.put(R.id.iv_bg_blue, ResourceParser.BLUE);
        sBgSelectorBtnsMap.put(R.id.iv_bg_green, ResourceParser.GREEN);
        sBgSelectorBtnsMap.put(R.id.iv_bg_white, ResourceParser.WHITE);
    }

    private static final Map<Integer, Integer> sBgSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        // 将背景颜色资源标识符映射到选择状态下的按钮 ID
        sBgSelectorSelectionMap.put(ResourceParser.YELLOW, R.id.iv_bg_yellow_select);
        sBgSelectorSelectionMap.put(ResourceParser.RED, R.id.iv_bg_red_select);
        sBgSelectorSelectionMap.put(ResourceParser.BLUE, R.id.iv_bg_blue_select);
        sBgSelectorSelectionMap.put(ResourceParser.GREEN, R.id.iv_bg_green_select);
        sBgSelectorSelectionMap.put(ResourceParser.WHITE, R.id.iv_bg_white_select);
    }

    private static final Map<Integer, Integer> sFontSizeBtnsMap = new HashMap<Integer, Integer>();
    static {
        // 将字体大小按钮 ID 映射到对应的资源标识符
        sFontSizeBtnsMap.put(R.id.ll_font_large, ResourceParser.TEXT_LARGE);
        sFontSizeBtnsMap.put(R.id.ll_font_small, ResourceParser.TEXT_SMALL);
        sFontSizeBtnsMap.put(R.id.ll_font_normal, ResourceParser.TEXT_MEDIUM);
        sFontSizeBtnsMap.put(R.id.ll_font_super, ResourceParser.TEXT_SUPER);
    }

    private static final Map<Integer, Integer> sFontSelectorSelectionMap = new HashMap<Integer, Integer>();
    static {
        // 将字体大小资源标识符映射到选择状态下的按钮 ID
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_LARGE, R.id.iv_large_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SMALL, R.id.iv_small_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_MEDIUM, R.id.iv_medium_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SUPER, R.id.iv_super_select);
    }
    // 日志标签，用于日志输出
    private static final String TAG = "NoteEditActivity";

    // ViewHolder 类实例，用于缓存头部视图中的控件引用
    private HeadViewHolder mNoteHeaderHolder;

    // 头部视图面板
    private View mHeadViewPanel;

    // 背景颜色选择器视图
    private View mNoteBgColorSelector;

    // 字体大小选择器视图
    private View mFontSizeSelector;

    // 笔记编辑器文本框
    private EditText mNoteEditor;

    // 笔记编辑器面板视图
    private View mNoteEditorPanel;

    // 当前正在编辑的工作笔记对象
    private WorkingNote mWorkingNote;

    // SharedPreferences 实例，用于存储应用偏好设置
    private SharedPreferences mSharedPrefs;

    // 当前字体大小的 ID
    private int mFontSizeId;

    // 存储字体大小偏好的键名
    private static final String PREFERENCE_FONT_SIZE = "pref_font_size";

    // 快捷图标标题的最大长度
    private static final int SHORTCUT_ICON_TITLE_MAX_LEN = 10;

    // 勾选状态的标签字符
    public static final String TAG_CHECKED = String.valueOf('\u221A');

    // 未勾选状态的标签字符
    public static final String TAG_UNCHECKED = String.valueOf('\u25A1');

    // 线性布局容器，用于存放多个编辑文本框
    private LinearLayout mEditTextList;

    // 用户输入的查询字符串
    private String mUserQuery;

    // 正则表达式模式，用于匹配特定的字符串
    private Pattern mPattern;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 调用父类的 onCreate 方法
        super.onCreate(savedInstanceState);

        // 设置当前 Activity 的布局文件
        this.setContentView(R.layout.note_edit);

        // 如果没有保存的状态，并且初始化 Activity 状态失败，则关闭 Activity
        if (savedInstanceState == null && !initActivityState(getIntent())) {
            finish();
            return;
        }

        // 初始化资源
        initResources();
    }

    /**
     * 当内存不足时，当前 Activity 可能会被系统杀死。一旦被杀死，在下次用户加载此 Activity 时，
     * 应恢复之前的状态。
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        // 调用父类的 onRestoreInstanceState 方法
        super.onRestoreInstanceState(savedInstanceState);

        // 如果存在保存的状态并且包含 Intent.EXTRA_UID 键
        if (savedInstanceState != null && savedInstanceState.containsKey(Intent.EXTRA_UID)) {
            // 创建一个新的 Intent 并设置 ACTION_VIEW 动作
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // 将 UID 添加到 Intent 中
            intent.putExtra(Intent.EXTRA_UID, savedInstanceState.getLong(Intent.EXTRA_UID));

            // 如果初始化 Activity 状态失败，则关闭 Activity
            if (!initActivityState(intent)) {
                finish();
                return;
            }

            // 输出日志信息，表示从被杀死的 Activity 恢复
            Log.d(TAG, "从被杀死的 Activity 恢复");
        }
    }

    private boolean initActivityState(Intent intent) {
        // 初始化工作笔记为 null
        mWorkingNote = null;

        // 如果 Intent 的动作是 ACTION_VIEW
        if (TextUtils.equals(Intent.ACTION_VIEW, intent.getAction())) {
            // 获取笔记 ID，默认值为 0
            long noteId = intent.getLongExtra(Intent.EXTRA_UID, 0);
            mUserQuery = "";

            // 如果 Intent 包含搜索结果数据
            if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
                // 解析搜索结果中的笔记 ID 和查询字符串
                noteId = Long.parseLong(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
                mUserQuery = intent.getStringExtra(SearchManager.USER_QUERY);
            }

            // 如果笔记 ID 不存在于数据库中
            if (!DataUtils.visibleInNoteDatabase(getContentResolver(), noteId, Notes.TYPE_NOTE)) {
                // 跳转到笔记列表 Activity
                Intent jump = new Intent(this, NotesListActivity.class);
                startActivity(jump);
                showToast(R.string.error_note_not_exist);
                finish();
                return false;
            } else {
                // 加载笔记
                mWorkingNote = WorkingNote.load(this, noteId);
                if (mWorkingNote == null) {
                    Log.e(TAG, "加载笔记失败，笔记 ID: " + noteId);
                    finish();
                    return false;
                }
            }

            // 隐藏软键盘并调整布局大小
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                            | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else if (TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, intent.getAction())) {
            // 新建笔记
            long folderId = intent.getLongExtra(Notes.INTENT_EXTRA_FOLDER_ID, 0);
            int widgetId = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            int widgetType = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_TYPE,
                    Notes.TYPE_WIDGET_INVALIDE);
            int bgResId = intent.getIntExtra(Notes.INTENT_EXTRA_BACKGROUND_ID,
                    ResourceParser.getDefaultBgId(this));

            // 解析通话记录笔记
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            long callDate = intent.getLongExtra(Notes.INTENT_EXTRA_CALL_DATE, 0);
            if (callDate != 0 && phoneNumber != null) {
                if (TextUtils.isEmpty(phoneNumber)) {
                    Log.w(TAG, "通话记录号码为空");
                }
                long noteId = 0;
                if ((noteId = DataUtils.getNoteIdByPhoneNumberAndCallDate(getContentResolver(),
                        phoneNumber, callDate)) > 0) {
                    // 加载通话记录笔记
                    mWorkingNote = WorkingNote.load(this, noteId);
                    if (mWorkingNote == null) {
                        Log.e(TAG, "加载通话记录笔记失败，笔记 ID: " + noteId);
                        finish();
                        return false;
                    }
                } else {
                    // 创建空笔记并转换为通话记录笔记
                    mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId,
                            widgetType, bgResId);
                    mWorkingNote.convertToCallNote(phoneNumber, callDate);
                }
            } else {
                // 创建空笔记
                mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId, widgetType,
                        bgResId);
            }

            // 调整布局大小并显示软键盘
            getWindow().setSoftInputMode(
                    WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                            | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else {
            // 如果 Intent 动作未指定，不支持该操作
            Log.e(TAG, "Intent 未指定动作，不支持");
            finish();
            return false;
        }

        // 设置状态更改监听器
        mWorkingNote.setOnSettingStatusChangedListener(this);
        return true;
    }
    @Override
    protected void onResume() {
        super.onResume();
        initNoteScreen();  // 初始化笔记界面
    }

    private void initNoteScreen() {
        // 设置文本外观
        mNoteEditor.setTextAppearance(this, TextAppearanceResources.getTexAppearanceResource(mFontSizeId));

        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            // 切换到列表模式
            switchToListMode(mWorkingNote.getContent());
        } else {
            // 设置高亮查询结果
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
            mNoteEditor.setSelection(mNoteEditor.getText().length());  // 设置光标位置
        }

        // 遍历背景选择器并隐藏所有视图
        for (Integer id : sBgSelectorSelectionMap.keySet()) {
            findViewById(sBgSelectorSelectionMap.get(id)).setVisibility(View.GONE);
        }

        // 设置标题背景
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
        // 设置编辑器背景
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());

        // 设置修改日期
        mNoteHeaderHolder.tvModified.setText(DateUtils.formatDateTime(this,
                mWorkingNote.getModifiedDate(), DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR));

        /**
         *  添加设置提醒的菜单。目前禁用，因为 DateTimePicker 尚未准备好
         */
        showAlertHeader();  // 显示提醒头部
    }

    private void showAlertHeader() {
        if (mWorkingNote.hasClockAlert()) {
            long currentTime = System.currentTimeMillis();

            if (currentTime > mWorkingNote.getAlertDate()) {
                // 如果提醒时间已过期
                mNoteHeaderHolder.tvAlertDate.setText(R.string.note_alert_expired);
            } else {
                // 设置相对时间
                mNoteHeaderHolder.tvAlertDate.setText(DateUtils.getRelativeTimeSpanString(
                        mWorkingNote.getAlertDate(), currentTime, DateUtils.MINUTE_IN_MILLIS));
            }

            // 显示提醒时间和图标
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.VISIBLE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.VISIBLE);
        } else {
            // 隐藏提醒时间和图标
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.GONE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initActivityState(intent);  // 初始化活动状态
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        /**
         * 对于没有笔记 ID 的新笔记，应首先保存以生成一个 ID。
         * 如果编辑的笔记不值得保存，则没有 ID，相当于创建新笔记
         */
        if (!mWorkingNote.existInDatabase()) {
            saveNote();  // 保存笔记
        }
        outState.putLong(Intent.EXTRA_UID, mWorkingNote.getNoteId());
        Log.d(TAG, "Save working note id: " + mWorkingNote.getNoteId() + " onSaveInstanceState");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mNoteBgColorSelector, ev)) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        }

        if (mFontSizeSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mFontSizeSelector, ev)) {
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    private boolean inRangeOfView(View view, MotionEvent ev) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];

        // 检查触摸事件是否在视图范围内
        if (ev.getX() < x
                || ev.getX() > (x + view.getWidth())
                || ev.getY() < y
                || ev.getY() > (y + view.getHeight())) {
            return false;
        }
        return true;
    }

    private void initResources() {
        // 初始化标题面板
        mHeadViewPanel = findViewById(R.id.note_title);

        // 初始化头部视图持有者
        mNoteHeaderHolder = new HeadViewHolder();
        mNoteHeaderHolder.tvModified = (TextView) findViewById(R.id.tv_modified_date);
        mNoteHeaderHolder.ivAlertIcon = (ImageView) findViewById(R.id.iv_alert_icon);
        mNoteHeaderHolder.tvAlertDate = (TextView) findViewById(R.id.tv_alert_date);
        mNoteHeaderHolder.ibSetBgColor = (ImageView) findViewById(R.id.btn_set_bg_color);
        mNoteHeaderHolder.ibSetBgColor.setOnClickListener(this);

        // 初始化编辑器
        mNoteEditor = (EditText) findViewById(R.id.note_edit_view);
        mNoteEditorPanel = findViewById(R.id.sv_note_edit);
        mNoteBgColorSelector = findViewById(R.id.note_bg_color_selector);

        // 设置背景颜色选择器按钮点击事件
        for (int id : sBgSelectorBtnsMap.keySet()) {
            ImageView iv = (ImageView) findViewById(id);
            iv.setOnClickListener(this);
        }

        // 初始化字体大小选择器
        mFontSizeSelector = findViewById(R.id.font_size_selector);
        for (int id : sFontSizeBtnsMap.keySet()) {
            View view = findViewById(id);
            view.setOnClickListener(this);
        }

        // 获取共享偏好设置
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFontSizeId = mSharedPrefs.getInt(PREFERENCE_FONT_SIZE, ResourceParser.BG_DEFAULT_FONT_SIZE);

        /**
         * HACKME: 修复将资源 ID 存储在共享偏好设置中的问题。
         * 如果 ID 大于资源长度，在这种情况下返回默认值
         */
        if (mFontSizeId >= TextAppearanceResources.getResourcesSize()) {
            mFontSizeId = ResourceParser.BG_DEFAULT_FONT_SIZE;
        }

        // 初始化编辑列表
        mEditTextList = (LinearLayout) findViewById(R.id.note_edit_list);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (saveNote()) {
            Log.d(TAG, "Note data was saved with length:" + mWorkingNote.getContent().length());
        }
        clearSettingState();  // 清除设置状态
    }

    private void updateWidget() {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_2X) {
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_4X) {
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            Log.e(TAG, "Unsupported widget type");
            return;
        }

        // 设置小部件 ID
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { mWorkingNote.getWidgetId() });

        // 发送广播更新小部件
        sendBroadcast(intent);
        setResult(RESULT_OK, intent);
    }


    public void onClick(View v) {
        int id = v.getId();

        // 当点击设置背景颜色按钮时
        if (id == R.id.btn_set_bg_color) {
            mNoteBgColorSelector.setVisibility(View.VISIBLE);
            findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(View.VISIBLE);
        }
        // 当点击背景颜色选择器按钮时
        else if (sBgSelectorBtnsMap.containsKey(id)) {
            findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(View.GONE);
            mWorkingNote.setBgColorId(sBgSelectorBtnsMap.get(id));
            mNoteBgColorSelector.setVisibility(View.GONE);
        }
        // 当点击字体大小选择器按钮时
        else if (sFontSizeBtnsMap.containsKey(id)) {
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.GONE);
            mFontSizeId = sFontSizeBtnsMap.get(id);
            mSharedPrefs.edit().putInt(PREFERENCE_FONT_SIZE, mFontSizeId).commit();
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);

            // 根据当前模式更新编辑器
            if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
                getWorkingText();
                switchToListMode(mWorkingNote.getContent());
            } else {
                mNoteEditor.setTextAppearance(this, TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
            }

            mFontSizeSelector.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        // 清除设置状态
        if (clearSettingState()) {
            return;
        }

        // 保存笔记并返回上一级
        saveNote();
        super.onBackPressed();
    }

    private boolean clearSettingState() {
        // 隐藏背景颜色选择器
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        }
        // 隐藏字体大小选择器
        else if (mFontSizeSelector.getVisibility() == View.VISIBLE) {
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        return false;
    }
    public void onBackgroundColorChanged() {
        // 显示选中的背景颜色
        findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(View.VISIBLE);

        // 设置编辑面板背景颜色
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());

        // 设置标题面板背景颜色
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 如果 Activity 正在结束，则直接返回
        if (isFinishing()) {
            return true;
        }

        // 清除设置状态
        clearSettingState();

        // 清空菜单
        menu.clear();

        // 根据笔记所在的文件夹加载不同的菜单
        if (mWorkingNote.getFolderId() == Notes.ID_CALL_RECORD_FOLDER) {
            getMenuInflater().inflate(R.menu.call_note_edit, menu);
        } else {
            getMenuInflater().inflate(R.menu.note_edit, menu);
        }

        // 更新模式切换菜单项的标题
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_normal_mode);
        } else {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_list_mode);
        }

        // 根据是否有提醒设置菜单项的可见性
        if (mWorkingNote.hasClockAlert()) {
            menu.findItem(R.id.menu_alert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_delete_remind).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_new_note:
                createNewNote();
                break;
            case R.id.menu_delete:
                // 创建删除对话框
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.alert_title_delete));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(getString(R.string.alert_message_delete_note));

                // 确定按钮处理
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                deleteCurrentNote();
                                finish();
                            }
                        });

                // 取消按钮处理
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                break;
            case R.id.menu_font_size:
                // 显示字体大小选择器
                mFontSizeSelector.setVisibility(View.VISIBLE);
                findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);
                break;
            case R.id.menu_list_mode:
                // 切换检查列表模式
                mWorkingNote.setCheckListMode(mWorkingNote.getCheckListMode() == 0 ?
                        TextNote.MODE_CHECK_LIST : 0);
                break;
            case R.id.menu_share:
                // 获取当前文本并分享
                getWorkingText();
                sendTo(this, mWorkingNote.getContent());
                break;
            case R.id.menu_send_to_desktop:
                // 发送到桌面
                sendToDesktop();
                break;
            case R.id.menu_alert:
                // 设置提醒
                setReminder();
                break;
            case R.id.menu_delete_remind:
                // 删除提醒
                mWorkingNote.setAlertDate(0, false);
                break;
            default:
                break;
        }
        return true;
    }

    private void setReminder() {
        // 创建日期时间选择对话框
        DateTimePickerDialog d = new DateTimePickerDialog(this, System.currentTimeMillis());

        // 设置日期时间选择监听器
        d.setOnDateTimeSetListener(new OnDateTimeSetListener() {
            public void OnDateTimeSet(AlertDialog dialog, long date) {
                mWorkingNote.setAlertDate(date, true);
            }
        });

        // 显示日期时间选择对话框
        d.show();
    }

    /**
     * Share note to apps that support {@link Intent#ACTION_SEND} action
     * and "text/plain" type
     */
    private void sendTo(Context context, String info) {
        // 创建发送意图
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, info);
        intent.setType("text/plain");

        // 启动发送意图
        context.startActivity(intent);
    }

    private void createNewNote() {
        // 首先保存当前编辑的笔记
        saveNote();

        // 为了安全起见，启动一个新的 `NoteEditActivity`
        finish();

        // 创建新的 `Intent` 并设置相关参数
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mWorkingNote.getFolderId());

        // 启动新的 `NoteEditActivity`
        startActivity(intent);
    }

    private void deleteCurrentNote() {
        // 检查当前笔记是否存在于数据库中
        if (mWorkingNote.existInDatabase()) {
            HashSet<Long> ids = new HashSet<Long>();

            // 获取笔记 ID
            long id = mWorkingNote.getNoteId();

            // 如果 ID 不是根文件夹 ID，则添加到集合中
            if (id != Notes.ID_ROOT_FOLDER) {
                ids.add(id);
            } else {
                Log.d(TAG, "Wrong note id, should not happen");
            }

            // 根据同步模式进行删除或移动操作
            if (!isSyncMode()) {
                if (!DataUtils.batchDeleteNotes(getContentResolver(), ids)) {
                    Log.e(TAG, "Delete Note error");
                }
            } else {
                if (!DataUtils.batchMoveToFolder(getContentResolver(), ids, Notes.ID_TRASH_FOLER)) {
                    Log.e(TAG, "Move notes to trash folder error, should not happen");
                }
            }
        }

        // 标记笔记为已删除
        mWorkingNote.markDeleted(true);
    }

    private boolean isSyncMode() {
        // 检查是否处于同步模式
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
    }

    public void onClockAlertChanged(long date, boolean set) {
        /**
         * 用户可能对未保存的笔记设置提醒时间，在设置提醒之前应先保存笔记。
         */
        if (!mWorkingNote.existInDatabase()) {
            saveNote();  // 保存当前笔记
        }

        if (mWorkingNote.getNoteId() > 0) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mWorkingNote.getNoteId()));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            showAlertHeader();  // 显示提醒头部

            if (!set) {
                // 取消提醒
                alarmManager.cancel(pendingIntent);
            } else {
                // 设置提醒时间
                alarmManager.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);
            }
        } else {
            /**
             * 如果用户没有输入任何内容（笔记不值得保存），则没有笔记 ID，
             * 提醒用户应该输入一些内容。
             */
            Log.e(TAG, "提醒设置错误");
            showToast(R.string.error_note_empty_for_clock);  // 显示错误提示
        }
    }

    public void onWidgetChanged() {
        updateWidget();  // 更新小部件
    }

    public void onEditTextDelete(int index, String text) {
        int childCount = mEditTextList.getChildCount();
        if (childCount == 1) {
            return;  // 如果只有一个子视图，则直接返回
        }

        for (int i = index + 1; i < childCount; i++) {
            // 更新后续 EditText 的索引值
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i - 1);
        }

        mEditTextList.removeViewAt(index);  // 删除指定索引位置的视图

        NoteEditText edit = null;
        if (index == 0) {
            // 获取第一个 EditText
            edit = (NoteEditText) mEditTextList.getChildAt(0).findViewById(R.id.et_edit_text);
        } else {
            // 获取前一个 EditText
            edit = (NoteEditText) mEditTextList.getChildAt(index - 1).findViewById(R.id.et_edit_text);
        }

        int length = edit.length();  // 获取当前文本长度
        edit.append(text);  // 追加删除的文本
        edit.requestFocus();  // 获取焦点
        edit.setSelection(length);  // 设置光标位置
    }

    public void onEditTextEnter(int index, String text) {
        /**
         * 此情况不应该发生，用于调试检查。
         */
        if (index > mEditTextList.getChildCount()) {
            Log.e(TAG, "索引超出 mEditTextList 范围，不应该发生");
        }

        View view = getListItem(text, index);  // 获取列表项视图
        mEditTextList.addView(view, index);  // 在指定索引位置添加视图

        NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);  // 获取 EditText
        edit.requestFocus();  // 获取焦点
        edit.setSelection(0);  // 设置光标位置为开头

        for (int i = index + 1; i < mEditTextList.getChildCount(); i++) {
            // 更新后续 EditText 的索引值
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i);
        }
    }


    private void switchToListMode(String text) {
        /**
         * 切换到列表模式，并显示所有文本项。
         */
        mEditTextList.removeAllViews();  // 清空现有的视图列表

        String[] items = text.split("\n");  // 将文本按行分割成数组
        int index = 0;

        for (String item : items) {
            if (!TextUtils.isEmpty(item)) {
                // 如果项不为空，则添加到视图列表中
                mEditTextList.addView(getListItem(item, index));
                index++;
            }
        }

        // 添加一个空白项作为最后一个项
        mEditTextList.addView(getListItem("", index));
        mEditTextList.getChildAt(index).findViewById(R.id.et_edit_text).requestFocus();  // 设置焦点

        // 隐藏编辑器并显示列表
        mNoteEditor.setVisibility(View.GONE);
        mEditTextList.setVisibility(View.VISIBLE);
    }

    private Spannable getHighlightQueryResult(String fullText, String userQuery) {
        /**
         * 对文本中的查询结果进行高亮处理。
         */
        SpannableString spannable = new SpannableString(fullText == null ? "" : fullText);

        if (!TextUtils.isEmpty(userQuery)) {
            // 编译正则表达式
            mPattern = Pattern.compile(userQuery);
            Matcher m = mPattern.matcher(fullText);
            int start = 0;

            while (m.find(start)) {
                // 设置高亮背景色
                spannable.setSpan(
                        new BackgroundColorSpan(this.getResources().getColor(
                                R.color.user_query_highlight)), m.start(), m.end(),
                        Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                start = m.end();
            }
        }

        return spannable;
    }

    private View getListItem(String item, int index) {
        /**
         * 创建并返回一个列表项视图。
         */
        View view = LayoutInflater.from(this).inflate(R.layout.note_edit_list_item, null);

        final NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        edit.setTextAppearance(this, TextAppearanceResources.getTexAppearanceResource(mFontSizeId));

        CheckBox cb = ((CheckBox) view.findViewById(R.id.cb_edit_item));
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    // 如果选中，则添加删除线样式
                    edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    // 如果未选中，则恢复默认样式
                    edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
                }
            }
        });

        if (item.startsWith(TAG_CHECKED)) {
            cb.setChecked(true);
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            item = item.substring(TAG_CHECKED.length(), item.length()).trim();
        } else if (item.startsWith(TAG_UNCHECKED)) {
            cb.setChecked(false);
            edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            item = item.substring(TAG_UNCHECKED.length(), item.length()).trim();
        }

        edit.setOnTextViewChangeListener(this);
        edit.setIndex(index);
        edit.setText(getHighlightQueryResult(item, mUserQuery));  // 设置高亮文本

        return view;
    }

    public void onTextChange(int index, boolean hasText) {
        /**
         * 根据文本是否存在来更新对应的复选框可见性。
         *
         * @param index   当前编辑文本的索引
         * @param hasText 是否有文本
         */
        if (index >= mEditTextList.getChildCount()) {
            Log.e(TAG, "错误的索引，不应该发生");
            return;
        }

        CheckBox cb = (CheckBox) mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item);

        if (hasText) {
            cb.setVisibility(View.VISIBLE);  // 如果有文本，则显示复选框
        } else {
            cb.setVisibility(View.GONE);  // 如果没有文本，则隐藏复选框
        }
    }

    public void onCheckListModeChanged(int oldMode, int newMode) {
        /**
         * 根据新的模式切换到相应的编辑模式。
         *
         * @param oldMode 旧模式
         * @param newMode 新模式
         */
        if (newMode == TextNote.MODE_CHECK_LIST) {
            // 如果新模式是检查列表模式，则切换到列表模式
            switchToListMode(mNoteEditor.getText().toString());
        } else {
            // 如果新模式不是检查列表模式
            if (!getWorkingText()) {
                // 移除未选中标签
                mWorkingNote.setWorkingText(mWorkingNote.getContent().replace(TAG_UNCHECKED + " ", ""));
            }

            // 设置编辑器的文本，并高亮显示查询结果
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));

            // 隐藏列表并显示编辑器
            mEditTextList.setVisibility(View.GONE);
            mNoteEditor.setVisibility(View.VISIBLE);
        }
    }

    private boolean getWorkingText() {
        /**
         * 获取当前正在编辑的文本，并根据检查列表模式设置工作文本。
         *
         * @return 是否存在已选中的项
         */
        boolean hasChecked = false;

        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            // 构建字符串以保存当前编辑的文本
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < mEditTextList.getChildCount(); i++) {
                View view = mEditTextList.getChildAt(i);
                NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);

                if (!TextUtils.isEmpty(edit.getText())) {
                    CheckBox cb = (CheckBox) view.findViewById(R.id.cb_edit_item);

                    if (cb.isChecked()) {
                        // 如果项被选中，则添加已选中标记
                        sb.append(TAG_CHECKED).append(" ").append(edit.getText()).append("\n");
                        hasChecked = true;
                    } else {
                        // 如果项未被选中，则添加未选中标记
                        sb.append(TAG_UNCHECKED).append(" ").append(edit.getText()).append("\n");
                    }
                }
            }

            // 设置工作文本
            mWorkingNote.setWorkingText(sb.toString());
        } else {
            // 如果不是检查列表模式，则直接设置编辑器中的文本
            mWorkingNote.setWorkingText(mNoteEditor.getText().toString());
        }

        return hasChecked;
    }

    private boolean saveNote() {
        /**
         * 保存当前笔记，并根据保存状态设置返回结果。
         *
         * @return 是否成功保存
         */
        // 获取当前编辑的文本
        getWorkingText();

        boolean saved = mWorkingNote.saveNote();

        if (saved) {
            // 设置返回结果，用于区分创建或编辑状态
            setResult(RESULT_OK);
        }

        return saved;
    }

    private void sendToDesktop() {
        /**
         * 将当前编辑的笔记发送到桌面快捷方式。
         * 在发送之前，确保当前编辑的笔记存在于数据库中。
         * 对于新笔记，首先保存它。
         */
        if (!mWorkingNote.existInDatabase()) {
            // 如果笔记不存在于数据库中，则先保存笔记
            saveNote();
        }

        if (mWorkingNote.getNoteId() > 0) {
            // 创建 Intent 以发送到桌面
            Intent sender = new Intent();
            Intent shortcutIntent = new Intent(this, NoteEditActivity.class);
            shortcutIntent.setAction(Intent.ACTION_VIEW);
            shortcutIntent.putExtra(Intent.EXTRA_UID, mWorkingNote.getNoteId());
            sender.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            sender.putExtra(Intent.EXTRA_SHORTCUT_NAME,
                    makeShortcutIconTitle(mWorkingNote.getContent()));
            sender.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(this, R.drawable.icon_app));
            sender.putExtra("duplicate", true);
            sender.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            showToast(R.string.info_note_enter_desktop);
            sendBroadcast(sender);
        } else {
            // 如果用户没有输入任何内容（笔记不值得保存），提醒用户应该输入一些内容
            Log.e(TAG, "Send to desktop error");
            showToast(R.string.error_note_empty_for_send_to_desktop);
        }
    }

    private String makeShortcutIconTitle(String content) {
        /**
         * 生成桌面快捷方式的标题。
         * 去除已选中和未选中的标记，并截取部分内容作为标题。
         *
         * @param content 笔记内容
         * @return 短标题
         */
        content = content.replace(TAG_CHECKED, "");
        content = content.replace(TAG_UNCHECKED, "");

        if (content.length() > SHORTCUT_ICON_TITLE_MAX_LEN) {
            return content.substring(0, SHORTCUT_ICON_TITLE_MAX_LEN);
        } else {
            return content;
        }
    }

    private void showToast(int resId) {
        /**
         * 显示 Toast 消息，默认持续时间短。
         *
         * @param resId 资源 ID
         */
        showToast(resId, Toast.LENGTH_SHORT);
    }

    private void showToast(int resId, int duration) {
        /**
         * 显示 Toast 消息。
         *
         * @param resId   资源 ID
         * @param duration 持续时间
         */
        Toast.makeText(this, resId, duration).show();
    }

}
