package com.stefanosiano.powerful_libraries.samasample_androidtest.extensions

import android.content.res.Resources
import android.view.View
import android.widget.Spinner
import androidx.appcompat.widget.SearchView
import androidx.test.espresso.*
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers
import com.stefanosiano.powerful_libraries.sama_sample.R
import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers
import org.hamcrest.core.AllOf

class SpinnerActionExtension {

}

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