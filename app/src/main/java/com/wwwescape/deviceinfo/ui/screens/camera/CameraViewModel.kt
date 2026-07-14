package com.wwwescape.deviceinfo.ui.screens.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.wwwescape.deviceinfo.data.camera.CameraLensInfo
import com.wwwescape.deviceinfo.data.camera.CameraRepository
import com.wwwescape.deviceinfo.util.CameraLensRole
import com.wwwescape.deviceinfo.util.classifyLensRoles
import com.wwwescape.deviceinfo.util.mainLens

class CameraViewModel(application: Application) : AndroidViewModel(application) {
    val cameras: List<CameraLensInfo> = CameraRepository.listCameras(application)
    val lensRoles: Map<String, CameraLensRole> = classifyLensRoles(cameras)
    val mainLens: CameraLensInfo? = mainLens(cameras, lensRoles)
}
