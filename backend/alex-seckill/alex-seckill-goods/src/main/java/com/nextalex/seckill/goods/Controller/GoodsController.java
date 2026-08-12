package com.nextalex.seckill.goods.Controller;

import com.nextalex.seckill.common.aspect.ApiOperationLog;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.goods.model.vo.*;
import com.nextalex.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/seckill/goods")
@Slf4j
public class GoodsController {
    @Resource
    private GoodsService goodsService;

    @PostMapping("/list")
    @ApiOperationLog(description = "查询秒杀商品列表")
    public Response<List<FindSeckillGoodsListRspVO>> getSeckillGoodsList(@RequestBody @Validated FindSeckillGoodsListReqVO reqVO){
        return goodsService.findSeckillGoodsList(reqVO);
    }

    @PostMapping("/detail")
    @ApiOperationLog(description = "查询秒杀商品详情")
    public Response<FindSeckillGoodsDetailRspVO> getSeckillGoodsDetail(@RequestBody @Validated FindSeckillGoodsDetailReqVO reqVO) {
        return goodsService.findSeckillGoodsDetail(reqVO);
    }


}
