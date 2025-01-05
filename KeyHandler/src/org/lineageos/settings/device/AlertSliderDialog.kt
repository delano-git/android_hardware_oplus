/*
 * Copyright (C) 2019 CypherOS
 * Copyright (C) 2014-2020 Paranoid Android
 * Copyright (C) 2023 The LineageOS Project
 * Copyright (C) 2023 Yet Another AOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.settings.device

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.ColorDrawable
import android.media.AudioManager
import android.view.Gravity
import android.view.Surface
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.lineageos.settings.device.KeyHandler 

import org.lineageos.settings.device.R

/**
 * View with some logging to show that its being run.
 */
class AlertSliderDialog(private var context: Context) : Dialog(context, R.style.alert_slider_theme) {
    private val dialogView by lazy { findViewById<LinearLayout>(R.id.alert_slider_dialog) }
    private val frameView by lazy { findViewById<ViewGroup>(R.id.alert_slider_view) }
    private val iconView by lazy { findViewById<ImageView>(R.id.alert_slider_icon) }
    private val textView by lazy { findViewById<TextView>(R.id.alert_slider_text) }

    private val rotation: Int = context.getDisplay().getRotation()
    private val isLand: Boolean = rotation != Surface.ROTATION_0

    private var length: Int = 0
    private var xPos: Int = 0
    private var yPos: Int = 0

    init {
        // window init
        window!!.requestFeature(Window.FEATURE_NO_TITLE)
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                    or WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )
        window.addPrivateFlags(WindowManager.LayoutParams.PRIVATE_FLAG_TRUSTED_OVERLAY)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        window.setType(WindowManager.LayoutParams.TYPE_VOLUME_OVERLAY)

        window!!.attributes = window.attributes.apply {
            format = PixelFormat.TRANSLUCENT
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            title = TAG
        }

        setCanceledOnTouchOutside(false)
        setContentView(R.layout.alert_slider_dialog)

        // position calculations
        val fraction = context.resources.getFraction(R.fraction.alert_slider_dialog_y, 1, 1)
        val widthPixels = context.resources.displayMetrics.widthPixels
        val heightPixels = context.resources.displayMetrics.heightPixels
        val pads = dialogView.paddingTop * 2 // equal paddings in all 4 directions
        length = if (isLand) context.resources.getDimension(R.dimen.alert_slider_dialog_width).toInt()
                 else context.resources.getDimension(R.dimen.alert_slider_dialog_height).toInt()
        val hv = (length + pads) * 0.5

        xPos = if (isLand) (widthPixels * fraction - hv).toInt()
               else widthPixels / 100
        yPos = if (isLand) 0
               else (heightPixels * fraction - hv).toInt()

        window!!.attributes = window.attributes.apply {
            gravity = when(rotation) {
                Surface.ROTATION_0 -> (Gravity.TOP or Gravity.RIGHT)
                Surface.ROTATION_90 -> (Gravity.TOP or Gravity.LEFT)
                Surface.ROTATION_270 -> (Gravity.BOTTOM or Gravity.RIGHT)
                else -> (Gravity.TOP or Gravity.RIGHT)
            }

            x = xPos
            y = yPos
        }
    }

    @Synchronized
    fun setState(position: Int, ringerMode: Int) {
        frameView.setBackgroundResource(
            when (rotation) {
                Surface.ROTATION_90 -> sBackgroundResMap90.get(position)!!
                Surface.ROTATION_270 -> sBackgroundResMap270.get(position)!!
                else -> sBackgroundResMap.get(position)!! // Surface.ROTATION_0
            }
        )

        sIconResMap.get(ringerMode)?.let {
            iconView.setImageResource(it)
        } ?: {
            iconView.setImageResource(R.drawable.ic_info)
        }

        sTextResMap.get(ringerMode)?.let {
            textView.setText(it)
        } ?: {
            textView.setText(R.string.notification_slider_mode_none)
        }

        window!!.attributes = window.attributes.apply {
            val delta = length * when(position) {
                KeyHandler.POSITION_TOP -> -1
                KeyHandler.POSITION_BOTTOM -> 1
                else -> 0 // KeyHandler.POSITION_MIDDLE
            }

            if (isLand) x = xPos + delta
            else y = yPos + delta
        }
    }

    companion object {
        private const val TAG = "AlertSliderDialog"

        private val sBackgroundResMap = hashMapOf(
            KeyHandler.POSITION_TOP to R.drawable.alert_slider_top,
            KeyHandler.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            KeyHandler.POSITION_BOTTOM to R.drawable.alert_slider_bottom
        )

        private val sBackgroundResMap90 = hashMapOf(
            KeyHandler.POSITION_TOP to R.drawable.alert_slider_top_90,
            KeyHandler.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            KeyHandler.POSITION_BOTTOM to R.drawable.alert_slider_bottom_90
        )

        private val sBackgroundResMap270 = hashMapOf(
            KeyHandler.POSITION_TOP to R.drawable.alert_slider_top_270,
            KeyHandler.POSITION_MIDDLE to R.drawable.alert_slider_middle,
            KeyHandler.POSITION_BOTTOM to R.drawable.alert_slider_bottom_270
        )

        private val sIconResMap = hashMapOf(
            KeyHandler.KEY_VALUE_SILENT to R.drawable.ic_volume_ringer_mute,
            KeyHandler.KEY_VALUE_VIBRATE to R.drawable.ic_volume_ringer_vibrate,
            KeyHandler.KEY_VALUE_NORMAL to R.drawable.ic_volume_ringer,
            KeyHandler.KEY_VALUE_PRIORTY_ONLY to R.drawable.ic_notifications_alert,
            KeyHandler.KEY_VALUE_TOTAL_SILENCE to R.drawable.ic_notifications_silence
        )

        private val sTextResMap = hashMapOf(
            KeyHandler.KEY_VALUE_SILENT to R.string.notification_slider_mode_silent,
            KeyHandler.KEY_VALUE_VIBRATE to R.string.notification_slider_mode_vibrate,
            KeyHandler.KEY_VALUE_NORMAL to R.string.notification_slider_mode_none,
            KeyHandler.KEY_VALUE_PRIORTY_ONLY to R.string.notification_slider_mode_priority_only,
            KeyHandler.KEY_VALUE_TOTAL_SILENCE to R.string.notification_slider_mode_total_silence
        )
    }
}
