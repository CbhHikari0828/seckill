package com.nextalex.seckill.goods.Controller;

import com.nextalex.seckill.common.aspect.ApiOperationLog;
import com.nextalex.seckill.common.utils.Response;
import com.nextalex.seckill.goods.model.vo.PreheatActivityCacheReqVO;
import com.nextalex.seckill.goods.service.GoodsService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/seckill/goods")
@Slf4j
public class AdminGoodsController {

    @Resource
    private GoodsService goodsService;

    @PostMapping("cache/preheat")
    @ApiOperationLog(description = "手动预热缓存")
    public Response<?> preheatCache(@RequestBody @Validated PreheatActivityCacheReqVO reqVO) {
        return goodsService.preheatActivityGoods(reqVO.getActivityId());
    }
}
