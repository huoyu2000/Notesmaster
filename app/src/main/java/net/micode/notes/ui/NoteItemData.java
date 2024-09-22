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

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import net.micode.notes.data.Contact;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;
import net.micode.notes.tool.DataUtils;


public class NoteItemData {
    // 定义查询投影数组，包含所有需要查询的列
    static final String[] PROJECTION = new String[] {
            NoteColumns.ID,                // 0. ID 列
            NoteColumns.ALERTED_DATE,      // 1. 提醒日期列
            NoteColumns.BG_COLOR_ID,       // 2. 背景颜色 ID 列
            NoteColumns.CREATED_DATE,      // 3. 创建日期列
            NoteColumns.HAS_ATTACHMENT,    // 4. 是否有附件列
            NoteColumns.MODIFIED_DATE,     // 5. 修改日期列
            NoteColumns.NOTES_COUNT,       // 6. 笔记数量列
            NoteColumns.PARENT_ID,         // 7. 父项 ID 列
            NoteColumns.SNIPPET,           // 8. 摘要列
            NoteColumns.TYPE,              // 9. 类型列
            NoteColumns.WIDGET_ID,         // 10. 小部件 ID 列
            NoteColumns.WIDGET_TYPE        // 11. 小部件类型列
    };

    // 定义每个列对应的索引
    private static final int ID_COLUMN                    = 0;          // ID 列索引
    private static final int ALERTED_DATE_COLUMN          = 1;          // 提醒日期列索引
    private static final int BG_COLOR_ID_COLUMN           = 2;          // 背景颜色 ID 列索引
    private static final int CREATED_DATE_COLUMN          = 3;          // 创建日期列索引
    private static final int HAS_ATTACHMENT_COLUMN        = 4;          // 是否有附件列索引
    private static final int MODIFIED_DATE_COLUMN         = 5;          // 修改日期列索引
    private static final int NOTES_COUNT_COLUMN           = 6;          // 笔记数量列索引
    private static final int PARENT_ID_COLUMN             = 7;          // 父项 ID 列索引
    private static final int SNIPPET_COLUMN               = 8;          // 摘要列索引
    private static final int TYPE_COLUMN                  = 9;          // 类型列索引
    private static final int WIDGET_ID_COLUMN             = 10;         // 小部件 ID 列索引
    private static final int WIDGET_TYPE_COLUMN           = 11;         // 小部件类型列索引

    // ID
    private long mId;

    // 提醒日期
    private long mAlertDate;

    // 背景颜色 ID
    private int mBgColorId;

    // 创建日期
    private long mCreatedDate;

    // 是否有附件
    private boolean mHasAttachment;

    // 修改日期
    private long mModifiedDate;

    // 笔记数量
    private int mNotesCount;

    // 父项 ID
    private long mParentId;

    // 摘要
    private String mSnippet;

    // 类型
    private int mType;

    // 小部件 ID
    private int mWidgetId;

    // 小部件类型
    private int mWidgetType;

    // 名称
    private String mName;

    // 电话号码
    private String mPhoneNumber;

    // 是否是最后一项
    private boolean mIsLastItem;

    // 是否是第一项
    private boolean mIsFirstItem;

    // 是否是唯一一项
    private boolean mIsOnlyOneItem;

    // 是否是一个笔记跟随文件夹
    private boolean mIsOneNoteFollowingFolder;

    // 是否是多个笔记跟随文件夹
    private boolean mIsMultiNotesFollowingFolder;

    public NoteItemData(Context context, Cursor cursor) {
        // 从 Cursor 中获取 ID
        mId = cursor.getLong(ID_COLUMN);

        // 从 Cursor 中获取提醒日期
        mAlertDate = cursor.getLong(ALERTED_DATE_COLUMN);

        // 从 Cursor 中获取背景颜色 ID
        mBgColorId = cursor.getInt(BG_COLOR_ID_COLUMN);

        // 从 Cursor 中获取创建日期
        mCreatedDate = cursor.getLong(CREATED_DATE_COLUMN);

        // 从 Cursor 中获取是否有附件的状态
        mHasAttachment = (cursor.getInt(HAS_ATTACHMENT_COLUMN) > 0) ? true : false;

        // 从 Cursor 中获取修改日期
        mModifiedDate = cursor.getLong(MODIFIED_DATE_COLUMN);

        // 从 Cursor 中获取笔记数量
        mNotesCount = cursor.getInt(NOTES_COUNT_COLUMN);

        // 从 Cursor 中获取父项 ID
        mParentId = cursor.getLong(PARENT_ID_COLUMN);

        // 从 Cursor 中获取摘要，并移除特定标签
        mSnippet = cursor.getString(SNIPPET_COLUMN);
        mSnippet = mSnippet.replace(NoteEditActivity.TAG_CHECKED, "")
                .replace(NoteEditActivity.TAG_UNCHECKED, "");

        // 从 Cursor 中获取类型
        mType = cursor.getInt(TYPE_COLUMN);

        // 从 Cursor 中获取小部件 ID
        mWidgetId = cursor.getInt(WIDGET_ID_COLUMN);

        // 从 Cursor 中获取小部件类型
        mWidgetType = cursor.getInt(WIDGET_TYPE_COLUMN);

        // 初始化电话号码为空字符串
        mPhoneNumber = "";

        // 如果父项 ID 是通话记录文件夹
        if (mParentId == Notes.ID_CALL_RECORD_FOLDER) {
            // 获取通话记录中的电话号码
            mPhoneNumber = DataUtils.getCallNumberByNoteId(context.getContentResolver(), mId);

            // 如果电话号码不为空
            if (!TextUtils.isEmpty(mPhoneNumber)) {
                // 获取联系人名称
                mName = Contact.getContact(context, mPhoneNumber);

                // 如果联系人名称为空，则使用电话号码
                if (mName == null) {
                    mName = mPhoneNumber;
                }
            }
        }
        // 如果联系人名称为空，则初始化为空字符串
        if (mName == null) {
            mName = "";
        }
        // 检查当前位置信息
        checkPostion(cursor);
    }

    private void checkPostion(Cursor cursor) {
        // 检查当前项是否是最后一项
        mIsLastItem = cursor.isLast() ? true : false;

        // 检查当前项是否是第一项
        mIsFirstItem = cursor.isFirst() ? true : false;

        // 检查当前项是否是唯一的项
        mIsOnlyOneItem = (cursor.getCount() == 1);

        // 初始化是否是多个笔记跟随文件夹为 false
        mIsMultiNotesFollowingFolder = false;

        // 初始化是否是一个笔记跟随文件夹为 false
        mIsOneNoteFollowingFolder = false;

        // 如果当前项是笔记类型且不是第一项
        if (mType == Notes.TYPE_NOTE && !mIsFirstItem) {
            // 记录当前游标的位置
            int position = cursor.getPosition();

            // 将游标移动到前一个位置
            if (cursor.moveToPrevious()) {
                // 检查前一个项是否是文件夹或系统项
                if (cursor.getInt(TYPE_COLUMN) == Notes.TYPE_FOLDER
                        || cursor.getInt(TYPE_COLUMN) == Notes.TYPE_SYSTEM) {
                    // 如果总项数大于当前位置加 1，则表示有多个笔记跟随文件夹
                    if (cursor.getCount() > (position + 1)) {
                        mIsMultiNotesFollowingFolder = true;
                    } else {
                        // 否则表示只有一个笔记跟随文件夹
                        mIsOneNoteFollowingFolder = true;
                    }
                }

                // 将游标移回到原来的位置
                if (!cursor.moveToNext()) {
                    throw new IllegalStateException("cursor move to previous but can't move back");
                }
            }
        }
    }


    public boolean isOneFollowingFolder() {
        return mIsOneNoteFollowingFolder;
    }

    public boolean isMultiFollowingFolder() {
        return mIsMultiNotesFollowingFolder;
    }

    public boolean isLast() {
        return mIsLastItem;
    }

    public String getCallName() {
        return mName;
    }

    public boolean isFirst() {
        return mIsFirstItem;
    }

    public boolean isSingle() {
        return mIsOnlyOneItem;
    }

    public long getId() {
        return mId;
    }

    public long getAlertDate() {
        return mAlertDate;
    }

    public long getCreatedDate() {
        return mCreatedDate;
    }

    public boolean hasAttachment() {
        return mHasAttachment;
    }

    public long getModifiedDate() {
        return mModifiedDate;
    }

    public int getBgColorId() {
        return mBgColorId;
    }

    public long getParentId() {
        return mParentId;
    }

    public int getNotesCount() {
        return mNotesCount;
    }

    public long getFolderId () {
        return mParentId;
    }

    public int getType() {
        return mType;
    }

    public int getWidgetType() {
        return mWidgetType;
    }

    public int getWidgetId() {
        return mWidgetId;
    }

    public String getSnippet() {
        return mSnippet;
    }

    public boolean hasAlert() {
        return (mAlertDate > 0);
    }

    public boolean isCallRecord() {
        return (mParentId == Notes.ID_CALL_RECORD_FOLDER && !TextUtils.isEmpty(mPhoneNumber));
    }

    public static int getNoteType(Cursor cursor) {
        return cursor.getInt(TYPE_COLUMN);
    }
}
