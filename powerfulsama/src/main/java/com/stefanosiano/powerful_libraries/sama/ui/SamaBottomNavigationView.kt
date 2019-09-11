package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseIntArray
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stefanosiano.powerful_libraries.sama.launch
import com.stefanosiano.powerful_libraries.sama.runOnUi
import com.stefanosiano.powerful_libraries.sama.utils.WeakPair
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.view.SamaFragment
import java.lang.ref.WeakReference


/**
 * Class that provides easy Bottom Navigation
 */
class SamaBottomNavigationView: BottomNavigationView {

    companion object {
        val cacheSelectedId = SparseIntArray()
    }

    private var containerId = 0
    private var pairs: Array<WeakPair<Int, out SamaFragment>>? = null
    private var activityReference: WeakReference<AppCompatActivity>? = null
    private var active: WeakReference<Fragment>? = null
    private val itemSelectedListeners = ArrayList<(Int) -> Unit>()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)


    init {

        setOnNavigationItemReselectedListener {  }
        setOnNavigationItemSelectedListener {

            val fragment: Fragment = pairs?.firstOrNull { pair -> pair.first() == it.itemId }?.second() ?: return@setOnNavigationItemSelectedListener false
            val activeFragment: Fragment? = active?.get()

            val fragmentManager = activityReference?.get()?.supportFragmentManager

            val fragmentTransaction = fragmentManager?.beginTransaction()
//                    ?.replace(containerId, fragment)
            if(activeFragment != null) fragmentTransaction?.hide(activeFragment)

            fragmentTransaction?.show(fragment)
                ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                ?.commitAllowingStateLoss() ?: return@setOnNavigationItemSelectedListener false

            active = WeakReference(fragment)

            itemSelectedListeners.forEach { listener -> listener.invoke(it.itemId) }

            return@setOnNavigationItemSelectedListener true
        }
    }

    fun addItemSelectedListener(listener: (Int) -> Unit) = itemSelectedListeners.add(listener)

    /** Sets pairs of <menuId, fragment> and binds them to the bottom navigation view */
    fun bindFragments(containerId: Int, activity: SamaActivity, pairs: Array<out Pair<Int, SamaFragment>>) {

        this.containerId = containerId
        this.pairs = pairs.map { WeakPair(it.first, it.second) }.toTypedArray()
        this.activityReference = WeakReference(activity)

        //needed because "Only the original thread that created a view hierarchy can touch its views." in setSelectedItemId
        runOnUi {
            selectedItemId = cacheSelectedId.get(containerId)
            cacheSelectedId.delete(containerId)

            val pair: Pair<Int, SamaFragment> = pairs.firstOrNull { it.first == selectedItemId } ?: return@runOnUi

            val fragmentTransaction = activity.supportFragmentManager.beginTransaction()

            active = WeakReference(pair.second)

            if(!pair.second.isAdded) fragmentTransaction.add(containerId, pair.second)

            fragmentTransaction
                .show(pair.second)
                .commitAllowingStateLoss()

            this.pairs?.filter { it.second() != pair.second && it.second()?.isAdded == false }?.launch(activity) { p -> p.second()?.also {
                fragmentTransaction.add(containerId, it).hide(it)
            } }
            selectedItemId = pair.first
        }
    }

    /**
     * Call this function to know if bottom navigation should return to first tab.
     * @return true if it returns to first tab,
     *          false if it's already at the first tab and you can close the activity
     */
    fun onBackPressed(id: Int): Boolean {
        if (id != 0 && id != selectedItemId) {
            runOnUi { selectedItemId = id }
            return true
        }
        return false
    }

    fun selectFragment(id: Int) {
        if (id != 0 && id != selectedItemId) {
            runOnUi { selectedItemId = id }
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        cacheSelectedId.put(containerId, selectedItemId)
        return super.onSaveInstanceState()
    }
}