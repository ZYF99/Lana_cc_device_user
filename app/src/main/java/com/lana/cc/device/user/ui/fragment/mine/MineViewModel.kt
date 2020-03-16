package com.lana.cc.device.user.ui.fragment.mine

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.lana.cc.device.user.BuildConfig
import com.lana.cc.device.user.manager.api.GoodsService
import com.lana.cc.device.user.manager.api.UserService
import com.lana.cc.device.user.manager.sharedpref.SharedPrefModel
import com.lana.cc.device.user.model.api.mine.Profile
import com.lana.cc.device.user.model.api.ResultModel
import com.lana.cc.device.user.model.api.mine.UpdateUserModel
import com.lana.cc.device.user.ui.base.BaseViewModel
import com.lana.cc.device.user.ui.utils.getImageFromServer
import com.lana.cc.device.user.util.getAgeByBirth
import io.reactivex.Single
import com.lana.cc.device.user.util.switchThread
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.kodein.di.generic.instance
import java.io.File
import java.util.*

class MineViewModel(application: Application) : BaseViewModel(application) {

    val profile = MutableLiveData(Profile.getDefault())
    val age = MutableLiveData("0岁")
    private val userService by instance<UserService>()
    private val goodsService by instance<GoodsService>()
    val avatarFile = MutableLiveData<File>()
    val avatar = MutableLiveData<String>()
    val isRefreshing = MutableLiveData(false)

    fun getUserProfile() {
        userService.getUserProfile(SharedPrefModel.uid)
            .dealGetProfileSuccess()

    }

    private fun Single<ResultModel<Profile>>.dealGetProfileSuccess() =
        doOnApiSuccess {
            profile.postValue(it.data)
            avatar.postValue("${BuildConfig.BASE_URL}/image/${it.data?.avatar}")
            age.postValue(getAgeByBirth(Date(it.data?.birthday ?: 0.toLong())).toString() + "岁")
        }


    fun getExchangeGoodsList() {
        goodsService.getExchangeHistoryList().doOnApiSuccess {

        }
    }

    //上传头像,并修改头像
    fun uploadAvatar() {
        if (avatarFile.value != null) {
            val photoRequestBody =
                RequestBody.create("image/png".toMediaTypeOrNull(), avatarFile.value!!)
            val photo = MultipartBody.Part.createFormData(
                "imageFile",
                avatarFile.value!!.name,
                photoRequestBody
            )
            userService.upLoadAvatar(
                photo
            ).flatMap {
                //上传成功就更新本地头像
                avatar.postValue(getImageFromServer(it.data?.imagePath))
                //调用修改头像api
                userService.updateUserProfile(
                    UpdateUserModel(
                        0, "", "", it.data?.imagePath
                    )
                )
            }.doOnApiSuccess {}
        }
    }

    //更改 昵称 生日 签名
    fun updateUserProfile(birthLong: Long, nickName: String, signature: String) {
        userService.updateUserProfile(
            UpdateUserModel(
                birthLong, nickName, signature, ""
            )
        ).flatMap {
            userService.getUserProfile(SharedPrefModel.uid)
        }.dealGetProfileSuccess()
    }


    private fun <T> Single<T>.doOnApiSuccess(action: ((T) -> Unit)?) {
        switchThread()
            .doOnSuccess {
                action?.invoke(it)
            }
            .doOnSubscribe { isRefreshing.postValue(true) }
            .doOnSuccess { isRefreshing.postValue(false) }
            .doOnError { isRefreshing.postValue(false) }
            .catchApiError()
            .bindLife()
    }

}