package com.stefanosiano.powerful_libraries.samasample_androidtest.extensions

import android.view.View
import android.widget.Spinner
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

class SpinnerActionExtension

class SpinnerAssertion private constructor(private val matcher: Matcher<String>) : ViewAssertion {
    override fun check(view: View, noViewFoundException: NoMatchingViewException?) {
        if (noViewFoundException != null) {
            throw noViewFoundException
        }
        val spinner = view as Spinner
        ViewMatchers.assertThat(spinner.selectedItem.toString(), matcher)
    }

    companion object {
        fun withText(expectedQuery: String): SpinnerAssertion {
            return SpinnerAssertion(CoreMatchers.equalTo(expectedQuery))
        }
    }
}
