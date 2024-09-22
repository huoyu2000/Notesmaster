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
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Display;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnCreateContextMenuListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.gtask.remote.GTaskSyncService;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.tool.BackupUtils;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.ui.NotesListAdapter.AppWidgetAttribute;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

public class NotesListActivity extends Activity implements OnClickListener, OnItemLongClickListener {
    private static final int FOLDER_NOTE_LIST_QUERY_TOKEN = 0;
    // 一个整型常量，用来标识查询笔记列表的后台查询任务的标记。
    private static final int FOLDER_LIST_QUERY_TOKEN = 1;
    // 另一个整型常量，标识查询文件夹列表的后台查询任务的标记。
    private static final int MENU_FOLDER_DELETE = 0;
    // 这些整型常量分别代表菜单选项中的删除文件夹、查看文件夹和更改文件夹名称的功能标识符。
    private static final int MENU_FOLDER_VIEW = 1;

    private static final int MENU_FOLDER_CHANGE_NAME = 2;
    // 字符串类型的常量，表示一个偏好设置键名，可能用于存储应用介绍相关的偏好设置
    private static final String PREFERENCE_ADD_INTRODUCTION = "net.micode.notes.introduction";

    private enum ListEditState {
        NOTE_LIST, SUB_FOLDER, CALL_RECORD_FOLDER
    };
    // 枚举类型，定义了三种不同的列表编辑状态：NOTE_LIST（笔记列表）、SUB_FOLDER（子文件夹）和CALL_RECORD_FOLDER（通话记录文件夹）。
    private ListEditState mState;
    // 成员变量，存储当前的列表编辑状态
    private BackgroundQueryHandler mBackgroundQueryHandler;
    // 成员变量，用于处理后台数据库查询的处理器对象。
    private NotesListAdapter mNotesListAdapter;
    // 成员变量，表示适配器对象，用于管理笔记列表视图的数据
    private ListView mNotesListView;
    // 成员变量，表示笔记列表视图组件。
    private Button mAddNewNote;
    // 成员变量，表示用于添加新笔记的按钮组件。
    private boolean mDispatch;
    // 成员变量，布尔型，  分发逻辑。
    private int mOriginY;
    // 成员变量，表示当前列表项的Y坐标
    private int mDispatchY;
    // 成员变量，表示当前列表项的Y坐标
    private TextView mTitleBar;
    // 成员变量，表示标题栏组件
    private long mCurrentFolderId;
    // 成员变量，表示当前文件夹的ID
    private ContentResolver mContentResolver;
    // 成员变量，表示内容解析器对象，用于访问数据库
    private ModeCallback mModeCallBack;
    // 成员变量，表示多选模式回调对象
    private static final String TAG = "NotesListActivity";
    // 标识符，表示日志标签
    public static final int NOTES_LISTVIEW_SCROLL_RATE = 30;
    // 滚动速率
    private NoteItemData mFocusNoteDataItem;
    // 成员变量，表示当前焦点的笔记数据对象
    private static final String NORMAL_SELECTION = NoteColumns.PARENT_ID + "=?";
    // 表示一个笔记列表查询的SQL语句，用于查询指定父文件夹ID下的所有笔记列表。
    private static final String ROOT_FOLDER_SELECTION = "(" + NoteColumns.TYPE + "<>"
            + Notes.TYPE_SYSTEM + " AND " + NoteColumns.PARENT_ID + "=?)" + " OR ("
            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER + " AND "
            + NoteColumns.NOTES_COUNT + ">0)";
    // 表示一个笔记列表查询的SQL语句，用于查询根文件夹下的所有笔记列表。
    private final static int REQUEST_CODE_OPEN_NODE = 102;
    // 表示打开笔记列表请求的请求码
    private final static int REQUEST_CODE_NEW_NODE  = 103;
    // 表示新建笔记列表请求的请求码
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_list);
        initResources();
        // 初始化各种资源
        /**
         * Insert an introduction when user firstly use this application
         */
        setAppInfoFromRawRes();
        // 设置应用介绍信息
    }

    // onActivityResult()方法用于处理打开笔记列表和新建笔记列表的返回结果。
    /*
        onActivityResult() 方法是在 Android 应用中处理 Activity 之间交互的一种重要机制。当一个 Activity 启动另一个 Activity 并等待其结果时，这个结果会在 onActivityResult() 方法中被处理。
        requestCode：请求码，用于标识启动的 Activity。
        resultCode：返回的结果码，表示 Activity 的执行结果。
        data：返回的数据 Intent，包含 Activity 返回的数据。
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK
                && (requestCode == REQUEST_CODE_OPEN_NODE || requestCode == REQUEST_CODE_NEW_NODE)) {
            mNotesListAdapter.changeCursor(null);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    // setAppInfoFromRawRes()方法用于设置应用介绍信息。
    private void setAppInfoFromRawRes() {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        // 这是一个SharedPreferences对象，用于存储应用介绍
        if (!sp.getBoolean(PREFERENCE_ADD_INTRODUCTION, false)) {// 如果没有设置过应用介绍，则执行以下代码块
            StringBuilder sb = new StringBuilder();// 创建一个StringBuilder对象，用于存储应用介绍文本
            /*
                StringBuilder 是一个可变字符序列，它允许在字符串末尾追加字符，并支持在字符串中插入、删除和替换字符等操作。
            */
            InputStream in = null;// 声明一个输入流对象，用于读取应用介绍文本
            /*
                InputStream 是一个抽象类，它表示字节输入流，用于从输入源读取字节数据。
             */
            try {
                in = getResources().openRawResource(R.raw.introduction);// 获取raw目录下的introduction文件
                if (in != null) {
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr);
                    char [] buf = new char[1024];
                    int len = 0;
                    while ((len = br.read(buf)) > 0) {
                        sb.append(buf, 0, len);// 将读取的字符串追加到sb中
                    }
                } else {
                    Log.e(TAG, "Read introduction file error");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            } finally {// 在finally块中关闭输入流对象
                /*
                    finally 块是 Java 中用于确保某些代码无论是否发生异常都能被执行的关键字。它通常与 try-catch 结构一起使用，以确保一些重要的清理工作能够完成。下面详细介绍 finally 块的作用及其使用方法。
                 */
                if(in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

            WorkingNote note = WorkingNote.createEmptyNote(this, Notes.ID_ROOT_FOLDER,
                    AppWidgetManager.INVALID_APPWIDGET_ID, Notes.TYPE_WIDGET_INVALIDE,
                    ResourceParser.RED);
            // 创建一个空的笔记对象，用于存储应用介绍文本 这里的参数暂时不用管
            /*
                WorkingNote 类是一个笔记对象，用于封装笔记的属性和行为。它封装了笔记的ID、内容、类型、背景颜色、最后修改日期等属性，并提供了保存笔记、删除笔记等方法。
             */
            note.setWorkingText(sb.toString());// 设置笔记的文本内容

            if (note.saveNote()) {
                sp.edit().putBoolean(PREFERENCE_ADD_INTRODUCTION, true).commit();// 将设置标志位保存到SharedPreferences中
            } else {
                Log.e(TAG, "Save introduction note error");
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startAsyncNotesListQuery();// 开始异步查询笔记列表
    }

    // initResources()方法用于初始化各种资源。
    private void initResources() {
        mContentResolver = this.getContentResolver();
        mBackgroundQueryHandler = new BackgroundQueryHandler(this.getContentResolver());
        mCurrentFolderId = Notes.ID_ROOT_FOLDER;
        mNotesListView = (ListView) findViewById(R.id.notes_list);
        mNotesListView.addFooterView(LayoutInflater.from(this).inflate(R.layout.note_list_footer, null),
                null, false);
        mNotesListView.setOnItemClickListener(new OnListItemClickListener());
        mNotesListView.setOnItemLongClickListener(this);
        mNotesListAdapter = new NotesListAdapter(this);
        mNotesListView.setAdapter(mNotesListAdapter);
        mAddNewNote = (Button) findViewById(R.id.btn_new_note);
        mAddNewNote.setOnClickListener(this);
        mAddNewNote.setOnTouchListener(new NewNoteOnTouchListener());
        mDispatch = false;
        mDispatchY = 0;
        mOriginY = 0;
        mTitleBar = (TextView) findViewById(R.id.tv_title_bar);
        mState = ListEditState.NOTE_LIST;
        mModeCallBack = new ModeCallback();
    }
    // 创建一个ModeCallback对象，用于处理多选模式下的事件。
    private class ModeCallback implements ListView.MultiChoiceModeListener, OnMenuItemClickListener {
        /*
            ListView.MultiChoiceModeListener 是 Android 中用于处理 ListView 多选模式的接口。
            当你需要在 ListView 中启用多选模式时，可以实现这个接口来处理多选模式下的各种事件。

            OnMenuItemClickListener 是 Android 中用于处理菜单项点击事件的接口。
            当你需要在 Toolbar、ActionBar 或者 PopupMenu 等组件中处理菜单项点击事件时，可以实现这个接口
         */
        private DropdownMenu mDropDownMenu;// 弹出菜单
        private ActionMode mActionMode;// ActionMode 对象
        /*
            ActionMode 是 Android 中的一个接口，它定义了在多选模式下的操作菜单。在多选模式下，用户可以选择多个项目，然后通过菜单进行操作，如删除、移动等。
         */
        private MenuItem mMoveMenu;// 移动菜单项
        /*
            MenuItem 是 Android 中的一个接口，它定义了菜单项，包括标题、图标等。
         */
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            /*
                onCreateActionMode 方法是 Android 框架的一部分，它属于 Activity 或者实现了 ActionMode.Callback 接口的类。这个方法会在用户开始一个上下文操作模式（Contextual Action Mode）时被调用。上下文操作模式允许用户选择多个项目，并对它们进行批量操作，比如删除、剪切、复制等。
                具体来说，onCreateActionMode 会在以下情况被调用：
                当用户在支持多选的 View（如 ListView, GridView 或 RecyclerView）中选择了第一个项目时。
                当用户手动开启了上下文操作栏（例如，通过长按某个列表项）。
                当 onCreateActionMode 被调用时，系统会创建一个新的 ActionMode 对象，并且给定一个 Menu 对象来填充上下文操作栏的菜单项。你可以在这个方法中设置菜单项以及它们的行为。
             */

            // 加载菜单资源文件到当前 ActionMode 的菜单中
            getMenuInflater().inflate(R.menu.note_list_options, menu);
            // 找到菜单中的 "删除" 项，并设置点击监听器
            menu.findItem(R.id.delete).setOnMenuItemClickListener(this);
            // 获取 "移动" 菜单项
            mMoveMenu = menu.findItem(R.id.move);
            // 根据条件判断是否禁用 "移动" 菜单项
            if (mFocusNoteDataItem.getParentId() == Notes.ID_CALL_RECORD_FOLDER
                    || DataUtils.getUserFolderCount(mContentResolver) == 0) {
                // 如果当前笔记位于通话记录文件夹或没有用户文件夹，则隐藏 "移动" 菜单项
                mMoveMenu.setVisible(false);
            } else {
                // 否则显示 "移动" 菜单项，并设置点击监听器
                mMoveMenu.setVisible(true);
                mMoveMenu.setOnMenuItemClickListener(this);
            }
            // 保存当前的 ActionMode 实例
            mActionMode = mode;
            // 设置适配器的选择模式为开启状态
            mNotesListAdapter.setChoiceMode(true);
            // 禁用 ListView 的长按事件
            mNotesListView.setLongClickable(false);
            // 隐藏添加新笔记按钮
            mAddNewNote.setVisibility(View.GONE);
            // 从布局文件中加载自定义视图
            View customView = LayoutInflater.from(NotesListActivity.this).inflate(
                    R.layout.note_list_dropdown_menu, null);
            // 设置 ActionMode 的自定义视图
            mode.setCustomView(customView);
            // 创建 DropdownMenu 实例
            mDropDownMenu = new DropdownMenu(NotesListActivity.this,
                    (Button) customView.findViewById(R.id.selection_menu),
                    R.menu.note_list_dropdown);
            // 设置 DropdownMenu 的点击监听器
            mDropDownMenu.setOnDropdownMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    // 切换所有笔记的选中状态
                    mNotesListAdapter.selectAll(!mNotesListAdapter.isAllSelected());
                    // 更新菜单状态
                    updateMenu();
                    // 表示事件已处理
                    return true;
                }
            });
            // 表示事件已处理
            return true;
        }


        private void updateMenu() {
            // 获取当前选中的项目数量
            int selectedCount = mNotesListAdapter.getSelectedCount();
            // 更新下拉菜单的标题
            // 使用资源文件中的字符串，并传入选中项目的数量
            String format = getResources().getString(R.string.menu_select_title, selectedCount);
            mDropDownMenu.setTitle(format);
            // 查找 "全选" 菜单项
            MenuItem item = mDropDownMenu.findItem(R.id.action_select_all);
            // 如果找到了 "全选" 菜单项
            if (item != null) {
                // 如果所有项目都被选中
                if (mNotesListAdapter.isAllSelected()) {
                    // 设置 "全选" 菜单项为选中状态
                    item.setChecked(true);
                    // 将 "全选" 菜单项的标题改为 "取消全选"
                    item.setTitle(R.string.menu_deselect_all);
                } else {
                    // 如果不是所有项目都被选中
                    // 设置 "全选" 菜单项为未选中状态
                    item.setChecked(false);
                    // 将 "全选" 菜单项的标题改回 "全选"
                    item.setTitle(R.string.menu_select_all);
                }
            }
        }


        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            // TODO Auto-generated method stub
            return false;
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            // TODO Auto-generated method stub
            return false;
        }

        public void onDestroyActionMode(ActionMode mode) {
            mNotesListAdapter.setChoiceMode(false);
            mNotesListView.setLongClickable(true);
            mAddNewNote.setVisibility(View.VISIBLE);
        }

        public void finishActionMode() {
            mActionMode.finish();
        }

        public void onItemCheckedStateChanged(ActionMode mode, int position, long id,
                boolean checked) {
            mNotesListAdapter.setCheckedItem(position, checked);
            updateMenu();
        }

        public boolean onMenuItemClick(MenuItem item) {
            // 检查是否有项目被选中
            if (mNotesListAdapter.getSelectedCount() == 0) {
                // 如果没有项目被选中，显示提示信息
                Toast.makeText(NotesListActivity.this, getString(R.string.menu_select_none),
                        Toast.LENGTH_SHORT).show();
                return true;// 表示事件已处理
            }

            switch (item.getItemId()) {
                case R.id.delete:// 如果点击的是删除
                    // 创建一个对话框构建器
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    // 设置对话框的标题
                    builder.setTitle(getString(R.string.alert_title_delete));
                    // 设置对话框的图标
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    // 设置对话框的消息
                    builder.setMessage(getString(R.string.alert_message_delete_notes,
                                             mNotesListAdapter.getSelectedCount()));
                    // 设置确认按钮及其点击事件
                    builder.setPositiveButton(android.R.string.ok,
                                             new DialogInterface.OnClickListener() {
                                                 public void onClick(DialogInterface dialog,
                                                         int which) {
                                                     batchDelete();
                                                 }
                                             });
                    // 设置取消按钮
                    builder.setNegativeButton(android.R.string.cancel, null);
                    // 显示对话框
                    builder.show();
                    break;
                case R.id.move:
                    // 开始查询目标文件夹
                    startQueryDestinationFolders();
                    break;
                default:
                    // 如果是其他菜单项，则返回 false
                    return false;
            }
            // 表示事件已处理
            return true;
        }
    }

    private class NewNoteOnTouchListener implements OnTouchListener {
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    // 获取屏幕的高度
                    Display display = getWindowManager().getDefaultDisplay();
                    int screenHeight = display.getHeight();
                    // 获取新建笔记视图的高度
                    int newNoteViewHeight = mAddNewNote.getHeight();
                    // 计算新建笔记视图在屏幕上的起始位置
                    int start = screenHeight - newNoteViewHeight;
                    // 计算触摸事件的 Y 坐标
                    int eventY = start + (int) event.getY();
                    // 如果当前状态为子文件夹状态，减去标题栏的高度
                    if (mState == ListEditState.SUB_FOLDER) {
                        eventY -= mTitleBar.getHeight();
                        start -= mTitleBar.getHeight();
                    }
                    // 处理点击透明部分的情况
                    // 如果点击的是新建笔记按钮的透明部分，则将事件传递给列表视图
                    if (event.getY() < (event.getX() * (-0.12) + 94)) {
                        // 获取列表视图的最后一个子视图
                        View view = mNotesListView.getChildAt(mNotesListView.getChildCount() - 1
                                - mNotesListView.getFooterViewsCount());
                        // 检查子视图是否有效，并且在指定范围内
                        if (view != null && view.getBottom() > start && (view.getTop() < (start + 94))) {
                            // 记录原始 Y 坐标
                            mOriginY = (int) event.getY();
                            // 计算新的 Y 坐标
                            mDispatchY = eventY;
                            // 更新事件的 Y 坐标
                            event.setLocation(event.getX(), mDispatchY);
                            // 设置标记，表示需要分发事件
                            mDispatch = true;
                            // 分发触摸事件到列表视图
                            return mNotesListView.dispatchTouchEvent(event);
                        }
                    }
                    break;
                }
                case MotionEvent.ACTION_MOVE: {
                    // 如果需要分发事件
                    if (mDispatch) {
                        // 更新 Y 坐标的偏移量
                        mDispatchY += (int) event.getY() - mOriginY;
                        // 更新事件的 Y 坐标
                        event.setLocation(event.getX(), mDispatchY);
                        // 分发触摸事件到列表视图
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
                default: {
                    // 如果需要分发事件
                    if (mDispatch) {
                        // 更新事件的 Y 坐标
                        event.setLocation(event.getX(), mDispatchY);
                        // 清除分发标记
                        mDispatch = false;
                        // 分发触摸事件到列表视图
                        return mNotesListView.dispatchTouchEvent(event);
                    }
                    break;
                }
            }
            return false;
        }
    }

    private void startAsyncNotesListQuery() {
        // 根据当前文件夹 ID 确定查询条件
        String selection = (mCurrentFolderId == Notes.ID_ROOT_FOLDER) ? ROOT_FOLDER_SELECTION
                : NORMAL_SELECTION;
        // 启动异步查询
        mBackgroundQueryHandler.startQuery(
                FOLDER_NOTE_LIST_QUERY_TOKEN,  // 查询标识符
                null,                          // 查询参数（可选）
                Notes.CONTENT_NOTE_URI,        // 内容 URI
                NoteItemData.PROJECTION,       // 查询列
                selection,                     // 查询条件
                new String[] {                 // 查询条件参数
                        String.valueOf(mCurrentFolderId)
                },
                NoteColumns.TYPE + " DESC," + NoteColumns.MODIFIED_DATE + " DESC"  // 排序字段
        );
    }


    private final class BackgroundQueryHandler extends AsyncQueryHandler {
        /*
            AsyncQueryHandler 是 Android 中用于执行异步查询的一个类，它继承自 Handler 并提供了异步查询的能力。
            AsyncQueryHandler 可以帮助你在后台线程中执行数据库查询，并在主线程中处理查询结果，从而避免阻塞 UI 线程。
         */
        public BackgroundQueryHandler(ContentResolver contentResolver) {
            super(contentResolver);
        }
        @Override
        protected void onQueryComplete(int token, Object cookie, Cursor cursor) {//在主线程中处理查询结果
            switch (token) {
                case FOLDER_NOTE_LIST_QUERY_TOKEN:
                    // 当查询完成时，更新笔记列表适配器的游标
                    mNotesListAdapter.changeCursor(cursor);
                    break;
                case FOLDER_LIST_QUERY_TOKEN:
                    // 当查询完成时，处理文件夹列表
                    if (cursor != null && cursor.getCount() > 0) {
                        // 如果游标不为空且有数据，则显示文件夹列表菜单
                        showFolderListMenu(cursor);
                    } else {
                        // 如果查询失败或没有数据，则记录错误日志
                        Log.e(TAG, "Query folder failed");
                    }
                    break;
                default:
                    // 对于其他未知的查询标识符，不做任何处理
                    return;
            }
        }
    }
    // 展示文件夹列表菜单
    private void showFolderListMenu(Cursor cursor) {
        // 创建 AlertDialog.Builder 实例
        AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
        // 设置对话框标题
        builder.setTitle(R.string.menu_title_select_folder);
        // 创建 FoldersListAdapter 实例
        final FoldersListAdapter adapter = new FoldersListAdapter(this, cursor);
        // 设置对话框的适配器和点击监听器
        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取选定文件夹的 ID
                long folderId = adapter.getItemId(which);
                // 批量移动选定的笔记到指定文件夹
                DataUtils.batchMoveToFolder(mContentResolver,
                        mNotesListAdapter.getSelectedItemIds(), folderId);
                // 显示提示信息
                int selectedCount = mNotesListAdapter.getSelectedCount();
                String folderName = adapter.getFolderName(NotesListActivity.this, which);
                /*
                    在 strings.xml 文件中定义一个带有占位符的字符串资源，并在 Java 代码中使用 getString 方法传递实际的值。
                 */
                String message = getString(R.string.format_move_notes_to_folder, selectedCount, folderName);
                Toast.makeText(NotesListActivity.this, message, Toast.LENGTH_SHORT).show();
                // 结束 Action Mode
                mModeCallBack.finishActionMode();
            }
        });

        // 显示对话框
        builder.show();
    }


    private void createNewNote() {
        // 创建一个新的 Intent 对象，用于启动 NoteEditActivity
        Intent intent = new Intent(this, NoteEditActivity.class);
        // 设置 Intent 的操作为 ACTION_INSERT_OR_EDIT，表示这是一个插入或编辑操作
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        // 将当前文件夹的 ID 作为额外数据附加到 Intent 中
        // 这使得 NoteEditActivity 可以知道当前操作对应的文件夹
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mCurrentFolderId);
        // 启动 NoteEditActivity，并请求返回结果
        // REQUEST_CODE_NEW_NODE 是一个请求码，用于标识此 startActivityForResult 调用
        this.startActivityForResult(intent, REQUEST_CODE_NEW_NODE);
    }


    private void batchDelete() {
        // 创建一个异步任务（AsyncTask）来执行批量删除操作
        new AsyncTask<Void, Void, HashSet<AppWidgetAttribute>>() {
            /*
                AppWidgetAttribute 类用于表示小部件的属性，包括小部件的 ID 和类型
                是作者自己定义的类 之后再详看
             */
            @Override
            protected HashSet<AppWidgetAttribute> doInBackground(Void... unused) {
                // 获取当前选中的小部件集合
                HashSet<AppWidgetAttribute> widgets = mNotesListAdapter.getSelectedWidget();
                // 根据同步模式的不同，执行不同的操作
                if (!isSyncMode()) {
                    // 如果不是同步模式，直接删除笔记
                    /*
                        DataUtils是作者自己定义的类，封装了笔记数据的操作，如插入、更新、删除等操作
                     */
                    if (DataUtils.batchDeleteNotes(mContentResolver, mNotesListAdapter
                            .getSelectedItemIds())) {
                        // 删除成功
                    } else {
                        // 删除失败，记录错误日志
                        Log.e(TAG, "Delete notes error, should not happen");
                    }
                } else {
                    // 如果是同步模式，将删除的笔记移动到回收站文件夹
                    if (!DataUtils.batchMoveToFolder(mContentResolver, mNotesListAdapter
                            .getSelectedItemIds(), Notes.ID_TRASH_FOLER)) {
                        // 移动失败，记录错误日志
                        Log.e(TAG, "Move notes to trash folder error, should not happen");
                    }
                }
                // 返回选中的小部件集合
                return widgets;
            }
            @Override
            protected void onPostExecute(HashSet<AppWidgetAttribute> widgets) {
                if (widgets != null) {
                    // 遍历选中的小部件集合
                    for (AppWidgetAttribute widget : widgets) {
                        if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                                && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                            // 更新小部件
                            updateWidget(widget.widgetId, widget.widgetType);
                        }
                    }
                }
                // 结束操作模式
                mModeCallBack.finishActionMode();
            }
        }.execute();
    }

    private void deleteFolder(long folderId) {
        // 检查是否尝试删除根文件夹
        if (folderId == Notes.ID_ROOT_FOLDER) {
            Log.e(TAG, "Wrong folder id, should not happen " + folderId);
            return;
        }
        // 创建一个 HashSet 来存储要删除的文件夹 ID
        HashSet<Long> ids = new HashSet<Long>();
        ids.add(folderId);
        // 获取指定文件夹下的所有小部件
        HashSet<AppWidgetAttribute> widgets = DataUtils.getFolderNoteWidget(mContentResolver, folderId);
        // 根据同步模式的不同，执行不同的操作
        if (!isSyncMode()) {
            // 如果不是同步模式，直接删除文件夹及其笔记
            DataUtils.batchDeleteNotes(mContentResolver, ids);
        } else {
            // 如果是同步模式，将删除的文件夹移动到回收站文件夹
            DataUtils.batchMoveToFolder(mContentResolver, ids, Notes.ID_TRASH_FOLER);
        }
        // 如果有相关的小部件，则更新它们
        if (widgets != null) {
            for (AppWidgetAttribute widget : widgets) {
                if (widget.widgetId != AppWidgetManager.INVALID_APPWIDGET_ID
                        && widget.widgetType != Notes.TYPE_WIDGET_INVALIDE) {
                    // 更新小部件
                    updateWidget(widget.widgetId, widget.widgetType);
                }
            }
        }
    }
    private void openNode(NoteItemData data) {
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_UID, data.getId());
        this.startActivityForResult(intent, REQUEST_CODE_OPEN_NODE);
    }

    private void openFolder(NoteItemData data) {
        // 设置当前文件夹 ID
        mCurrentFolderId = data.getId();
        // 启动异步查询笔记列表
        startAsyncNotesListQuery();
        // 根据文件夹 ID 设置不同的状态和 UI 显示
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 如果是通话记录文件夹
            mState = ListEditState.CALL_RECORD_FOLDER;
            mAddNewNote.setVisibility(View.GONE);
        } else {
            // 如果是普通子文件夹
            mState = ListEditState.SUB_FOLDER;
        }
        // 设置标题栏文本
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 如果是通话记录文件夹
            mTitleBar.setText(R.string.call_record_folder_name);
        } else {
            // 如果是普通子文件夹
            mTitleBar.setText(data.getSnippet());
        }
        // 显示标题栏
        mTitleBar.setVisibility(View.VISIBLE);
    }
    public void onClick(View v) {
        // 处理点击事件
        switch (v.getId()) {
            case R.id.btn_new_note:
                // 如果点击的是新建笔记按钮
                createNewNote();
                break;
            default:
                // 其他情况不做处理
                break;
        }
    }
    private void showSoftInput() {
        // 获取输入法管理器
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            // 显示软键盘
            inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
        }
    }

    private void hideSoftInput(View view) {
        // 获取输入法管理器
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            // 隐藏软键盘
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void showCreateOrModifyFolderDialog(final boolean create) {
        // 创建对话框构建器
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 加载自定义布局
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        final EditText etName = (EditText) view.findViewById(R.id.et_foler_name);
        // 显示软键盘
        showSoftInput();
        // 根据是否创建新文件夹设置对话框内容
        if (!create) {
            if (mFocusNoteDataItem != null) {
                // 如果是修改文件夹名称
                etName.setText(mFocusNoteDataItem.getSnippet());
                builder.setTitle(getString(R.string.menu_folder_change_name));
            } else {
                // 如果长按的数据项为空，记录错误日志并返回
                Log.e(TAG, "The long click data item is null");
                return;
            }
        } else {
            // 如果是创建新文件夹
            etName.setText("");
            builder.setTitle(this.getString(R.string.menu_create_folder));
        }
        // 设置确定按钮
        builder.setPositiveButton(android.R.string.ok, null);
        // 设置取消按钮
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // 隐藏软键盘
                hideSoftInput(etName);
            }
        });
        // 显示对话框
        final Dialog dialog = builder.setView(view).show();
        // 获取确定按钮
        final Button positive = (Button) dialog.findViewById(android.R.id.button1);
        positive.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // 隐藏软键盘
                hideSoftInput(etName);
                // 获取输入的文件夹名称
                String name = etName.getText().toString();
                // 检查文件夹名称是否存在
                if (DataUtils.checkVisibleFolderName(mContentResolver, name)) {
                    // 如果文件夹名称已存在，显示提示信息
                    Toast.makeText(NotesListActivity.this, getString(R.string.folder_exist, name),
                            Toast.LENGTH_LONG).show();
                    etName.setSelection(0, etName.length());
                    return;
                }
                // 修改或创建文件夹
                if (!create) {
                    if (!TextUtils.isEmpty(name)) {
                        // 修改现有文件夹
                        ContentValues values = new ContentValues();
                        values.put(NoteColumns.SNIPPET, name);
                        values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
                        values.put(NoteColumns.LOCAL_MODIFIED, 1);
                        mContentResolver.update(Notes.CONTENT_NOTE_URI, values, NoteColumns.ID + "=?",
                                new String[] { String.valueOf(mFocusNoteDataItem.getId()) });
                    }
                } else if (!TextUtils.isEmpty(name)) {
                    // 创建新文件夹
                    ContentValues values = new ContentValues();
                    values.put(NoteColumns.SNIPPET, name);
                    values.put(NoteColumns.TYPE, Notes.TYPE_FOLDER);
                    mContentResolver.insert(Notes.CONTENT_NOTE_URI, values);
                }
                // 关闭对话框
                dialog.dismiss();
            }
        });
        // 初始状态下，如果输入为空，则禁用确定按钮
        if (TextUtils.isEmpty(etName.getText())) {
            positive.setEnabled(false);
        }
        // 监听输入框文本变化，以启用或禁用确定按钮
        etName.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // 文本变化前的回调
            }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 文本变化中的回调
                if (TextUtils.isEmpty(etName.getText())) {
                    positive.setEnabled(false);
                } else {
                    positive.setEnabled(true);
                }
            }
            public void afterTextChanged(Editable s) {
                // 文本变化后的回调
            }
        });
    }


    @Override
    public void onBackPressed() {
        // 根据当前状态处理返回键事件
        switch (mState) {
            case SUB_FOLDER:
                // 当前状态为子文件夹
                mCurrentFolderId = Notes.ID_ROOT_FOLDER; // 设置当前文件夹 ID 为根文件夹 ID
                mState = ListEditState.NOTE_LIST; // 更改状态为笔记列表
                startAsyncNotesListQuery(); // 启动异步查询笔记列表
                mTitleBar.setVisibility(View.GONE); // 隐藏标题栏
                break;
            case CALL_RECORD_FOLDER:
                // 当前状态为通话记录文件夹
                mCurrentFolderId = Notes.ID_ROOT_FOLDER; // 设置当前文件夹 ID 为根文件夹 ID
                mState = ListEditState.NOTE_LIST; // 更改状态为笔记列表
                mAddNewNote.setVisibility(View.VISIBLE); // 显示添加新笔记按钮
                mTitleBar.setVisibility(View.GONE); // 隐藏标题栏
                startAsyncNotesListQuery(); // 启动异步查询笔记列表
                break;
            case NOTE_LIST:
                // 当前状态为笔记列表
                super.onBackPressed(); // 调用父类的返回键处理方法
                break;
            default:
                // 默认情况下不做处理
                break;
        }
    }


    private void updateWidget(int appWidgetId, int appWidgetType) {
        // 创建更新小部件的 Intent
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // 根据不同的小部件类型设置 Intent 的目标类
        if (appWidgetType == Notes.TYPE_WIDGET_2X) {
            // 如果是 2x 小部件类型
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (appWidgetType == Notes.TYPE_WIDGET_4X) {
            // 如果是 4x 小部件类型
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            // 如果是不支持的小部件类型，记录错误日志并返回
            Log.e(TAG, "Unsupported widget type");
            return;
        }
        // 将小部件 ID 添加到 Intent 中
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[] { appWidgetId });
        // 发送广播更新小部件
        sendBroadcast(intent);
        // 设置结果为成功，并附带 Intent
        setResult(RESULT_OK, intent);
    }


    private final OnCreateContextMenuListener mFolderOnCreateContextMenuListener = new OnCreateContextMenuListener() {
        public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
            // 如果当前聚焦的数据项不为空
            if (mFocusNoteDataItem != null) {
                // 设置菜单的标题为数据项的摘要
                menu.setHeaderTitle(mFocusNoteDataItem.getSnippet());
                // 添加查看文件夹选项
                menu.add(0, MENU_FOLDER_VIEW, 0, R.string.menu_folder_view);
                // 添加删除文件夹选项
                menu.add(0, MENU_FOLDER_DELETE, 0, R.string.menu_folder_delete);
                // 添加修改文件夹名称选项
                menu.add(0, MENU_FOLDER_CHANGE_NAME, 0, R.string.menu_folder_change_name);
            }
        }
    };

    @Override
    public void onContextMenuClosed(Menu menu) {
        // 如果笔记列表视图不为空
        if (mNotesListView != null) {
            // 移除上下文菜单创建监听器
            mNotesListView.setOnCreateContextMenuListener(null);
        }
        // 调用父类的方法
        super.onContextMenuClosed(menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        // 如果当前聚焦的数据项为空，则记录错误日志并返回 false
        if (mFocusNoteDataItem == null) {
            Log.e(TAG, "The long click data item is null");
            return false;
        }
        // 根据选中的菜单项执行相应的操作
        switch (item.getItemId()) {
            case MENU_FOLDER_VIEW:
                // 打开文件夹
                openFolder(mFocusNoteDataItem);
                break;
            case MENU_FOLDER_DELETE:
                // 创建一个对话框来确认删除文件夹
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.alert_title_delete));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(getString(R.string.alert_message_delete_folder));
                builder.setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // 删除文件夹
                                deleteFolder(mFocusNoteDataItem.getId());
                            }
                        });
                builder.setNegativeButton(android.R.string.cancel, null);
                builder.show();
                break;
            case MENU_FOLDER_CHANGE_NAME:
                // 显示修改文件夹名称的对话框
                showCreateOrModifyFolderDialog(false);
                break;
            default:
                // 默认情况下不做处理
                break;
        }
        // 返回 true 表示已处理此菜单项
        return true;
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 清空当前菜单
        menu.clear();

        // 根据当前状态加载不同的菜单资源
        if (mState == ListEditState.NOTE_LIST) {
            // 如果状态为 NOTE_LIST，则加载 note_list 菜单资源
            getMenuInflater().inflate(R.menu.note_list, menu);
            // 设置同步或取消同步的菜单项标题
            menu.findItem(R.id.menu_sync).setTitle(
                    GTaskSyncService.isSyncing() ? R.string.menu_sync_cancel : R.string.menu_sync);
        } else if (mState == ListEditState.SUB_FOLDER) {
            // 如果状态为 SUB_FOLDER，则加载 sub_folder 菜单资源
            getMenuInflater().inflate(R.menu.sub_folder, menu);
        } else if (mState == ListEditState.CALL_RECORD_FOLDER) {
            // 如果状态为 CALL_RECORD_FOLDER，则加载 call_record_folder 菜单资源
            getMenuInflater().inflate(R.menu.call_record_folder, menu);
        } else {
            // 如果状态不正确，则记录错误日志
            Log.e(TAG, "Wrong state:" + mState);
        }
        // 返回 true 表示菜单已准备好
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 根据选中的菜单项执行相应的操作
        switch (item.getItemId()) {
            case R.id.menu_new_folder: {
                // 显示新建文件夹对话框
                showCreateOrModifyFolderDialog(true);
                break;
            }
            case R.id.menu_export_text: {
                // 导出笔记为文本
                exportNoteToText();
                break;
            }
            case R.id.menu_sync: {
                // 处理同步菜单项
                if (isSyncMode()) {
                    if (TextUtils.equals(item.getTitle(), getString(R.string.menu_sync))) {
                        // 如果当前菜单标题为“同步”，则开始同步
                        GTaskSyncService.startSync(this);
                    } else {
                        // 如果当前菜单标题为“取消同步”，则取消同步
                        GTaskSyncService.cancelSync(this);
                    }
                } else {
                    // 如果不是同步模式，则打开设置活动
                    startPreferenceActivity();
                }
                break;
            }
            case R.id.menu_setting: {
                // 打开设置活动
                startPreferenceActivity();
                break;
            }
            case R.id.menu_new_note: {
                // 创建新笔记
                createNewNote();
                break;
            }
            case R.id.menu_search: {
                // 触发搜索请求
                onSearchRequested();
                break;
            }
            default:
                // 默认情况下不做处理
                break;
        }
        // 返回 true 表示已处理此菜单项
        return true;
    }

    @Override
    public boolean onSearchRequested() {
        // 开始搜索
        startSearch(null, false, null /* appData */, false);
        return true;
    }

    private void exportNoteToText() {
        // 获取备份工具实例
        final BackupUtils backup = BackupUtils.getInstance(NotesListActivity.this);

        // 创建并执行一个异步任务来导出笔记为文本文件
        new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... unused) {
                // 在后台线程中执行导出操作，并返回结果
                return backup.exportToText();
            }

            @Override
            protected void onPostExecute(Integer result) {
                // 根据导出结果展示不同的对话框
                if (result == BackupUtils.STATE_SD_CARD_UNMOUONTED) {
                    // 如果 SD 卡未挂载，则显示错误提示
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.failed_sdcard_export));
                    builder.setMessage(NotesListActivity.this
                            .getString(R.string.error_sdcard_unmounted));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                } else if (result == BackupUtils.STATE_SUCCESS) {
                    // 如果导出成功，则显示成功提示
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.success_sdcard_export));
                    builder.setMessage(NotesListActivity.this.getString(
                            R.string.format_exported_file_location,
                            backup.getExportedTextFileName(),
                            backup.getExportedTextFileDir()));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                } else if (result == BackupUtils.STATE_SYSTEM_ERROR) {
                    // 如果发生系统错误，则显示错误提示
                    AlertDialog.Builder builder = new AlertDialog.Builder(NotesListActivity.this);
                    builder.setTitle(NotesListActivity.this
                            .getString(R.string.failed_sdcard_export));
                    builder.setMessage(NotesListActivity.this
                            .getString(R.string.error_sdcard_export));
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.show();
                }
            }

        }.execute();
    }

    private boolean isSyncMode() {
        // 检查是否处于同步模式
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
    }

    private void startPreferenceActivity() {
        // 获取父 Activity 或当前 Activity
        Activity from = getParent() != null ? getParent() : this;
        // 创建 Intent 并启动 Preferences Activity
        Intent intent = new Intent(from, NotesPreferenceActivity.class);
        from.startActivityIfNeeded(intent, -1);
    }

    private class OnListItemClickListener implements OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            // 确认视图是 NotesListItem 类型
            if (view instanceof NotesListItem) {
                NoteItemData item = ((NotesListItem) view).getItemData();
                // 如果处于选择模式
                if (mNotesListAdapter.isInChoiceMode()) {
                    if (item.getType() == Notes.TYPE_NOTE) {
                        // 调整位置以排除头部视图
                        position = position - mNotesListView.getHeaderViewsCount();
                        // 切换选择状态
                        mModeCallBack.onItemCheckedStateChanged(null, position, id,
                                !mNotesListAdapter.isSelectedItem(position));
                    }
                    return;
                }
                // 根据当前状态处理点击事件
                switch (mState) {
                    case NOTE_LIST:
                        if (item.getType() == Notes.TYPE_FOLDER
                                || item.getType() == Notes.TYPE_SYSTEM) {
                            // 打开文件夹
                            openFolder(item);
                        } else if (item.getType() == Notes.TYPE_NOTE) {
                            // 打开笔记
                            openNode(item);
                        } else {
                            // 错误的日志记录
                            Log.e(TAG, "Wrong note type in NOTE_LIST");
                        }
                        break;
                    case SUB_FOLDER:
                    case CALL_RECORD_FOLDER:
                        if (item.getType() == Notes.TYPE_NOTE) {
                            // 打开笔记
                            openNode(item);
                        } else {
                            // 错误的日志记录
                            Log.e(TAG, "Wrong note type in SUB_FOLDER");
                        }
                        break;
                    default:
                        // 默认情况不做处理
                        break;
                }
            }
        }
    }

    private void startQueryDestinationFolders() {
        // 构建查询条件
        String selection = NoteColumns.TYPE + "=? AND " + NoteColumns.PARENT_ID + "<>? AND " + NoteColumns.ID + "<>?";

        // 根据当前状态调整查询条件
        if (mState == ListEditState.NOTE_LIST) {
            selection = selection;
        } else {
            selection = "(" + selection + ") OR (" + NoteColumns.ID + "=" + Notes.ID_ROOT_FOLDER + ")";
        }

        // 开始后台查询
        mBackgroundQueryHandler.startQuery(
                FOLDER_LIST_QUERY_TOKEN,
                null,
                Notes.CONTENT_NOTE_URI,
                FoldersListAdapter.PROJECTION,
                selection,
                new String[] {
                        String.valueOf(Notes.TYPE_FOLDER),
                        String.valueOf(Notes.ID_TRASH_FOLER),
                        String.valueOf(mCurrentFolderId)
                },
                NoteColumns.MODIFIED_DATE + " DESC"
        );
    }

    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // 确认视图是 NotesListItem 类型
        if (view instanceof NotesListItem) {
            mFocusNoteDataItem = ((NotesListItem) view).getItemData();

            // 如果是笔记且不在选择模式
            if (mFocusNoteDataItem.getType() == Notes.TYPE_NOTE && !mNotesListAdapter.isInChoiceMode()) {
                // 启动 Action Mode
                if (mNotesListView.startActionMode(mModeCallBack) != null) {
                    mModeCallBack.onItemCheckedStateChanged(null, position, id, true);
                    mNotesListView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                } else {
                    // 启动 Action Mode 失败，记录错误日志
                    Log.e(TAG, "startActionMode fails");
                }
            } else if (mFocusNoteDataItem.getType() == Notes.TYPE_FOLDER) {
                // 设置上下文菜单监听器
                mNotesListView.setOnCreateContextMenuListener(mFolderOnCreateContextMenuListener);
            }
        }
        // 返回 false 表示不消耗长按事件
        return false;
    }

}
