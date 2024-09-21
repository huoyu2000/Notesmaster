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

package net.micode.notes.data;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.HashMap;

public class Contact {
    private static HashMap<String, String> sContactCache;  // 静态哈希图，用于缓存联系人名称和电话号码
    private static final String TAG = "Contact";  // 日志标签

    // SQL查询条件，用于匹配电话号码
    private static final String CALLER_ID_SELECTION = "PHONE_NUMBERS_EQUAL(" + Phone.NUMBER
            + ",?) AND " + Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'"
            + " AND " + Data.RAW_CONTACT_ID + " IN "
            + "(SELECT raw_contact_id "
            + " FROM phone_lookup"
            + " WHERE min_match = '+')";

    // 根据电话号码获取联系人名称的静态方法
    public static String getContact(Context context, String phoneNumber) {
        if (sContactCache == null) {
            sContactCache = new HashMap<String, String>();  // 初始化缓存
        }

        if (sContactCache.containsKey(phoneNumber)) {  // 如果缓存中已有此电话号码
            return sContactCache.get(phoneNumber);  // 直接返回联系人名称
        }

        // 替换查询条件中的通配符并执行数据库查询
        String selection = CALLER_ID_SELECTION.replace("+",
                PhoneNumberUtils.toCallerIDMinMatch(phoneNumber));
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,   //ContentProvider的uri
                new String[] { Phone.DISPLAY_NAME },
                selection,
                new String[] { phoneNumber },
                null);

        if (cursor != null && cursor.moveToFirst()) {  // 查询结果非空并可读取第一条数据
            try {
                String name = cursor.getString(0);  // 获取联系人名称
                sContactCache.put(phoneNumber, name);  // 将电话号码和联系人名称放入缓存
                return name;  // 返回联系人名称
            } catch (IndexOutOfBoundsException e) {  // 捕获并记录异常
                Log.e(TAG, "Cursor get string error " + e.toString());
                return null;
            } finally {
                cursor.close();  // 关闭游标
            }
        } else {  // 未匹配到任何联系人
            Log.d(TAG, "No contact matched with number:" + phoneNumber);
            return null;
        }
    }
}