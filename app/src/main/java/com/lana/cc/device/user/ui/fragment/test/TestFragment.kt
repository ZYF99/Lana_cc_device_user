package com.lana.cc.device.user.ui.fragment.test

import android.app.AlertDialog
import android.view.View
import android.widget.Toast
import androidx.viewpager2.widget.ViewPager2
import com.lana.cc.device.user.R
import com.lana.cc.device.user.databinding.FragmentTestBinding
import com.lana.cc.device.user.manager.sharedpref.SharedPrefModel
import com.lana.cc.device.user.ui.base.BaseFragment
import com.lana.cc.device.user.util.switchThread
import io.reactivex.Completable
import java.util.concurrent.TimeUnit

class TestFragment : BaseFragment<FragmentTestBinding, TestViewModel>(
    TestViewModel::class.java, layoutRes = R.layout.fragment_test
) {
    override fun initView() {
        binding.tipScroll.setOnClickListener { viewModel.hasClickedTip.value = true }
        binding.testPager.adapter = TestPagerAdapter(
            viewModel.testList.value ?: emptyList(),
            onAnswerCorrect = { testCardBinding, currentCardPosition, answerSortId ->
                viewModel.changeRedeem(currentCardPosition){
                    if (binding.testPager.currentItem != (viewModel.testList.value?.size
                            ?: 0) - 1
                    ) //不是最后一张，滑动到下一张
                        binding.testPager.setCurrentItem(currentCardPosition + 1, true)
                    else //是最后一张 弹框问用户是否需要获取新的一轮答题
                        AlertDialog.Builder(context).setMessage("本轮答题结束，是否开启新的一轮答题")
                            .setPositiveButton(
                                "开启"
                            ) { _, _ ->
                                viewModel.fetchTestList{
                                    binding.testPager.setCurrentItem(0, true)
                                }
                            }.create().show()
                }
            },
            onAnswerError = {
                Toast.makeText(context, "答错咯~", Toast.LENGTH_SHORT).show()
            }
        )
        binding.testPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                //当pager翻页时 回调方法 并传过来当前页数
                //给title设置当前页数
                viewModel.currentPager.value = position
                setTitle(position)
            }
        })

        //观察测试列表数据变化
        viewModel.testList.observeNonNull {
            (binding.testPager.adapter as TestPagerAdapter).replaceData(it)
            setTitle(0)
        }

    }

    override fun initData() {
        viewModel.fetchTestList{}
    }

    private fun setTitle(position: Int) {
        viewModel.title.postValue("当前题目  ${position + 1}/${viewModel.testList.value?.size}")
    }

    override fun onResume() {
        binding.testPager.setCurrentItem(viewModel.currentPager.value?:0,true)
        super.onResume()
    }

}