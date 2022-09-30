package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.appcompat.widget.AppCompatTextView
import com.google.android.material.textfield.TextInputEditText
import com.stefanosiano.powerful_libraries.sama.R
import com.stefanosiano.powerful_libraries.sama.extensions.replaceAfterFirst
import com.stefanosiano.powerful_libraries.sama.extensions.tryOr
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Custom AppCompatEditText that writes a [BigDecimal] formatted in selected locale,
 * and with convenient methods to set and get the number written.
 */
@Suppress("StringLiteralDuplication")
class BigDecimalEditText : AppCompatEditText {

    private var formatter = NumberFormat.getNumberInstance(Locale.getDefault())

    /** Decimal separator based on current locale. */
    private val decimalSeparator by lazy { formatter.format(0.1F).firstOrNull { !it.isDigit() } ?: '.' }

    private var currentDecimal = BigDecimal.ZERO

    constructor(context: Context) : super(context) {
        init(null, 0)
    }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs, 0)
    }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)  {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        inputType = inputType or InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.BigDecimalEditText, defStyleAttr, 0)
        val maxFractionDigits = attrSet.getInt(R.styleable.BigDecimalEditText_bdetMaxFractionDigits, 5)
        val minFractionDigits = attrSet.getInt(R.styleable.BigDecimalEditText_bdetMinFractionDigits, 0)
        val locale = attrSet.getInteger(R.styleable.BigDecimalEditText_bdetLocale, 0)
        attrSet.recycle()

        formatter = initFormatter(locale, minFractionDigits, maxFractionDigits)
        keyListener = DigitsKeyListener.getInstance("0123456789$decimalSeparator")

        addTextChangedListener(BigDecimalTextWatcher(this, formatter, decimalSeparator) { currentDecimal = it })
        setTextBd(getTextBd())
    }

    /** Set [newValue] as text. */
    fun setTextBd(newValue: BigDecimal?) = setTextBd(this, currentDecimal, newValue, formatter)

    /** Get the current text as [BigDecimal]. */
    fun getTextBd() : BigDecimal = currentDecimal
}

/**
 * Custom TextInputEditText that writes a [BigDecimal] formatted in selected locale
 * and with convenient methods to set and get the number written.
 */
class BigDecimalTextInputEditText : TextInputEditText {

    private var formatter = NumberFormat.getNumberInstance(Locale.getDefault())

    /** Decimal separator based on current locale. */
    private val decimalSeparator by lazy { formatter.format(0.1F).firstOrNull { !it.isDigit() } ?: '.' }

    private var currentDecimal = BigDecimal.ZERO

    constructor(context: Context) : super(context) { init(null, 0) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init(attrs, 0) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)  {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        inputType = inputType or InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.BigDecimalEditText, defStyleAttr, 0)
        val maxFractionDigits = attrSet.getInt(R.styleable.BigDecimalEditText_bdetMaxFractionDigits, 5)
        val minFractionDigits = attrSet.getInt(R.styleable.BigDecimalEditText_bdetMinFractionDigits, 0)
        val locale = attrSet.getInteger(R.styleable.BigDecimalEditText_bdetLocale, 0)
        attrSet.recycle()

        formatter = initFormatter(locale, minFractionDigits, maxFractionDigits)
        keyListener = DigitsKeyListener.getInstance("0123456789$decimalSeparator")

        addTextChangedListener(BigDecimalTextWatcher(this, formatter, decimalSeparator) { currentDecimal = it })
        setTextBd(getTextBd())
    }

    /** Set [newValue] as text. */
    fun setTextBd(newValue: BigDecimal?) = setTextBd(this, currentDecimal, newValue, formatter)

    /** Get the current text as [BigDecimal]. */
    fun getTextBd() : BigDecimal = currentDecimal
}

/**
 * Custom AppCompatTextView that writes a [BigDecimal] formatted in selected locale
 * and with convenient methods to set and get the number written.
 */
class BigDecimalTextView : AppCompatTextView {

    private var formatter = NumberFormat.getNumberInstance(Locale.getDefault())

    /** Decimal separator based on current locale. */
    private val decimalSeparator by lazy { formatter.format(0.1F).firstOrNull { !it.isDigit() } ?: '.' }

    private var currentDecimal = BigDecimal.ZERO

    constructor(context: Context) : super(context) { init(null, 0) }
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) { init(attrs, 0) }
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)  {
        init(attrs, defStyleAttr)
    }

    private fun init(attrs: AttributeSet?, defStyleAttr: Int) {
        inputType = inputType or InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL

        val attrSet = context.theme.obtainStyledAttributes(attrs, R.styleable.BigDecimalTextView, defStyleAttr, 0)
        val maxFractionDigits = attrSet.getInt(R.styleable.BigDecimalTextView_bdtvMaxFractionDigits, 5)
        val minFractionDigits = attrSet.getInt(R.styleable.BigDecimalTextView_bdtvMinFractionDigits, 0)
        val locale = attrSet.getInteger(R.styleable.BigDecimalTextView_bdtvLocale, 0)
        attrSet.recycle()

        formatter = initFormatter(locale, minFractionDigits, maxFractionDigits)
        keyListener = DigitsKeyListener.getInstance("0123456789$decimalSeparator")

        addTextChangedListener(BigDecimalTextWatcher(this, formatter, decimalSeparator) { currentDecimal = it })
        setTextBd(getTextBd())
    }

    /** Set [newValue] as text. */
    fun setTextBd(newValue: BigDecimal?) = setTextBd(this, currentDecimal, newValue, formatter)

    /** Get the current text as [BigDecimal]. */
    fun getTextBd() : BigDecimal = currentDecimal
}


private fun initFormatter(locale: Int, minFractionDigits: Int, maxFractionDigits: Int): NumberFormat {
    val formatter = NumberFormat.getNumberInstance(when(locale) {
        0 -> Locale.getDefault()
        1 -> Locale.US
        else -> Locale.getDefault()
    })
    formatter.minimumFractionDigits = minFractionDigits
    formatter.maximumFractionDigits = maxFractionDigits
    return formatter
}

private fun <T: TextView> setTextBd(v: T, oldValue: BigDecimal, newValue: BigDecimal?, formatter: NumberFormat) {
    if(oldValue.toDouble() != newValue?.toDouble() || v.text.isEmpty()) {
        val cursorPosition = v.selectionEnd
        val toSet = formatter.format(newValue ?: BigDecimal.ZERO)
        v.text = toSet
        if(v is EditText) {
            v.setSelection(cursorPosition.coerceIn(0, toSet.length))
        }
    }
}

private class BigDecimalTextWatcher<T: TextView> (
    val v: T,
    var formatter: NumberFormat,
    val decimalSeparator: Char,
    val f: (BigDecimal) -> Unit
) : TextWatcher {

    var oldText = ""

    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { oldText = s.toString() }
    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) { }
    override fun afterTextChanged(s: Editable) {
        val clearText = s.toString()

        //check if old integer part changed. If so, number needs to be parsed again
        if(oldText.substringBefore(decimalSeparator) != clearText.substringBefore(decimalSeparator)) {
            val num: Number = tryOr(0 as Number) { formatter.parse(clearText) ?: 0 }
            val toSet = formatter.format(num)
            val cursorPosition = when {
                toSet.length - oldText.length > 1 ->
                    v.selectionEnd + (toSet.length - oldText.length - 1).coerceAtLeast(0)
                toSet.length - oldText.length < -1 ->
                    v.selectionEnd + (toSet.length - oldText.length + 1).coerceAtMost(0)
                else -> v.selectionEnd
            }

            v.text = toSet
            if(v is EditText) {
                v.setSelection(cursorPosition.coerceIn(0, toSet.length))
            }
        }

        //remove all decimal separators after first, if there are any
        val separatorCount = clearText.count { it == decimalSeparator }
        if(separatorCount > 1) {
            val cursorPosition = v.selectionEnd - separatorCount + 1
            v.text = clearText.replaceAfterFirst(decimalSeparator.toString(), "")
            if(v is EditText) {
                v.setSelection(cursorPosition)
            }
            return
        }

        //remove all chars after maximum set fraction digits
        if(clearText.substringAfter(decimalSeparator.toString(), "").length > formatter.maximumFractionDigits) {
            val decimalText =
                clearText.substringAfter(decimalSeparator.toString()).substring(0, formatter.maximumFractionDigits)
            v.text = "${clearText.substringBefore(decimalSeparator.toString())}$decimalSeparator$decimalText"
            if(v is EditText) {
                v.setSelection(v.text.toString().length)
            }
            return
        }

        val num: Number = tryOr(0 as Number) { formatter.parse(clearText) ?: 0 }
        f(BigDecimal(num.toString()))
    }
}
