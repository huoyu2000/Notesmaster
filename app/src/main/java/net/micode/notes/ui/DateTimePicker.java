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

import java.text.DateFormatSymbols;
import java.util.Calendar;

import net.micode.notes.R;


import android.content.Context;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.NumberPicker;

public class DateTimePicker extends FrameLayout {
    // 默认启用状态
    private static final boolean DEFAULT_ENABLE_STATE = true;
    // 半天的小时数
    private static final int HOURS_IN_HALF_DAY = 12;
    // 一天的小时数
    private static final int HOURS_IN_ALL_DAY = 24;
    // 一周的天数
    private static final int DAYS_IN_ALL_WEEK = 7;
    // 日期选择器的最小值
    private static final int DATE_SPINNER_MIN_VAL = 0;
    // 日期选择器的最大值
    private static final int DATE_SPINNER_MAX_VAL = DAYS_IN_ALL_WEEK - 1;
    // 24小时制下小时选择器的最小值
    private static final int HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW = 0;
    // 24小时制下小时选择器的最大值
    private static final int HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW = 23;
    // 12小时制下小时选择器的最小值
    private static final int HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW = 1;
    // 12小时制下小时选择器的最大值
    private static final int HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW = 12;
    // 分钟选择器的最小值
    private static final int MINUT_SPINNER_MIN_VAL = 0;
    // 分钟选择器的最大值
    private static final int MINUT_SPINNER_MAX_VAL = 59;
    // AM/PM选择器的最小值
    private static final int AMPM_SPINNER_MIN_VAL = 0;
    // AM/PM选择器的最大值
    private static final int AMPM_SPINNER_MAX_VAL = 1;
    // 日期选择器
    private final NumberPicker mDateSpinner;
    // 小时选择器
    private final NumberPicker mHourSpinner;
    // 分钟选择器
    private final NumberPicker mMinuteSpinner;
    // AM/PM选择器
    private final NumberPicker mAmPmSpinner;
    // 当前日期时间
    private Calendar mDate;
    // 日期显示值数组
    private String[] mDateDisplayValues = new String[DAYS_IN_ALL_WEEK];
    // 当前是否为上午
    private boolean mIsAm;
    // 是否为24小时制显示
    private boolean mIs24HourView;
    // 控件是否启用
    private boolean mIsEnabled = DEFAULT_ENABLE_STATE;
    // 初始化标志
    private boolean mInitialising;
    // 时间更改监听器
    private OnDateTimeChangedListener mOnDateTimeChangedListener;


    private NumberPicker.OnValueChangeListener mOnDateChangedListener = new NumberPicker.OnValueChangeListener() {
        /**
         * 日期选择器的值改变监听器。
         *
         * @param picker  选择器对象
         * @param oldVal  旧值
         * @param newVal  新值
         */
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            // 更新日期
            mDate.add(Calendar.DAY_OF_YEAR, newVal - oldVal);

            // 更新日期控件
            updateDateControl();

            // 触发时间更改事件
            onDateTimeChanged();
        }
    };

    private NumberPicker.OnValueChangeListener mOnHourChangedListener = new NumberPicker.OnValueChangeListener() {
        /**
         * 小时选择器的值改变监听器。
         *
         * @param picker  选择器对象
         * @param oldVal  旧值
         * @param newVal  新值
         */
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            boolean isDateChanged = false;
            Calendar cal = Calendar.getInstance();

            if (!mIs24HourView) {
                // 12小时制逻辑
                if (!mIsAm && oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY) {
                    // 从下午11点切换到午夜0点
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    isDateChanged = true;
                } else if (mIsAm && oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) {
                    // 从凌晨12点切换到晚上11点
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    isDateChanged = true;
                }
                if (oldVal == HOURS_IN_HALF_DAY - 1 && newVal == HOURS_IN_HALF_DAY ||
                        oldVal == HOURS_IN_HALF_DAY && newVal == HOURS_IN_HALF_DAY - 1) {
                    // 切换 AM/PM
                    mIsAm = !mIsAm;
                    updateAmPmControl();
                }
            } else {
                // 24小时制逻辑
                if (oldVal == HOURS_IN_ALL_DAY - 1 && newVal == 0) {
                    // 从23点切换到0点
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                    isDateChanged = true;
                } else if (oldVal == 0 && newVal == HOURS_IN_ALL_DAY - 1) {
                    // 从0点切换到23点
                    cal.setTimeInMillis(mDate.getTimeInMillis());
                    cal.add(Calendar.DAY_OF_YEAR, -1);
                    isDateChanged = true;
                }
            }
            // 计算新的小时值
            int newHour = mHourSpinner.getValue() % HOURS_IN_HALF_DAY + (mIsAm ? 0 : HOURS_IN_HALF_DAY);
            // 设置新的小时值
            mDate.set(Calendar.HOUR_OF_DAY, newHour);
            // 触发时间更改事件
            onDateTimeChanged();
            // 如果日期发生了变化，则更新当前年月日
            if (isDateChanged) {
                setCurrentYear(cal.get(Calendar.YEAR));
                setCurrentMonth(cal.get(Calendar.MONTH));
                setCurrentDay(cal.get(Calendar.DAY_OF_MONTH));
            }
        }
    };


    private NumberPicker.OnValueChangeListener mOnMinuteChangedListener = new NumberPicker.OnValueChangeListener() {
        /**
         * 分钟选择器的值改变监听器。
         *
         * @param picker  选择器对象
         * @param oldVal  旧值
         * @param newVal  新值
         */
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            int minValue = mMinuteSpinner.getMinValue();
            int maxValue = mMinuteSpinner.getMaxValue();
            int offset = 0;

            // 检查是否从最大值切换到最小值
            if (oldVal == maxValue && newVal == minValue) {
                offset += 1;  // 增加一小时
            } else if (oldVal == minValue && newVal == maxValue) {
                offset -= 1;  // 减少一小时
            }

            // 如果有偏移，则更新时间
            if (offset != 0) {
                mDate.add(Calendar.HOUR_OF_DAY, offset);  // 更新小时数
                mHourSpinner.setValue(getCurrentHour());  // 设置新的小时值
                updateDateControl();  // 更新日期控件

                // 获取新的小时值
                int newHour = getCurrentHourOfDay();

                // 根据新的小时值更新 AM/PM 状态
                if (newHour >= HOURS_IN_HALF_DAY) {
                    mIsAm = false;
                    updateAmPmControl();
                } else {
                    mIsAm = true;
                    updateAmPmControl();
                }
            }

            // 设置新的分钟值
            mDate.set(Calendar.MINUTE, newVal);

            // 触发时间更改事件
            onDateTimeChanged();
        }
    };

    private NumberPicker.OnValueChangeListener mOnAmPmChangedListener = new NumberPicker.OnValueChangeListener() {
        /**
         * AM/PM 选择器的值改变监听器。
         *
         * @param picker  选择器对象
         * @param oldVal  旧值
         * @param newVal  新值
         */
        @Override
        public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
            mIsAm = !mIsAm;  // 切换 AM/PM 状态

            // 根据新的 AM/PM 状态调整小时数
            if (mIsAm) {
                mDate.add(Calendar.HOUR_OF_DAY, -HOURS_IN_HALF_DAY);  // 减去半天
            } else {
                mDate.add(Calendar.HOUR_OF_DAY, HOURS_IN_HALF_DAY);  // 加上半天
            }

            // 更新 AM/PM 控件
            updateAmPmControl();

            // 触发时间更改事件
            onDateTimeChanged();
        }
    };


    public interface OnDateTimeChangedListener {
        /**
         * 时间日期更改监听器接口。
         *
         * @param view          DateTimePicker 对象
         * @param year          年份
         * @param month         月份（0-11）
         * @param dayOfMonth    月份中的天数
         * @param hourOfDay     小时（24小时制）
         * @param minute        分钟
         */
        void onDateTimeChanged(DateTimePicker view, int year, int month,
                               int dayOfMonth, int hourOfDay, int minute);
    }

    public DateTimePicker(Context context) {
        this(context, System.currentTimeMillis());
    }

    public DateTimePicker(Context context, long date) {
        this(context, date, DateFormat.is24HourFormat(context));
    }

    public DateTimePicker(Context context, long date, boolean is24HourView) {
        super(context);
        mDate = Calendar.getInstance();
        mInitialising = true;
        mIsAm = getCurrentHourOfDay() >= HOURS_IN_HALF_DAY;
        inflate(context, R.layout.datetime_picker, this);

        // 初始化日期选择器
        mDateSpinner = (NumberPicker) findViewById(R.id.date);
        mDateSpinner.setMinValue(DATE_SPINNER_MIN_VAL);
        mDateSpinner.setMaxValue(DATE_SPINNER_MAX_VAL);
        mDateSpinner.setOnValueChangedListener(mOnDateChangedListener);

        // 初始化小时选择器
        mHourSpinner = (NumberPicker) findViewById(R.id.hour);
        mHourSpinner.setOnValueChangedListener(mOnHourChangedListener);

        // 初始化分钟选择器
        mMinuteSpinner = (NumberPicker) findViewById(R.id.minute);
        mMinuteSpinner.setMinValue(MINUT_SPINNER_MIN_VAL);
        mMinuteSpinner.setMaxValue(MINUT_SPINNER_MAX_VAL);
        mMinuteSpinner.setOnLongPressUpdateInterval(100);
        mMinuteSpinner.setOnValueChangedListener(mOnMinuteChangedListener);

        // 初始化 AM/PM 选择器
        String[] stringsForAmPm = new DateFormatSymbols().getAmPmStrings();
        mAmPmSpinner = (NumberPicker) findViewById(R.id.amPm);
        mAmPmSpinner.setMinValue(AMPM_SPINNER_MIN_VAL);
        mAmPmSpinner.setMaxValue(AMPM_SPINNER_MAX_VAL);
        mAmPmSpinner.setDisplayedValues(stringsForAmPm);
        mAmPmSpinner.setOnValueChangedListener(mOnAmPmChangedListener);

        // 更新控件到初始状态
        updateDateControl();
        updateHourControl();
        updateAmPmControl();

        // 设置 24 小时制或 12 小时制
        set24HourView(is24HourView);

        // 设置当前时间
        setCurrentDate(date);

        // 设置启用状态
        setEnabled(isEnabled());

        // 设置内容描述
        mInitialising = false;
    }


    @Override
    public void setEnabled(boolean enabled) {
        /**
         * 设置组件是否启用。
         *
         * @param enabled 是否启用
         */
        if (mIsEnabled == enabled) {
            return;
        }
        super.setEnabled(enabled);
        mDateSpinner.setEnabled(enabled);
        mMinuteSpinner.setEnabled(enabled);
        mHourSpinner.setEnabled(enabled);
        mAmPmSpinner.setEnabled(enabled);
        mIsEnabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        /**
         * 获取组件是否启用。
         *
         * @return 是否启用
         */
        return mIsEnabled;
    }

    /**
     * 获取当前日期的时间戳（毫秒）。
     *
     * @return 当前日期的时间戳（毫秒）
     */
    public long getCurrentDateInTimeMillis() {
        return mDate.getTimeInMillis();
    }

    /**
     * 设置当前日期。
     *
     * @param date 当前日期的时间戳（毫秒）
     */
    public void setCurrentDate(long date) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date);
        setCurrentDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH),
                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
    }

    /**
     * 设置当前日期。
     *
     * @param year      当前年份
     * @param month     当前月份（0-11）
     * @param dayOfMonth 当前月份中的天数
     * @param hourOfDay 当前小时（24小时制）
     * @param minute    当前分钟
     */
    public void setCurrentDate(int year, int month,
                               int dayOfMonth, int hourOfDay, int minute) {
        setCurrentYear(year);
        setCurrentMonth(month);
        setCurrentDay(dayOfMonth);
        setCurrentHour(hourOfDay);
        setCurrentMinute(minute);
    }

    /**
     * 获取当前年份。
     *
     * @return 当前年份
     */
    public int getCurrentYear() {
        return mDate.get(Calendar.YEAR);
    }

    /**
     * 设置当前年份。
     *
     * @param year 当前年份
     */
    public void setCurrentYear(int year) {
        if (!mInitialising && year == getCurrentYear()) {
            return;
        }
        mDate.set(Calendar.YEAR, year);
        updateDateControl();
        onDateTimeChanged();
    }

    /**
     * 获取当前月份。
     *
     * @return 当前月份（0-11）
     */
    public int getCurrentMonth() {
        return mDate.get(Calendar.MONTH);
    }

    /**
     * 设置当前月份。
     *
     * @param month 当前月份（0-11）
     */
    public void setCurrentMonth(int month) {
        if (!mInitialising && month == getCurrentMonth()) {
            return;
        }
        mDate.set(Calendar.MONTH, month);
        updateDateControl();
        onDateTimeChanged();
    }

    /**
     * 获取当前月份中的天数。
     *
     * @return 当前月份中的天数
     */
    public int getCurrentDay() {
        return mDate.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 设置当前月份中的天数。
     *
     * @param dayOfMonth 当前月份中的天数
     */
    public void setCurrentDay(int dayOfMonth) {
        if (!mInitialising && dayOfMonth == getCurrentDay()) {
            return;
        }
        mDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        updateDateControl();
        onDateTimeChanged();
    }

    /**
     * 获取当前小时（24小时制），范围在 0~23。
     *
     * @return 当前小时（24小时制）
     */
    public int getCurrentHourOfDay() {
        return mDate.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取当前小时，根据是否为24小时制进行调整。
     *
     * @return 当前小时
     */
    private int getCurrentHour() {
        if (mIs24HourView) {
            return getCurrentHourOfDay();
        } else {
            int hour = getCurrentHourOfDay();
            if (hour > HOURS_IN_HALF_DAY) {
                return hour - HOURS_IN_HALF_DAY;
            } else {
                return hour == 0 ? HOURS_IN_HALF_DAY : hour;
            }
        }
    }

    /**
     * 设置当前小时（24小时制），范围在 0~23。
     *
     * @param hourOfDay 当前小时（24小时制）
     */
    public void setCurrentHour(int hourOfDay) {
        if (!mInitialising && hourOfDay == getCurrentHourOfDay()) {
            return;
        }
        mDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
        if (!mIs24HourView) {
            if (hourOfDay >= HOURS_IN_HALF_DAY) {
                mIsAm = false;
                if (hourOfDay > HOURS_IN_HALF_DAY) {
                    hourOfDay -= HOURS_IN_HALF_DAY;
                }
            } else {
                mIsAm = true;
                if (hourOfDay == 0) {
                    hourOfDay = HOURS_IN_HALF_DAY;
                }
            }
            updateAmPmControl();
        }
        mHourSpinner.setValue(hourOfDay);
        onDateTimeChanged();
    }

    /**
     * 获取当前分钟。
     *
     * @return 当前分钟
     */
    public int getCurrentMinute() {
        return mDate.get(Calendar.MINUTE);
    }

    /**
     * 设置当前分钟。
     *
     * @param minute 当前分钟
     */
    public void setCurrentMinute(int minute) {
        if (!mInitialising && minute == getCurrentMinute()) {
            return;
        }
        mMinuteSpinner.setValue(minute);
        mDate.set(Calendar.MINUTE, minute);
        onDateTimeChanged();
    }

    /**
     * 判断是否为24小时制视图。
     *
     * @return 如果是24小时制视图返回true，否则返回false
     */
    public boolean is24HourView() {
        return mIs24HourView;
    }

    /**
     * 设置是否为24小时制或AM/PM模式。
     *
     * @param is24HourView 如果为24小时制则为true，否则为false
     */
    public void set24HourView(boolean is24HourView) {
        if (mIs24HourView == is24HourView) {
            return;
        }
        mIs24HourView = is24HourView;
        mAmPmSpinner.setVisibility(is24HourView ? View.GONE : View.VISIBLE);
        int hour = getCurrentHourOfDay();
        updateHourControl();
        setCurrentHour(hour);
        updateAmPmControl();
    }

    /**
     * 更新日期控件显示值。
     */
    private void updateDateControl() {
        // 创建一个日历实例，并设置为当前时间
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mDate.getTimeInMillis());
        // 计算一周中的起始日期
        cal.add(Calendar.DAY_OF_YEAR, -DAYS_IN_ALL_WEEK / 2 - 1);
        // 清空日期选择器的显示值
        mDateSpinner.setDisplayedValues(null);
        // 遍历一周中的每一天，并设置显示值
        for (int i = 0; i < DAYS_IN_ALL_WEEK; ++i) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            mDateDisplayValues[i] = (String) DateFormat.format("MM.dd EEEE", cal);
        }
        // 设置日期选择器的显示值
        mDateSpinner.setDisplayedValues(mDateDisplayValues);
        // 设置默认选中的日期为中间的一天
        mDateSpinner.setValue(DAYS_IN_ALL_WEEK / 2);
        // 使日期选择器无效，触发重新绘制
        mDateSpinner.invalidate();
    }

    /**
     * 更新 AM/PM 控件的显示状态。
     */
    private void updateAmPmControl() {
        if (mIs24HourView) {
            // 如果是24小时制，则隐藏 AM/PM 选择器
            mAmPmSpinner.setVisibility(View.GONE);
        } else {
            // 如果是12小时制，则设置 AM/PM 状态并显示选择器
            int index = mIsAm ? Calendar.AM : Calendar.PM;
            mAmPmSpinner.setValue(index);
            mAmPmSpinner.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 更新小时控件的范围。
     */
    private void updateHourControl() {
        if (mIs24HourView) {
            // 如果是24小时制，则设置小时范围为 0-23
            mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_24_HOUR_VIEW);
            mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_24_HOUR_VIEW);
        } else {
            // 如果是12小时制，则设置小时范围为 1-12
            mHourSpinner.setMinValue(HOUR_SPINNER_MIN_VAL_12_HOUR_VIEW);
            mHourSpinner.setMaxValue(HOUR_SPINNER_MAX_VAL_12_HOUR_VIEW);
        }
    }

    /**
     * 设置日期时间更改回调。
     *
     * @param callback 回调接口，如果为null则不做任何操作
     */
    public void setOnDateTimeChangedListener(OnDateTimeChangedListener callback) {
        mOnDateTimeChangedListener = callback;
    }

    /**
     * 触发日期时间更改事件。
     */
    private void onDateTimeChanged() {
        if (mOnDateTimeChangedListener != null) {
            // 调用回调接口，通知日期时间已更改
            mOnDateTimeChangedListener.onDateTimeChanged(this,
                    getCurrentYear(),
                    getCurrentMonth(),
                    getCurrentDay(),
                    getCurrentHourOfDay(),
                    getCurrentMinute());
        }
    }

}
