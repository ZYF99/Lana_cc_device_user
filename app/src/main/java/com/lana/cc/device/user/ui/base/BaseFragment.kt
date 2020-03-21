package com.lana.cc.device.user.ui.base

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.lana.cc.device.user.util.DialogUtil
import io.reactivex.Completable
import io.reactivex.disposables.CompositeDisposable
import org.kodein.di.KodeinAware
import org.kodein.di.android.x.kodein
import com.lana.cc.device.user.BR
import com.lana.cc.device.user.R
import com.lana.cc.device.user.databinding.DialogManageChooseBinding
import com.lana.cc.device.user.databinding.DialogManageNewsBinding
import com.lana.cc.device.user.model.api.ApiException

abstract class BaseFragment<Bind : ViewDataBinding, VM : BaseViewModel>
constructor(
    private val clazz: Class<VM>,
    private val bindingCreator: (LayoutInflater, ViewGroup?) -> Bind
) : Fragment(), BindLife, KodeinAware {

    constructor(clazz: Class<VM>, @LayoutRes layoutRes: Int) : this(clazz, { inflater, group ->
        DataBindingUtil.inflate(inflater, layoutRes, group, false)
    })

    protected open val viewModel: VM by lazy {
        ViewModelProviders.of(this).get(clazz)
    }

    protected lateinit var binding: Bind

    override val kodein by kodein()

    override val compositeDisposable = CompositeDisposable()

    //method
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        binding = bindingCreator.invoke(layoutInflater, container)
        binding.lifecycleOwner = this
        binding.setVariable(BR.vm, viewModel)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initEventObserver()
        initView()
        initDataAlways()
        if (!viewModel.vmInit) initData()
        viewModel.vmInit = true
    }


    private fun initEventObserver() {
        viewModel.apiError.bindDialog(context!!, this)
        viewModel.dialogEvent.bindDialog(context!!, this)
        viewModel.progressDialog.observeNonNull {
            if (it) DialogUtil.showProgressDialog(context)
            else DialogUtil.hideProgressDialog()
        }
    }

    abstract fun initView()
    //initData will invoke only after Fragment first created.
    abstract fun initData()

    open fun initDataAlways() {}

    //ext
    protected fun <T> LiveData<T>.observe(observer: (T?) -> Unit) where T : Any =
        observe(viewLifecycleOwner, Observer<T> { v -> observer(v) })

    protected fun <T> LiveData<T>.observeNonNull(observer: (T) -> Unit) {
        this.observe(viewLifecycleOwner, Observer {
            if (it != null) {
                observer(it)
            }
        })
    }

    fun Context.checkNet(): Completable =
        Completable.create { emitter ->
            if (!isNetworkAvailable()) emitter.onError(
                ApiException(
                    2222,
                    getString(R.string.net_unavailable)
                )
            )
            else emitter.onComplete()
        }

    //check network
    protected fun isNetworkAvailable() =
        (context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)?.activeNetworkInfo?.isConnected
            ?: false

    override fun onDestroy() {
        super.onDestroy()
        destroyDisposable()
    }

    //安卓原生弹窗  设置信息界面
    fun showViewDialog(view: View?, onConfirmAction: () -> Unit) {
        AlertDialog.Builder(context!!)
            .setView(view)
            .setCancelable(true)
            .setPositiveButton("确定")
            { _, _ ->
                //将方法参数中的action行为 传入这里 即达到传入的action在点击之后调用
                onConfirmAction()
            }
            .create()
            .show()
    }

    fun showManageChooseDialog(onEditClick: () -> Unit, onDeleteClick: () -> Unit) {
        val manageChooseDialogBinding =
            DataBindingUtil.inflate<DialogManageChooseBinding>(
                LayoutInflater.from(context),//一个Inflater对象，打开新布局都需要使用Inflater对象
                R.layout.dialog_manage_choose,//弹窗的layout文件
                null,//填null 无需多了解
                false//填false无需多了解
            )
        val chooseDialog = AlertDialog.Builder(context!!).setView(manageChooseDialogBinding.root)
            .setCancelable(true).create()

        manageChooseDialogBinding.tvEdit.setOnClickListener {
            onEditClick()
            chooseDialog.dismiss()
        }
        manageChooseDialogBinding.tvDelete.setOnClickListener {
            chooseDialog.dismiss()
            AlertDialog.Builder(context!!)
                .setTitle("警告")
                .setMessage("删除后将不能恢复，确认删除吗")
                .setCancelable(true)
                .setPositiveButton("确认") { _, _ ->
                    onDeleteClick()
                }
                .create()
                .show()
        }
        chooseDialog.show()
    }

    protected enum class EditType{
        ADD,EDIT
    }

}
