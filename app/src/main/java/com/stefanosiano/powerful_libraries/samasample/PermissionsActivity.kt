package com.stefanosiano.powerful_libraries.samasample

import android.Manifest
import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import com.stefanosiano.powerful_libraries.sama.utils.Perms
import com.stefanosiano.powerful_libraries.sama.view.SamaActivity
import com.stefanosiano.powerful_libraries.sama.viewModel.SamaViewModel
import com.stefanosiano.powerful_libraries.sama.viewModel.VmAction
import com.stefanosiano.powerful_libraries.samasample.databinding.ActivityPermissionsBinding

internal class PermissionsActivity : SamaActivity() {

    val permVm by viewModels<PermissionsVM>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityPermissionsBinding =
            DataBindingUtil.setContentView(this, R.layout.activity_permissions)
        binding.permVm = permVm
    }
}

internal class PermissionsVM : SamaViewModel<PermissionAction>() {
    var permissionsAskedSuccessfully = false

    fun askPermission() {
        permissionsAskedSuccessfully = false
        Perms.call(
            listOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            R.string.ask_location,
            R.string.ask_location_permanently_denied,
            listOf(Manifest.permission.CAMERA)
        ) {
            permissionsAskedSuccessfully = true
        }
    }
}

internal sealed class PermissionAction : VmAction
