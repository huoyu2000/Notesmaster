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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;

import net.micode.notes.R;

/**
 * 下拉菜单类，用于创建一个带有下拉菜单的按钮。
 */
public class DropdownMenu {
    // 按钮对象
    private Button mButton;
    // 弹出菜单对象
    private PopupMenu mPopupMenu;
    // 菜单对象
    private Menu mMenu;

    /**
     * 构造函数，初始化下拉菜单。
     *
     * @param context 上下文环境
     * @param button 按钮对象
     * @param menuId 菜单资源ID
     */
    public DropdownMenu(Context context, Button button, int menuId) {
        mButton = button;
        // 设置按钮背景
        mButton.setBackgroundResource(R.drawable.dropdown_icon);
        // 创建弹出菜单
        mPopupMenu = new PopupMenu(context, mButton);
        // 获取菜单对象
        mMenu = mPopupMenu.getMenu();
        // 加载菜单资源
        mPopupMenu.getMenuInflater().inflate(menuId, mMenu);
        // 设置按钮点击监听器
        mButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                // 显示弹出菜单
                mPopupMenu.show();
            }
        });
    }

    /**
     * 设置下拉菜单项点击监听器。
     *
     * @param listener 菜单项点击监听器
     */
    public void setOnDropdownMenuItemClickListener(OnMenuItemClickListener listener) {
        if (mPopupMenu != null) {
            // 设置菜单项点击监听器
            mPopupMenu.setOnMenuItemClickListener(listener);
        }
    }
    /**
     * 查找指定ID的菜单项。
     *
     * @param id 菜单项ID
     * @return 找到的菜单项
     */
    public MenuItem findItem(int id) {
        return mMenu.findItem(id);
    }

    /**
     * 设置按钮标题。
     *
     * @param title 按钮标题
     */
    public void setTitle(CharSequence title) {
        mButton.setText(title);
    }
}