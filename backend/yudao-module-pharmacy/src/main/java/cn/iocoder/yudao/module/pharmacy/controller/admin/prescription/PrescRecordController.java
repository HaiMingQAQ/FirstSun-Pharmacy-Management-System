package cn.iocoder.yudao.module.pharmacy.controller.admin.prescription;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordPageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordReviewReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.prescription.vo.PrescRecordSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.prescription.PhPrescRecordDO;
import cn.iocoder.yudao.module.pharmacy.service.prescription.PrescRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 处方记录")
@RestController
@RequestMapping("/pharmacy/prescription")
@Validated
public class PrescRecordController {

    @Resource
    private PrescRecordService prescRecordService;

    @PostMapping("/create")
    @Operation(summary = "登记处方")
    @PreAuthorize("@ss.hasPermission('pharmacy:prescription:create')")
    public CommonResult<Long> createPrescRecord(@Valid @RequestBody PrescRecordSaveReqVO createReqVO) {
        return success(prescRecordService.createPrescRecord(createReqVO));
    }

    @PostMapping("/review")
    @Operation(summary = "药师审核处方（通过/驳回）")
    @PreAuthorize("@ss.hasPermission('pharmacy:prescription:review')")
    public CommonResult<Boolean> reviewPrescRecord(@Valid @RequestBody PrescRecordReviewReqVO reviewReqVO) {
        prescRecordService.reviewPrescRecord(reviewReqVO);
        return success(true);
    }

    @PostMapping("/invalidate")
    @Operation(summary = "作废处方")
    @Parameter(name = "id", description = "处方编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:prescription:update')")
    public CommonResult<Boolean> invalidatePrescRecord(@RequestParam("id") Long id) {
        prescRecordService.invalidatePrescRecord(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得处方分页（台账）")
    @PreAuthorize("@ss.hasPermission('pharmacy:prescription:query')")
    public CommonResult<PageResult<PhPrescRecordDO>> getPrescRecordPage(@Valid PrescRecordPageReqVO pageReqVO) {
        return success(prescRecordService.getPrescRecordPage(pageReqVO));
    }

    @GetMapping("/get")
    @Operation(summary = "获得处方详情")
    @Parameter(name = "id", description = "处方编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:prescription:query')")
    public CommonResult<PrescRecordRespVO> getPrescRecord(@RequestParam("id") Long id) {
        PhPrescRecordDO record = prescRecordService.getPrescRecord(id);
        PrescRecordRespVO respVO = new PrescRecordRespVO();
        if (record != null) {
            org.springframework.beans.BeanUtils.copyProperties(record, respVO);
        }
        return success(respVO);
    }
}
