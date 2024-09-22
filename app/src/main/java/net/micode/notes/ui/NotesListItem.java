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
import android.text.format.DateUtils;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser.NoteItemBgResources;

/**
 * Notes列表项的视图组件。
 */
public class NotesListItem extends LinearLayout {
    private ImageView mAlert; // 警告图标
    private TextView mTitle; // 标题文本
    private TextView mTime; // 时间文本
    private TextView mCallName; // 呼叫名称文本
    private NoteItemData mItemData; // 当前项的数据
    private CheckBox mCheckBox; // 复选框

    /**
     * 构造函数。
     *
     * @param context 上下文
     */
    public NotesListItem(Context context) {
        super(context);
        inflate(context, R.layout.note_item, this);
        mAlert = (ImageView) findViewById(R.id.iv_alert_icon);
        mTitle = (TextView) findViewById(R.id.tv_title);
        mTime = (TextView) findViewById(R.id.tv_time);
        mCallName = (TextView) findViewById(R.id.tv_name);
        mCheckBox = (CheckBox) findViewById(android.R.id.checkbox);
    }

    /**
     * 绑定数据到视图。
     *
     * @param context 上下文
     * @param data 数据对象
     * @param choiceMode 是否处于选择模式
     * @param checked 是否选中
     */
    public void bind(Context context, NoteItemData data, boolean choiceMode, boolean checked) {
        // 如果处于选择模式且数据类型为笔记，则显示复选框并设置选中状态
        if (choiceMode && data.getType() == Notes.TYPE_NOTE) {
            mCheckBox.setVisibility(View.VISIBLE);
            mCheckBox.setChecked(checked);
        } else {
            // 否则隐藏复选框
            mCheckBox.setVisibility(View.GONE);
        }

        // 设置当前项的数据
        mItemData = data;

        // 根据数据类型设置不同的视图属性
        if (data.getId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 如果是通话记录文件夹
            mCallName.setVisibility(View.GONE); // 隐藏呼叫名称
            mAlert.setVisibility(View.VISIBLE); // 显示警告图标
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem); // 设置标题样式
            mTitle.setText(context.getString(R.string.call_record_folder_name)
                    + context.getString(R.string.format_folder_files_count, data.getNotesCount())); // 设置标题文本
            mAlert.setImageResource(R.drawable.call_record); // 设置警告图标资源
        } else if (data.getParentId() == Notes.ID_CALL_RECORD_FOLDER) {
            // 如果是通话记录文件夹中的条目
            mCallName.setVisibility(View.VISIBLE); // 显示呼叫名称
            mCallName.setText(data.getCallName()); // 设置呼叫名称文本
            mTitle.setTextAppearance(context, R.style.TextAppearanceSecondaryItem); // 设置标题样式
            mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet())); // 设置标题文本
            if (data.hasAlert()) {
                // 如果有警告
                mAlert.setImageResource(R.drawable.clock); // 设置警告图标资源
                mAlert.setVisibility(View.VISIBLE); // 显示警告图标
            } else {
                // 否则隐藏警告图标
                mAlert.setVisibility(View.GONE);
            }
        } else {
            // 其他类型的条目
            mCallName.setVisibility(View.GONE); // 隐藏呼叫名称
            mTitle.setTextAppearance(context, R.style.TextAppearancePrimaryItem); // 设置标题样式

            if (data.getType() == Notes.TYPE_FOLDER) {
                // 如果是文件夹
                mTitle.setText(data.getSnippet()
                        + context.getString(R.string.format_folder_files_count,
                        data.getNotesCount())); // 设置标题文本
                mAlert.setVisibility(View.GONE); // 隐藏警告图标
            } else {
                // 如果是笔记
                mTitle.setText(DataUtils.getFormattedSnippet(data.getSnippet())); // 设置标题文本
                if (data.hasAlert()) {
                    // 如果有警告
                    mAlert.setImageResource(R.drawable.clock); // 设置警告图标资源
                    mAlert.setVisibility(View.VISIBLE); // 显示警告图标
                } else {
                    // 否则隐藏警告图标
                    mAlert.setVisibility(View.GONE);
                }
            }
        }

        // 设置时间文本
        mTime.setText(DateUtils.getRelativeTimeSpanString(data.getModifiedDate()));

        // 设置背景
        setBackground(data);
    }


    /**
     * 设置背景资源。
     *
     * @param data 数据对象
     */
    private void setBackground(NoteItemData data) {
        // 获取背景颜色ID
        int id = data.getBgColorId();

        // 根据数据类型设置不同的背景资源
        if (data.getType() == Notes.TYPE_NOTE) {
            // 如果是笔记类型
            if (data.isSingle() || data.isOneFollowingFolder()) {
                // 如果是单个笔记或紧跟在文件夹后面的笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgSingleRes(id));
            } else if (data.isLast()) {
                // 如果是最后一个笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgLastRes(id));
            } else if (data.isFirst() || data.isMultiFollowingFolder()) {
                // 如果是第一个笔记或多个紧跟在文件夹后面的笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgFirstRes(id));
            } else {
                // 其他情况下的普通笔记
                setBackgroundResource(NoteItemBgResources.getNoteBgNormalRes(id));
            }
        } else {
            // 如果是文件夹类型
            setBackgroundResource(NoteItemBgResources.getFolderBgRes());
        }
    }


    /**
     * 获取当前项的数据。
     *
     * @return 当前项的数据
     */
    public NoteItemData getItemData() {
        return mItemData;
    }
}
