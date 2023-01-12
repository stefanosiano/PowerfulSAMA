package com.stefanosiano.powerful_libraries.samasample_androidtest

import androidx.databinding.ObservableArrayList
import androidx.lifecycle.MutableLiveData
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.stefanosiano.powerful_libraries.sama.ui.SamaSpinner
import com.stefanosiano.powerful_libraries.sama_sample.AllCustomViewsTestActivity
import com.stefanosiano.powerful_libraries.sama_sample.AllCustomViewsTestVM
import com.stefanosiano.powerful_libraries.sama_sample.R
import com.stefanosiano.powerful_libraries.sama_sample.TestListItem
import com.stefanosiano.powerful_libraries.samasample_androidtest.extensions.RecyclerViewItemCountAssertion.Companion.withItemCount
import com.stefanosiano.powerful_libraries.samasample_androidtest.extensions.SearchViewActionExtension.Companion.typeTextSearchView
import com.stefanosiano.powerful_libraries.samasample_androidtest.extensions.SearchViewAssertion
import com.stefanosiano.powerful_libraries.samasample_androidtest.extensions.SpinnerAssertion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AllCustomViewActivityTest {

    /** Create and launch the activity under test before each test, and close it after each test. */
    @get:Rule var activityScenarioRule = ActivityScenarioRule(AllCustomViewsTestActivity::class.java)

    lateinit var testVm: AllCustomViewsTestVM

    @Before
    fun init() {
        activityScenarioRule.scenario.onActivity { testVm = it.testVm }
    }

    /** Run each tests 10 times to be sure everything works. */
    @Test
    fun testAll_AllCustomViewActivity() {
        for (i in 0 until 10) {
            println("Test N. $i")
            spinnerKeyValueChange_AllCustomViewActivity()
            searchViewValueChange_AllCustomViewActivity()
            recyclerViewListChange_AllCustomViewActivity()
        }
    }

    /** Test binding of key and value of spinner, whenever an item is selected or the observableField is changed. */
    @Test
    fun spinnerKeyValueChange_AllCustomViewActivity() {
        testVm.setSpnItems(
            listOf(
                SamaSpinner.SamaSpinnerItem("key1", "value1"),
                SamaSpinner.SamaSpinnerItem("key2", "value2"),
                SamaSpinner.SamaSpinnerItem("key3", "value3"),
                SamaSpinner.SamaSpinnerItem("key4", "value4")
            )
        )

        // Type "fromSearch" and check the viewModel field
        Espresso.onView(withId(R.id.testSpinner)).perform(ViewActions.click())
        Espresso.onData(Matchers.equalTo("value3")).perform(ViewActions.click())

        runBlocking { delay(30) }
        Assert.assertEquals("key3", testVm.spnKey.get())
        Assert.assertEquals("value3", testVm.spnValue.get())

        testVm.spnValue.set("value2")
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testSpinner)).check(SpinnerAssertion.withText("value2"))
        runBlocking { delay(30) }
        Assert.assertEquals("key2", testVm.spnKey.get())
        Assert.assertEquals("value2", testVm.spnValue.get())

        testVm.spnKey.set("key4")
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testSpinner)).check(SpinnerAssertion.withText("value4"))
        runBlocking { delay(30) }
        Assert.assertEquals("key4", testVm.spnKey.get())
        Assert.assertEquals("value4", testVm.spnValue.get())
    }

    /** Test binding of items of recyclerViewAdapter, whenever they change. Using List, ObservableList, LiveData<List>. */
    @Test
    fun recyclerViewListChange_AllCustomViewActivity() {
        val itemsLiveData = MutableLiveData(
            listOf(
                TestListItem("title LiveData 1", "subtitle LiveData 1"),
                TestListItem("title LiveData 2", "subtitle LiveData 2"),
                TestListItem("title LiveData 3", "subtitle LiveData 3")
            )
        )
        val itemsObservableList = ObservableArrayList<TestListItem>()
        itemsObservableList.addAll(
            listOf(
                TestListItem("title Obs 1", "subtitle Obs 1"),
                TestListItem("title Obs 2", "subtitle Obs 2")
            )
        )
        val flowList = MutableStateFlow(
            listOf(
                TestListItem("title Flow 1", "subtitle Flow 1"),
                TestListItem("title Flow 2", "subtitle Flow 2")
            )
        )

        testVm.bindTestItems(emptyList())
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(0))

        testVm.bindTestItems(listOf(TestListItem("t1", "d1"), TestListItem("t2", "d2")))
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(2))

        testVm.bindTestItems(itemsLiveData)
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(3))

        itemsLiveData.postValue(listOf(TestListItem("title LiveData 1", "subtitle LiveData 1")))
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(1))

        testVm.bindTestItems(itemsObservableList)
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(2))

        itemsObservableList.add(TestListItem("title Obs 3", "subtitle Obs 3"))
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(3))

        itemsObservableList.removeAt(0)
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(2))

        itemsObservableList.clear()
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(0))

        testVm.bindTestItems(flowList)
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(2))

        runBlocking {
            flowList.emit(
                listOf(
                    TestListItem("title Flow 1", "subtitle Flow 1"),
                    TestListItem("title Flow 2", "subtitle Flow 2"),
                    TestListItem("title Flow 3", "subtitle Flow 3")
                )
            )
        }
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testRecyclerView)).check(withItemCount(3))
    }

    /** Test binding of text and milliseconds of searchview, whenever a text is written or the observableField is changed. */
    @Test
    fun searchViewValueChange_AllCustomViewActivity() {
        testVm.setSearchMillis(0)
        testVm.setSearchTerm("")
        Assert.assertEquals("", testVm.searchTerm.get())

        // Type "fromSearch" and check the viewModel field
        Espresso.onView(withId(R.id.testSearch)).perform(typeTextSearchView("fromSearch"))
        runBlocking { delay(30) }
        Assert.assertEquals("fromSearch", testVm.searchTerm.get())

        // Change the viewModel field in "fromVm" and check the searchView
        testVm.setSearchTerm("fromVm")
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testSearch)).check(SearchViewAssertion.withQuery("fromVm"))

        testVm.setSearchMillis(300)
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testSearch)).perform(typeTextSearchView("fromSearch"))
        runBlocking { delay(30) }
        Assert.assertEquals("fromVm", testVm.searchTerm.get())
        runBlocking { delay(100) }
        Assert.assertEquals("fromVm", testVm.searchTerm.get())
        runBlocking { delay(250) }
        Assert.assertEquals("fromSearch", testVm.searchTerm.get())

        testVm.setSearchMillis(0)
        runBlocking { delay(30) }
        Espresso.onView(withId(R.id.testSearch)).perform(typeTextSearchView("fromSearch2"))
        runBlocking { delay(30) }
        Assert.assertEquals("fromSearch2", testVm.searchTerm.get())
    }
}
