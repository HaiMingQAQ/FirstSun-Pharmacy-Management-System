package cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo.AppStoreRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 用户 APP - 门店选择（只读）
 *
 * 小程序下单需要指定履约门店，本控制器是 A 成员门店能力
 * （{@link StoreService#getEnabledStoreList()}）在 app-api 的只读投影。
 */
@Tag(name = "用户 APP - 门店选择")
@RestController
@RequestMapping("/pharmacy/store")
@Validated
@Slf4j
public class AppStoreController {

    @Resource
    private StoreService storeService;

    @GetMapping("/list")
    @Operation(summary = "获得营业中的门店列表")
    @PermitAll
    public CommonResult<List<AppStoreRespVO>> getStoreList() {
        List<StoreDO> list = storeService.getEnabledStoreList();
        return success(BeanUtils.toBean(list, AppStoreRespVO.class));
    }

}
