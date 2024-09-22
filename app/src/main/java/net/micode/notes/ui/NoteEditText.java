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
import android.graphics.Rect;
import android.text.Layout;
import android.text.Selection;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.MotionEvent;
import android.widget.EditText;

import net.micode.notes.R;

import java.util.HashMap;
import java.util.Map;

public class NoteEditText extends androidx.appcompat.widget.AppCompatEditText {
    private static final String TAG = "NoteEditText";
    // 日志标签，用于日志记录。

    private int mIndex;
    // 当前编辑文本的索引。

    private int mSelectionStartBeforeDelete;
    // 删除操作前的选择起始位置。

    private static final String SCHEME_TEL = "tel:";
    // 电话链接的 URI 方案。

    private static final String SCHEME_HTTP = "http:";
    // HTTP 链接的 URI 方案。

    private static final String SCHEME_EMAIL = "mailto:";
    // 邮件链接的 URI 方案。

    private static final Map<String, Integer> sSchemaActionResMap = new HashMap<String, Integer>();
    // 存储不同 URI 方案对应的资源 ID 的映射表。

    static {
        sSchemaActionResMap.put(SCHEME_TEL, R.string.note_link_tel);
        // 将电话链接方案映射到对应的资源 ID。

        sSchemaActionResMap.put(SCHEME_HTTP, R.string.note_link_web);
        // 将 HTTP 链接方案映射到对应的资源 ID。

        sSchemaActionResMap.put(SCHEME_EMAIL, R.string.note_link_email);
        // 将邮件链接方案映射到对应的资源 ID。
    }


    /**
     * 由 {@link NoteEditActivity} 调用，用于删除或添加编辑文本。
     */
    public interface OnTextViewChangeListener {
        /**
         * 当 {@link KeyEvent#KEYCODE_DEL} 发生且文本为空时，删除当前编辑文本。
         *
         * @param index 当前编辑文本的索引
         * @param text 当前编辑文本的内容
         */
        void onEditTextDelete(int index, String text);

        /**
         * 当 {@link KeyEvent#KEYCODE_ENTER} 发生时，在当前编辑文本后添加新的编辑文本。
         *
         * @param index 当前编辑文本的索引
         * @param text 当前编辑文本的内容
         */
        void onEditTextEnter(int index, String text);

        /**
         * 当文本发生变化时，隐藏或显示项目选项。
         *
         * @param index 当前编辑文本的索引
         * @param hasText 文本是否为空
         */
        void onTextChange(int index, boolean hasText);
    }

    private OnTextViewChangeListener mOnTextViewChangeListener;

    public NoteEditText(Context context) {
        super(context, null);
        mIndex = 0;
        // 初始化索引为 0
    }

    public void setIndex(int index) {
        mIndex = index;
        // 设置当前编辑文本的索引
    }

    public void setOnTextViewChangeListener(OnTextViewChangeListener listener) {
        mOnTextViewChangeListener = listener;
        // 设置文本视图变化监听器
    }

    public NoteEditText(Context context, AttributeSet attrs) {
        super(context, attrs, android.R.attr.editTextStyle);
        // 使用 AttributeSet 初始化 NoteEditText
    }

    public NoteEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // 使用 AttributeSet 和默认样式初始化 NoteEditText
        // TODO: 完成构造函数的具体实现
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 处理触摸事件的按下动作

                int x = (int) event.getX();
                int y = (int) event.getY();
                // 获取触摸点的坐标

                x -= getTotalPaddingLeft();
                y -= getTotalPaddingTop();
                // 去除控件的内边距

                x += getScrollX();
                y += getScrollY();
                // 考虑滚动偏移量

                Layout layout = getLayout();
                // 获取布局对象

                int line = layout.getLineForVertical(y);
                // 获取触摸点所在的行号

                int off = layout.getOffsetForHorizontal(line, x);
                // 获取触摸点在当前行的偏移量

                Selection.setSelection(getText(), off);
                // 设置光标位置

                break;
        }

        return super.onTouchEvent(event);
        // 交给父类处理其他触摸事件
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_ENTER:
                // 处理回车键事件

                if (mOnTextViewChangeListener != null) {
                    // 如果有监听器，则不拦截事件
                    return false;
                }
                break;

            case KeyEvent.KEYCODE_DEL:
                // 处理删除键事件

                mSelectionStartBeforeDelete = getSelectionStart();
                // 记录删除前的选择起始位置
                break;

            default:
                // 其他按键事件
                break;
        }

        return super.onKeyDown(keyCode, event);
        // 交给父类处理其他按键事件
    }
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                // 处理删除键事件

                if (mOnTextViewChangeListener != null) {
                    // 检查是否有监听器
                    if (0 == mSelectionStartBeforeDelete && mIndex != 0) {
                        // 如果选择起始位置为 0 并且索引不为 0
                        mOnTextViewChangeListener.onEditTextDelete(mIndex, getText().toString());
                        // 调用监听器的删除方法
                        return true;
                    }
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not set");
                    // 如果没有设置监听器，记录日志
                }
                break;

            case KeyEvent.KEYCODE_ENTER:
                // 处理回车键事件

                if (mOnTextViewChangeListener != null) {
                    // 检查是否有监听器
                    int selectionStart = getSelectionStart();
                    // 获取当前选择的起始位置

                    String text = getText().subSequence(selectionStart, length()).toString();
                    // 获取选择后的文本

                    setText(getText().subSequence(0, selectionStart));
                    // 设置当前文本为选择前的部分

                    mOnTextViewChangeListener.onEditTextEnter(mIndex + 1, text);
                    // 调用监听器的回车方法
                } else {
                    Log.d(TAG, "OnTextViewChangeListener was not set");
                    // 如果没有设置监听器，记录日志
                }
                break;

            default:
                // 其他按键事件
                break;
        }

        return super.onKeyUp(keyCode, event);
        // 交给父类处理其他按键事件
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        if (mOnTextViewChangeListener != null) {
            // 检查是否有监听器
            if (!focused && TextUtils.isEmpty(getText())) {
                // 如果失去焦点并且文本为空
                mOnTextViewChangeListener.onTextChange(mIndex, false);
                // 调用监听器的文本变化方法，传入 false 表示文本为空
            } else {
                mOnTextViewChangeListener.onTextChange(mIndex, true);
                // 调用监听器的文本变化方法，传入 true 表示文本不为空
            }
        }

        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        // 交给父类处理焦点变化事件
    }

    @Override
    protected void onCreateContextMenu(ContextMenu menu) {
        if (getText() instanceof Spanned) {
            int selStart = getSelectionStart();
            int selEnd = getSelectionEnd();

            int min = Math.min(selStart, selEnd);
            int max = Math.max(selStart, selEnd);

            final URLSpan[] urls = ((Spanned) getText()).getSpans(min, max, URLSpan.class);
            if (urls.length == 1) {
                int defaultResId = 0;
                for(String schema: sSchemaActionResMap.keySet()) {
                    if(urls[0].getURL().indexOf(schema) >= 0) {
                        defaultResId = sSchemaActionResMap.get(schema);
                        break;
                    }
                }

                if (defaultResId == 0) {
                    defaultResId = R.string.note_link_other;
                }

                menu.add(0, 0, 0, defaultResId).setOnMenuItemClickListener(
                        new OnMenuItemClickListener() {
                            public boolean onMenuItemClick(MenuItem item) {
                                // goto a new intent
                                urls[0].onClick(NoteEditText.this);
                                return true;
                            }
                        });
            }
        }
        super.onCreateContextMenu(menu);
    }
}
