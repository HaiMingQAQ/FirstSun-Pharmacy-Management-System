package cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.drug.DrugPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo.AppCategoryRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo.AppDrugPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.app.pharmacy.vo.AppDrugRespVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.CategoryDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.DrugDO;
import cn.iocoder.yudao.module.pharmacy.service.base.CategoryService;
import cn.iocoder.yudao.module.pharmacy.service.base.DrugService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_DRUG_NOT_EXISTS;
import static cn.iocoder.yudao.module.pharmacy.enums.ErrorCodeConstants.PHARMACY_DRUG_NOT_SALEABLE;

/**
 * 用户 APP - 药店商品浏览（只读）
 *
 * 说明：小程序首页/分类/详情需要商品数据，本控制器是 A 成员提供的
 * 商品能力（{@link DrugService} / {@link CategoryService}）在 app-api 的只读投影，
 * **不修改也不复制 A 的商品逻辑**，仅做「可线上销售」过滤与字段裁剪。
 *
 * 商品浏览无需登录（{@code @PermitAll}），加购与下单需要会员令牌。
 */
@Tag(name = "用户 APP - 药店商品浏览")
@RestController
@RequestMapping("/pharmacy/drug")
@Validated
@Slf4j
public class AppDrugController {

    /** 启用状态：1=启用（pharmacy 业务表口径） */
    private static final Integer STATUS_ENABLE = 1;
    /** 审核状态：1=通过 */
    private static final Integer APPROVE_PASS = 1;
    /** 线上可售：1=支持 */
    private static final Integer SALEABLE_ONLINE = 1;

    @Resource
    private DrugService drugService;

    @Resource
    private CategoryService categoryService;

    @GetMapping("/page")
    @Operation(summary = "获得可线上销售药品分页")
    @PermitAll
    public CommonResult<PageResult<AppDrugRespVO>> getDrugPage(@Valid AppDrugPageReqVO reqVO) {
        // 复用 A 的药品分页查询；服务端强制只返回「已审核通过 + 启用 + 线上可售」的药品
        DrugPageReqVO pageReqVO = new DrugPageReqVO();
        pageReqVO.setPageNo(reqVO.getPageNo());
        pageReqVO.setPageSize(reqVO.getPageSize());
        pageReqVO.setCategoryId(reqVO.getCategoryId());
        pageReqVO.setGenericName(reqVO.getKeyword());
        pageReqVO.setStatus(STATUS_ENABLE);
        pageReqVO.setApproveStatus(APPROVE_PASS);
        pageReqVO.setSaleableOnline(SALEABLE_ONLINE);
        PageResult<DrugDO> pageResult = drugService.getDrugPage(pageReqVO);
        PageResult<AppDrugRespVO> respPage = BeanUtils.toBean(pageResult, AppDrugRespVO.class);
        // 补充分类名称
        fillCategoryName(respPage.getList());
        return success(respPage);
    }

    @GetMapping("/get")
    @Operation(summary = "获得药品详情（仅可线上销售的药品）")
    @Parameter(name = "id", description = "药品编号", required = true, example = "1")
    @PermitAll
    public CommonResult<AppDrugRespVO> getDrug(@RequestParam("id") Long id) {
        DrugDO drug = drugService.getDrug(id);
        if (drug == null) {
            throw exception(PHARMACY_DRUG_NOT_EXISTS);
        }
        // 与列表口径保持一致：仅已审核通过 + 启用 + 线上可售的药品对外可见
        if (!Objects.equals(drug.getStatus(), STATUS_ENABLE)
                || !Objects.equals(drug.getApproveStatus(), APPROVE_PASS)
                || !Objects.equals(drug.getSaleableOnline(), SALEABLE_ONLINE)) {
            throw exception(PHARMACY_DRUG_NOT_SALEABLE);
        }
        AppDrugRespVO respVO = BeanUtils.toBean(drug, AppDrugRespVO.class);
        fillCategoryName(java.util.Collections.singletonList(respVO));
        return success(respVO);
    }

    @GetMapping("/category-list")
    @Operation(summary = "获得启用的药品分类列表")
    @PermitAll
    public CommonResult<List<AppCategoryRespVO>> getCategoryList() {
        List<CategoryDO> list = categoryService.getEnabledCategoryList();
        return success(BeanUtils.toBean(list, AppCategoryRespVO.class));
    }

    /**
     * 批量补充分类名称
     */
    private void fillCategoryName(List<AppDrugRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Set<Long> categoryIds = list.stream().map(AppDrugRespVO::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (categoryIds.isEmpty()) {
            return;
        }
        Map<Long, CategoryDO> categoryMap = categoryService.getCategoryList(categoryIds).stream()
                .collect(Collectors.toMap(CategoryDO::getId, Function.identity(), (a, b) -> a));
        for (AppDrugRespVO vo : list) {
            CategoryDO category = categoryMap.get(vo.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getCatName());
            }
        }
    }

}
