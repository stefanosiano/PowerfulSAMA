package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.stefanosiano.powerful_libraries.sama.R
import com.stefanosiano.powerful_libraries.sama.replaceAfterFirst
import com.stefanosiano.powerful_libraries.sama.tryOr
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.*

/** Custom AppCompatEditText that writes a bigdecimal formatted in selected locale and with convenient methods to set and get the number written */
class BigDecimalTextView : AppCompatTextView {

    private var formatter = NumberFormat.getNumberInstance(Locale.getDefault())

    /** Decimal separator based on current locale */
    private val decimalSeparator by lazy { formatter.format(0.1F).firstOrNull { !it.isDigit() } ?: '.' }


    private var currentDecimal = BigDecimal.ZERO

    constructor(context: Context?) : super(context) { init(null, 0) }
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) { init(attrs, 0) }
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int)
            : super(context, attrs, defStyleAttr)  { init(attrs, defStyleAttr) }


    fun init(attrs: AttributeSet?, defStyleAttr: Int) {

        inputType = (inputType or InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)

        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.BigDecimalTextView, defStyleAttr, 0)
        val maxFractionDigits = attrSet.getInt(R.styleable.BigDecimalTextView_bdtvMaxFractionDigits, 5)
        val minFractionDigits = attrSet.getInt(R.styleable.BigDecimalTextView_bdtvMinFractionDigits, 0)
        val locale = attrSet.getInteger(R.styleable.BigDecimalTextView_bdtvLocale, 0)
        attrSet.recycle()

        formatter = NumberFormat.getNumberInstance(when(locale) {
            0 -> Locale.getDefault()
            1 -> Locale.US
            else -> Locale.getDefault()
        })
        formatter.minimumFractionDigits = minFractionDigits
        formatter.maximumFractionDigits = maxFractionDigits

        keyListener = DigitsKeyListener.getInstance("0123456789$decimalSeparator")

        addTextChangedListener(object : TextWatcher {

            var oldText = ""

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { oldText = s.toString() }
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { }
            override fun afterTextChanged(s: Editable) {
                val clearText = s.toString()

                //check if old integer part changed. If so, number needs to be parsed again
                if(oldText.substringBefore(decimalSeparator) != clearText.substringBefore(decimalSeparator)) {
                    val num: Number = tryOr(0 as Number) { formatter.parse(clearText) ?: 0 }
                    val toSet = formatter.format(num)
                    setText(toSet)
                }

                //remove all decimal separators after first, if there are any
                val separatorCount = clearText.count { it == decimalSeparator }
                if(separatorCount > 1) {
                    setText(clearText.replaceAfterFirst(decimalSeparator.toString(), ""))
                    return
                }

                //remove all chars after maximum set fraction digits
                if(clearText.substringAfter(decimalSeparator.toString(), "").length > formatter.maximumFractionDigits) {
                    val decimalText = clearText.substringAfter(decimalSeparator.toString()).substring(0, formatter.maximumFractionDigits)
                    setText("${clearText.substringBefore(decimalSeparator.toString())}$decimalSeparator$decimalText")
                    return
                }

                val num: Number = tryOr(0 as Number) { formatter.parse(clearText) ?: 0 }
                currentDecimal = BigDecimal(num.toString())
            }
        })

        setTextBd(getTextBd())
    }

    fun setTextBd(newValue: BigDecimal?) {
        if(currentDecimal.toDouble() != newValue?.toDouble()) {
            val toSet = formatter.format(newValue ?: BigDecimal.ZERO)
            setText(toSet)
        }
    }


    fun getTextBd() : BigDecimal = currentDecimal
}
