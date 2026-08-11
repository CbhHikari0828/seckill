package com.nextalex.seckill.common.domain.dataobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoodsImgDO {
    private Long id;

    private Long goodsId;

    private String imgUrl;

    private Integer sort;

    private LocalDateTime createTime;

}