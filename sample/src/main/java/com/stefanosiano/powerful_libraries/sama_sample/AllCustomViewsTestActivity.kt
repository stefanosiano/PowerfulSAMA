package com.stefanosiano.powerful_libraries.sama_sample

import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.*
import androidx.lifecycle.LiveData
import com.stefanosiano.powerful_libraries.sama.ui.SamaSpinner
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaListItem
import com.stefanosiano.powerful_libraries.sama.view.SamaRvAdapter
import com.stefanosiano.powerful_libraries.sama.viewModel.SamaViewModel
import com.stefanosiano.powerful_libraries.sama.viewModel.VmAction
import com.stefanosiano.powerful_libraries.sama_sample.databinding.ActivityAllCustomViewsBinding
import kotlinx.coroutines.flow.Flow

class AllCustomViewsTestActivity : SamaActivity() {

    val testVm by viewModels<AllCustomViewsTestVM>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityAllCustomViewsBinding = DataBindingUtil.setContentView(this, R.layout.activity_all_custom_views)
        binding.testVm = testVm
        onVmAction(testVm, ::handleVmResponse)
    }

    fun handleVmResponse(action: AlcvTestAction) {
//        when(action) {}
    }
}

class AllCustomViewsTestVM : SamaViewModel<AlcvTestAction>() {
    val searchMillis = ObservableInt(0)
    val searchTerm = ObservableField("")
    val spnKey = ObservableField("")
    val spnValue = ObservableField("")
    val spnItems = ObservableField( emptyList<SamaSpinner.SamaSpinnerItem>() )
    val rvAdapter = SamaRvAdapter(R.layout.list_item_test, BR.item)

    fun setSearchMillis(millis: Int) { searchMillis.set(millis) }
    fun setSearchTerm(term: String) { searchTerm.set(term) }
    fun setSpnItems(items: List<SamaSpinner.SamaSpinnerItem>) { spnItems.set(items) }
    fun bindTestItems(items: ObservableList<TestListItem>) { rvAdapter.bindItems(items) }
    fun bindTestItems(items: Flow<List<TestListItem>>) { rvAdapter.bindItems(items) }
    fun bindTestItems(items: LiveData<List<TestListItem>>) { rvAdapter.bindItems(items) }
    fun bindTestItems(items: List<TestListItem>) { rvAdapter.bindItems(items) }
}

sealed class AlcvTestAction : VmAction {}

class TestListItem (val title: String, val subtitle: String): SamaListItem() {
    override fun getStableIdString(): String = title
}