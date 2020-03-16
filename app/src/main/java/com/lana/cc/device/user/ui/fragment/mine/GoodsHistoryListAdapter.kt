package com.lana.cc.device.user.ui.fragment.mine

import com.lana.cc.device.user.R
import com.lana.cc.device.user.databinding.ItemGoodsHistoryBinding
import com.lana.cc.device.user.model.GoodsHistory
import com.lana.cc.device.user.ui.adapter.BaseRecyclerAdapter

//商品历史列表的Adapter，继承自BaseRecyclerAdapter
class GoodsHistoryListAdapter(
    onClick: (GoodsHistory) -> Unit
) : BaseRecyclerAdapter<GoodsHistory, ItemGoodsHistoryBinding>(
    R.layout.item_goods_history,
    onClick
) {
    override fun bindData(binding: ItemGoodsHistoryBinding, position: Int) {
        binding.goodsHistory = baseList[position]
    }
}