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
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

import net.micode.notes.data.Notes;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;


public class NotesListAdapter extends CursorAdapter {
    private static final String TAG = "NotesListAdapter";
    private Context mContext;
    private HashMap<Integer, Boolean> mSelectedIndex;
    private int mNotesCount;
    private boolean mChoiceMode;

    // 内部类，用于存储小部件属性
    public static class AppWidgetAttribute {
        public int widgetId;
        public int widgetType;
    }

    // 构造函数
    public NotesListAdapter(Context context) {
        super(context, null);
        mSelectedIndex = new HashMap<Integer, Boolean>();
        mContext = context;
        mNotesCount = 0;
    }

    // 创建新视图
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return new NotesListItem(context);
    }

    // 绑定视图
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (view instanceof NotesListItem) {
            NoteItemData itemData = new NoteItemData(context, cursor);
            ((NotesListItem) view).bind(context, itemData, mChoiceMode,
                    isSelectedItem(cursor.getPosition()));
        }
    }

    // 设置指定位置的项目为选中状态
    public void setCheckedItem(final int position, final boolean checked) {
        mSelectedIndex.put(position, checked);
        notifyDataSetChanged();
    }

    // 判断是否处于选择模式
    public boolean isInChoiceMode() {
        return mChoiceMode;
    }

    // 设置选择模式
    public void setChoiceMode(boolean mode) {
        mSelectedIndex.clear();
        mChoiceMode = mode;
    }

    // 全选或取消全选
    public void selectAll(boolean checked) {
        Cursor cursor = getCursor();
        for (int i = 0; i < getCount(); i++) {
            if (cursor.moveToPosition(i)) {
                if (NoteItemData.getNoteType(cursor) == Notes.TYPE_NOTE) {
                    setCheckedItem(i, checked);
                }
            }
        }
    }

    // 获取所有选中项目的 ID
    public HashSet<Long> getSelectedItemIds() {
        HashSet<Long> itemSet = new HashSet<Long>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                Long id = getItemId(position);
                if (id == Notes.ID_ROOT_FOLDER) {
                    Log.d(TAG, "Wrong item id, should not happen");
                } else {
                    itemSet.add(id);
                }
            }
        }
        return itemSet;
    }

    // 获取所有选中小部件
    public HashSet<AppWidgetAttribute> getSelectedWidget() {
        HashSet<AppWidgetAttribute> itemSet = new HashSet<AppWidgetAttribute>();
        for (Integer position : mSelectedIndex.keySet()) {
            if (mSelectedIndex.get(position) == true) {
                Cursor c = (Cursor) getItem(position);
                if (c != null) {
                    AppWidgetAttribute widget = new AppWidgetAttribute();
                    NoteItemData item = new NoteItemData(mContext, c);
                    widget.widgetId = item.getWidgetId();
                    widget.widgetType = item.getWidgetType();
                    itemSet.add(widget);
                    /**
                     * 不要在这里关闭游标，只有适配器可以关闭它
                     */
                } else {
                    Log.e(TAG, "Invalid cursor");
                    return null;
                }
            }
        }
        return itemSet;
    }

    // 获取选中项目的数量
    public int getSelectedCount() {
        Collection<Boolean> values = mSelectedIndex.values();
        if (values == null) {
            return 0;
        }
        int count = 0;
        for (Boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    // 判断是否全部选中
    public boolean isAllSelected() {
        int checkedCount = getSelectedCount();
        return (checkedCount != 0 && checkedCount == mNotesCount);
    }

    // 判断指定位置的项目是否被选中
    public boolean isSelectedItem(final int position) {
        Boolean selected = mSelectedIndex.get(position);
        return selected != null && selected;
    }

    // 当内容发生变化时调用
    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        calcNotesCount();
    }

    // 更改游标
    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
        calcNotesCount();
    }

    // 计算笔记数量
    private void calcNotesCount() {
        mNotesCount = 0;
        for (int i = 0; i < getCount(); i++) {
            Cursor c = (Cursor) getItem(i);
            if (c != null) {
                if (NoteItemData.getNoteType(c) == Notes.TYPE_NOTE) {
                    mNotesCount++;
                }
            } else {
                Log.e(TAG, "Invalid cursor");
                return;
            }
        }
    }
}
