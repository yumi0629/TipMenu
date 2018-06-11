package com.yumi.tipmenu.tipmenu

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView

/**
 * @param clickType 长按 or 点击触发弹出tipMenu
 * @param cornerPosition 三角小箭头的位置
 */
class TipMenu(val context: Context, val clickType: Int = TYPE_LONG_CLICK, val cornerPosition: Int = CORNER_POSITION_TOP) {

    companion object {
        const val TYPE_CLICK_NONE = 0
        const val TYPE_LONG_CLICK = 1
        const val TYPE_CLICK = 2

        const val CORNER_POSITION_TOP = 0
        const val CORNER_POSITION_BOTTOM = 1
    }

    private val DEFAULT_NORMAL_TEXT_COLOR = Color.WHITE
    private val DEFAULT_PRESSED_TEXT_COLOR = Color.WHITE
    private val DEFAULT_TEXT_SIZE_DP = 14f
    private val DEFAULT_TEXT_PADDING_LEFT_DP = 15.0f
    private val DEFAULT_TEXT_PADDING_TOP_DP = 12.0f
    private val DEFAULT_TEXT_PADDING_RIGHT_DP = 15.0f
    private val DEFAULT_TEXT_PADDING_BOTTOM_DP = 12.0f
    private val DEFAULT_NORMAL_BACKGROUND_COLOR = -0x34000000
    private val DEFAULT_PRESSED_BACKGROUND_COLOR = -0x18888889
    private val DEFAULT_BACKGROUND_RADIUS_DP = 8
    private val DEFAULT_DIVIDER_COLOR = -0x65000001
    private val DEFAULT_DIVIDER_WIDTH_DP = 0.5f
    private val DEFAULT_DIVIDER_HEIGHT_DP = 20.0f

    private var popupWindow: PopupWindow? = null
    private lateinit var anchorView: View
    private val indicatorView: View by lazy {
        getDefaultIndicatorView(context)
    }
    private lateinit var menus: List<String>
    private var popupMenuListener: PopupMenuListener? = null
    private var rawX: Float = 0f
    private var rawY: Float = 0f
    private var leftItemBackground: StateListDrawable = StateListDrawable()
    private var rightItemBackground: StateListDrawable = StateListDrawable()
    private var cornerItemBackground: StateListDrawable = StateListDrawable()
    private var textColorStateList: ColorStateList? = null
    private var cornerBackground: GradientDrawable = GradientDrawable()
    private var indicatorWidth: Int = 0
    private var indicatorHeight: Int = 0
    private var popupWindowWidth: Int = 0
    private var popupWindowHeight: Int = 0

    private val screenWidth: Int by lazy {
        getDefaultScreenWidth()
    }
    private val screenHeight: Int by lazy {
        getDefaultScreenHeight()
    }
    private var normalTextColor: Int = DEFAULT_NORMAL_TEXT_COLOR
        set(value) {
            field = value
            refreshTextColorStateList(pressedTextColor, field)
        }

    private var pressedTextColor: Int = DEFAULT_PRESSED_TEXT_COLOR
        set(value) {
            field = value
            refreshTextColorStateList(pressedTextColor, field)
        }
    private var textSize: Float = dp2px(DEFAULT_TEXT_SIZE_DP).toFloat()
    private var textPaddingLeft = dp2px(DEFAULT_TEXT_PADDING_LEFT_DP)
    private var textPaddingTop: Int = dp2px(DEFAULT_TEXT_PADDING_TOP_DP)
    private var textPaddingRight: Int = dp2px(DEFAULT_TEXT_PADDING_RIGHT_DP)
    private var textPaddingBottom: Int = dp2px(DEFAULT_TEXT_PADDING_BOTTOM_DP)
    private var normalBackgroundColor: Int = DEFAULT_NORMAL_BACKGROUND_COLOR
        set(value) {
            field = value
            refreshBackgroundOrRadiusStateList()
        }
    private var pressedBackgroundColor: Int = DEFAULT_PRESSED_BACKGROUND_COLOR
        set(value) {
            field = value
            refreshBackgroundOrRadiusStateList()
        }
    private var backgroundCornerRadius: Int = dp2px(DEFAULT_BACKGROUND_RADIUS_DP.toFloat())
        set(value) {
            field = value
            refreshBackgroundOrRadiusStateList()
        }
    private var dividerColor: Int = DEFAULT_DIVIDER_COLOR
    private var dividerWidth: Int = dp2px(DEFAULT_DIVIDER_WIDTH_DP)
    private var dividerHeight: Int = dp2px(DEFAULT_DIVIDER_HEIGHT_DP)

    init {
        refreshBackgroundOrRadiusStateList()
        refreshTextColorStateList(pressedTextColor, normalTextColor)
    }

    fun bind(anchorView: View, menus: List<String>, popupMenuListener: PopupMenuListener?) {
        this.anchorView = anchorView
        this.menus = menus
        this.popupMenuListener = popupMenuListener
        this.popupWindow = null
        this.anchorView.setOnTouchListener { _, event ->
            rawX = event.rawX
            rawY = event.rawY
            false
        }
        when (clickType) {
            TYPE_LONG_CLICK -> this.anchorView.setOnLongClickListener {
                showPopupListWindow()
                true
            }
            TYPE_CLICK -> this.anchorView.setOnClickListener { showPopupListWindow() }
        }
    }

    private fun showPopupListWindow() {
        if (context is Activity && (context as Activity).isFinishing) {
            return
        }
        if (popupWindow == null) {
            val contentView = LinearLayout(context)
            contentView.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            contentView.orientation = LinearLayout.VERTICAL

            val layoutParams = if (indicatorView.layoutParams == null) {
                LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            } else {
                indicatorView.layoutParams as LinearLayout.LayoutParams
            }
            layoutParams.gravity = Gravity.CENTER
            indicatorView.layoutParams = layoutParams
            val viewParent = indicatorView.parent
            if (viewParent is ViewGroup) {
                viewParent.removeView(indicatorView)
            }

            val popupListContainer = LinearLayout(context)
            popupListContainer.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            popupListContainer.orientation = LinearLayout.HORIZONTAL
            popupListContainer.setBackgroundDrawable(cornerBackground)

            when (cornerPosition) {
                CORNER_POSITION_TOP -> {
                    contentView.run {
                        addView(indicatorView)
                        addView(popupListContainer)
                    }
                }
                CORNER_POSITION_BOTTOM -> {
                    contentView.run {
                        addView(popupListContainer)
                        addView(indicatorView)
                    }
                }
            }

            for (i in menus.indices) {
                val textView = TextView(context)
                textView.setTextColor(textColorStateList)
                textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)
                textView.setPadding(textPaddingLeft, textPaddingTop, textPaddingRight, textPaddingBottom)
                textView.isClickable = true
                textView.setOnClickListener {
                    popupMenuListener?.run {
                        onPopupMenuClick(i)
                        hidePopupListWindow()
                    }

                }
                textView.text = menus[i]

                if (menus.size > 1 && i == 0) {
                    textView.setBackgroundDrawable(leftItemBackground)
                } else if (menus.size > 1 && i == menus.size - 1) {
                    textView.setBackgroundDrawable(rightItemBackground)
                } else if (menus.size == 1) {
                    textView.setBackgroundDrawable(cornerItemBackground)
                } else {
                    textView.setBackgroundDrawable(getCenterItemBackground())
                }
                popupListContainer.addView(textView)
                if (menus.size > 1 && i != menus.size - 1) {
                    val divider = View(context)
                    val lp = LinearLayout.LayoutParams(dividerWidth, dividerHeight)
                    lp.gravity = Gravity.CENTER
                    divider.layoutParams = lp
                    divider.setBackgroundColor(dividerColor)
                    popupListContainer.addView(divider)
                }
            }
            if (popupWindowWidth == 0) {
                popupWindowWidth = getViewWidth(popupListContainer)
            }
            if (indicatorWidth == 0) {
                indicatorWidth = if (indicatorView.layoutParams.width > 0) {
                    indicatorView.layoutParams.width
                } else {
                    getViewWidth(indicatorView)
                }
            }
            if (indicatorHeight == 0) {
                indicatorHeight = if (indicatorView.layoutParams.height > 0) {
                    indicatorView.layoutParams.height
                } else {
                    getViewHeight(indicatorView)
                }
            }
            if (popupWindowHeight == 0) {
                popupWindowHeight = getViewHeight(popupListContainer) + indicatorHeight
            }
            popupWindow = PopupWindow(contentView, popupWindowWidth, popupWindowHeight, true)
            popupWindow?.run {
                isTouchable = true
                setBackgroundDrawable(BitmapDrawable())
            }
        }
        val marginLeftScreenEdge = rawX
        val marginRightScreenEdge = screenWidth - rawX
        if (marginLeftScreenEdge < popupWindowWidth / 2f) {
            // in case of the draw of indicator out of corner's bounds
            if (marginLeftScreenEdge < indicatorWidth / 2f + backgroundCornerRadius) {
                indicatorView.translationX = indicatorWidth / 2f + backgroundCornerRadius - popupWindowWidth / 2f
            } else {
                indicatorView.translationX = marginLeftScreenEdge - popupWindowWidth / 2f
            }
        } else if (marginRightScreenEdge < popupWindowWidth / 2f) {
            if (marginRightScreenEdge < indicatorWidth / 2f + backgroundCornerRadius) {
                indicatorView.translationX = popupWindowWidth / 2f - indicatorWidth / 2f - backgroundCornerRadius.toFloat()
            } else {
                indicatorView.translationX = popupWindowWidth / 2f - marginRightScreenEdge
            }
        } else {
            indicatorView.translationX = 0f
        }

        showTipMenu()
    }

    private fun showTipMenu() {
        popupWindow?.run {
            if (!isShowing) {
                when (cornerPosition) {
                    CORNER_POSITION_TOP -> {
                        showAtLocation(anchorView, Gravity.CENTER,
                                rawX.toInt() - screenWidth / 2,
                                rawY.toInt() - screenHeight / 2 + popupWindowHeight / 2 + indicatorHeight)
                    }

                    CORNER_POSITION_BOTTOM -> {
                        showAtLocation(anchorView, Gravity.CENTER,
                                rawX.toInt() - screenWidth / 2,
                                rawY.toInt() - screenHeight / 2 - popupWindowHeight + indicatorHeight)
                    }

                }
            }
        }
    }

    private fun hidePopupListWindow() {
        if (context is Activity && (context as Activity).isFinishing) {
            return
        }
        popupWindow?.run {
            if (isShowing) dismiss()
        }
    }

    private fun refreshBackgroundOrRadiusStateList() {
        // left
        val leftItemPressedDrawable = GradientDrawable()
        leftItemPressedDrawable.setColor(pressedBackgroundColor)
        leftItemPressedDrawable.cornerRadii = floatArrayOf(backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat(), 0f, 0f, 0f, 0f, backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat())
        val leftItemNormalDrawable = GradientDrawable()
        leftItemNormalDrawable.setColor(Color.TRANSPARENT)
        leftItemNormalDrawable.cornerRadii = floatArrayOf(backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat(), 0f, 0f, 0f, 0f, backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat())
        leftItemBackground = StateListDrawable()
        leftItemBackground.addState(intArrayOf(android.R.attr.state_pressed), leftItemPressedDrawable)
        leftItemBackground.addState(intArrayOf(), leftItemNormalDrawable)
        // right
        val rightItemPressedDrawable = GradientDrawable()
        rightItemPressedDrawable.setColor(pressedBackgroundColor)
        rightItemPressedDrawable.cornerRadii = floatArrayOf(0f, 0f, backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat(), 0f, 0f)
        val rightItemNormalDrawable = GradientDrawable()
        rightItemNormalDrawable.setColor(Color.TRANSPARENT)
        rightItemNormalDrawable.cornerRadii = floatArrayOf(0f, 0f, backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat(), backgroundCornerRadius.toFloat(), 0f, 0f)
        rightItemBackground = StateListDrawable()
        rightItemBackground.addState(intArrayOf(android.R.attr.state_pressed), rightItemPressedDrawable)
        rightItemBackground.addState(intArrayOf(), rightItemNormalDrawable)
        // corner
        val cornerItemPressedDrawable = GradientDrawable()
        cornerItemPressedDrawable.setColor(pressedBackgroundColor)
        cornerItemPressedDrawable.cornerRadius = backgroundCornerRadius.toFloat()
        val cornerItemNormalDrawable = GradientDrawable()
        cornerItemNormalDrawable.setColor(Color.TRANSPARENT)
        cornerItemNormalDrawable.cornerRadius = backgroundCornerRadius.toFloat()
        cornerItemBackground = StateListDrawable()
        cornerItemBackground.addState(intArrayOf(android.R.attr.state_pressed), cornerItemPressedDrawable)
        cornerItemBackground.addState(intArrayOf(), cornerItemNormalDrawable)
        cornerBackground = GradientDrawable()
        cornerBackground.setColor(normalBackgroundColor)
        cornerBackground.cornerRadius = backgroundCornerRadius.toFloat()
    }

    private fun refreshTextColorStateList(pressedTextColor: Int, normalTextColor: Int) {
        val states = arrayOfNulls<IntArray>(2)
        states[0] = intArrayOf(android.R.attr.state_pressed)
        states[1] = intArrayOf()
        val colors = intArrayOf(pressedTextColor, normalTextColor)
        textColorStateList = ColorStateList(states, colors)
    }

    private fun getCenterItemBackground(): StateListDrawable {
        val centerItemBackground = StateListDrawable()
        val centerItemPressedDrawable = GradientDrawable()
        centerItemPressedDrawable.setColor(pressedBackgroundColor)
        val centerItemNormalDrawable = GradientDrawable()
        centerItemNormalDrawable.setColor(Color.TRANSPARENT)
        centerItemBackground.addState(intArrayOf(android.R.attr.state_pressed), centerItemPressedDrawable)
        centerItemBackground.addState(intArrayOf(), centerItemNormalDrawable)
        return centerItemBackground
    }

    private fun getDefaultIndicatorView(context: Context): View {
        return getTriangleIndicatorView(context, dp2px(16f).toFloat(), dp2px(8f).toFloat(), DEFAULT_NORMAL_BACKGROUND_COLOR)
    }

    private fun getTriangleIndicatorView(context: Context, widthPixel: Float, heightPixel: Float,
                                         color: Int): View {
        val indicator = ImageView(context)
        val drawable = object : Drawable() {
            override fun draw(canvas: Canvas) {
                val path = Path()
                val paint = Paint()
                paint.color = color
                paint.style = Paint.Style.FILL

                when (cornerPosition) {
                    CORNER_POSITION_TOP -> {
                        path.run {
                            moveTo(0f, heightPixel)
                            lineTo(widthPixel, heightPixel)
                            lineTo(widthPixel / 2, 0f)
                        }
                    }
                    CORNER_POSITION_BOTTOM -> {
                        path.run {
                            moveTo(0f, 0f)
                            lineTo(widthPixel, 0f)
                            lineTo(widthPixel / 2, heightPixel)
                        }
                    }
                }
                path.close()
                canvas.drawPath(path, paint)
            }

            override fun setAlpha(alpha: Int) {

            }

            override fun setColorFilter(colorFilter: ColorFilter?) {

            }

            override fun getOpacity(): Int {
                return PixelFormat.TRANSLUCENT
            }

            override fun getIntrinsicWidth(): Int {
                return widthPixel.toInt()
            }

            override fun getIntrinsicHeight(): Int {
                return heightPixel.toInt()
            }
        }
        indicator.setImageDrawable(drawable)
        return indicator
    }

    private fun getDefaultScreenWidth(): Int {
        val wm = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.widthPixels
    }

    private fun getDefaultScreenHeight(): Int {
        val wm = context
                .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val outMetrics = DisplayMetrics()
        wm.defaultDisplay.getMetrics(outMetrics)
        return outMetrics.heightPixels
    }

    private fun getViewWidth(view: View): Int {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        return view.measuredWidth
    }

    private fun getViewHeight(view: View): Int {
        view.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        return view.measuredHeight
    }

    fun dp2px(value: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                value, context.resources.displayMetrics).toInt()
    }

    fun sp2px(value: Float): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                value, context.resources.displayMetrics).toInt()
    }

    fun setTextPadding(left: Int, top: Int, right: Int, bottom: Int) {
        this.textPaddingLeft = left
        this.textPaddingTop = top
        this.textPaddingRight = right
        this.textPaddingBottom = bottom
    }

}