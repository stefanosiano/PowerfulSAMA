package com.stefanosiano.powerful_libraries.samasample_androidtest.extensions

import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.hamcrest.core.AllOf

internal class SearchViewActionExtension {

    companion object {
        fun submitText(text: String): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): Matcher<View> {
                    return AllOf.allOf(isDisplayed(), isAssignableFrom(SearchView::class.java))
                }

                override fun getDescription(): String {
                    return "Set text and submit"
                }

                override fun perform(uiController: UiController, view: View) {
                    (view as SearchView).setQuery(text, true) // submit=true will fire search
                }
            }
        }
        fun typeTextSearchView(text: String): ViewAction {
            return object : ViewAction {
                override fun getConstraints(): org.hamcrest.Matcher<View> {
                    return AllOf.allOf(isDisplayed(), isAssignableFrom(SearchView::class.java))
                }

                override fun getDescription(): String {
                    return "Set text"
                }

                override fun perform(uiController: UiController, view: View) {
                    (view as SearchView).setQuery(text, false)
                }
            }
        }
    }
}

internal class SearchViewAssertion private constructor(private val matcher: Matcher<String>) : ViewAssertion {
    override fun check(view: View, noViewFoundException: NoMatchingViewException?) {
        if (noViewFoundException != null) {
            throw noViewFoundException
        }
        val searchView = view as SearchView
        ViewMatchers.assertThat(searchView.query?.toString(), matcher)
    }

    companion object {
        fun withQuery(expectedQuery: String): SearchViewAssertion {
            return SearchViewAssertion(CoreMatchers.equalTo(expectedQuery))
        }
    }
}
