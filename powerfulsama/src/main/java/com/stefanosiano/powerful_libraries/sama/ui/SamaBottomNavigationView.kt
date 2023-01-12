package com.stefanosiano.powerful_libraries.sama.ui

import android.content.Context
import android.os.Parcelable
import android.util.AttributeSet
import android.util.SparseIntArray
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.stefanosiano.powerful_libraries.sama.utils.WeakPair
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import java.lang.ref.WeakReference

/** Class that provides easy Bottom Navigation. */
open class SamaBottomNavigationView : BottomNavigationView {

    private var containerId = 0
    private var pairs: Array<WeakPair<Int, out Fragment>>? = null
    private var activityReference: WeakReference<AppCompatActivity>? = null
    private var active: WeakReference<Fragment>? = null
    private val itemSelectedListeners = ArrayList<(Int) -> Unit>()

    init {

        setOnNavigationItemReselectedListener { }
        setOnNavigationItemSelectedListener {

            val fragment: Fragment = pairs?.firstOrNull { pair -> pair.first() == it.itemId }?.second() ?: return@setOnNavigationItemSelectedListener false
            val activeFragment: Fragment? = active?.get()

            val fragmentManager = activityReference?.get()?.supportFragmentManager

            val fragmentTransaction = fragmentManager?.beginTransaction()
//                    ?.replace(containerId, fragment)
            if (activeFragment != null) fragmentTransaction?.hide(activeFragment)

            fragmentTransaction?.show(fragment)
                ?.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                ?.commitAllowingStateLoss() ?: return@setOnNavigationItemSelectedListener false

            active = WeakReference(fragment)

            itemSelectedListeners.forEach { listener -> listener.invoke(it.itemId) }

            return@setOnNavigationItemSelectedListener true
        }
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun addItemSelectedListener(listener: (Int) -> Unit) = itemSelectedListeners.add(listener)

    /** Sets pairs of <menuId, fragment> and binds them to the bottom navigation view. Remove any preexisting fragment already attached (memory leaks may still occur). */
    fun bindFragments(containerId: Int, activity: SamaActivity, pairs: Array<out Pair<Int, Fragment>>) {
        this.containerId = containerId
        this.pairs = pairs.map { WeakPair(it.first, it.second) }.toTypedArray()
        this.activityReference = WeakReference(activity)

        // needed because "Only the original thread that created a view hierarchy can touch its views." in setSelectedItemId
        post {
            selectedItemId = cacheSelectedId.get(containerId)
            cacheSelectedId.delete(containerId)

            val selectedPair: Pair<Int, Fragment> = pairs.firstOrNull { it.first == selectedItemId } ?: return@post

            val fragmentTransaction = activity.supportFragmentManager.beginTransaction()

            active = WeakReference(selectedPair.second)

            val isSelectedPairAdded = selectedPair.second.isAdded
            if (!isSelectedPairAdded) fragmentTransaction.replace(containerId, selectedPair.second)

            this.pairs?.filter { it.second() != selectedPair.second && it.second()?.isAdded == false }?.forEach { p ->
                p.second()?.also {
                    fragmentTransaction.add(containerId, it).hide(it)
                }
            }

            if (!isSelectedPairAdded) fragmentTransaction.show(selectedPair.second)
            fragmentTransaction.commitAllowingStateLoss()

            selectedItemId = selectedPair.first
        }
    }

    /**
     * Call this function to know if bottom navigation should return to first tab.
     * @return true if it returns to first tab,
     *          false if it's already at the first tab and you can close the activity
     */
    fun onBackPressed(id: Int): Boolean {
        if (id != 0 && id != selectedItemId) {
            post { selectedItemId = id }
            return true
        }
        return false
    }

    fun selectFragment(id: Int) {
        if (id != 0 && id != selectedItemId) {
            post { selectedItemId = id }
        }
    }

    override fun onSaveInstanceState(): Parcelable {
        cacheSelectedId.put(containerId, selectedItemId)
        return super.onSaveInstanceState()
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState(state)
        cacheSelectedId.delete(containerId)
    }

    companion object {
        val cacheSelectedId = SparseIntArray()
    }
}
