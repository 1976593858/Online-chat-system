package com.onlinechat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Data
@Schema(description = "分页查询参数")
public class PageQuery {

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "页码", example = "1")
    private long pageNo = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    @Schema(description = "每页条数", example = "10")
    private long pageSize = 10;
}
