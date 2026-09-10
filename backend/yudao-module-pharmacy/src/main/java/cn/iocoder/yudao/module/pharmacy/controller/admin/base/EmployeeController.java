package cn.iocoder.yudao.module.pharmacy.controller.admin.base;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee.EmployeePageReqVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee.EmployeeRespVO;
import cn.iocoder.yudao.module.pharmacy.controller.admin.base.vo.employee.EmployeeSaveReqVO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.EmployeeDO;
import cn.iocoder.yudao.module.pharmacy.dal.dataobject.base.StoreDO;
import cn.iocoder.yudao.module.pharmacy.service.base.EmployeeService;
import cn.iocoder.yudao.module.pharmacy.service.base.StoreService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 员工")
@RestController
@RequestMapping("/pharmacy/base/employee")
@Validated
public class EmployeeController {

    @Resource
    private EmployeeService employeeService;

    @Resource
    private StoreService storeService;

    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "创建员工")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:employee:create')")
    public CommonResult<Long> createEmployee(@Valid @RequestBody EmployeeSaveReqVO createReqVO) {
        Long id = employeeService.createEmployee(createReqVO);
        return success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新员工")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:employee:update')")
    public CommonResult<Boolean> updateEmployee(@Valid @RequestBody EmployeeSaveReqVO updateReqVO) {
        employeeService.updateEmployee(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除员工")
    @Parameter(name = "id", description = "员工编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:employee:delete')")
    public CommonResult<Boolean> deleteEmployee(@RequestParam("id") Long id) {
        employeeService.deleteEmployee(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得员工详情")
    @Parameter(name = "id", description = "员工编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:employee:query')")
    public CommonResult<EmployeeRespVO> getEmployee(@RequestParam("id") Long id) {
        EmployeeDO employee = employeeService.getEmployee(id);
        EmployeeRespVO respVO = BeanUtils.toBean(employee, EmployeeRespVO.class);
        // 补充关联信息
        populateRelations(respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获得员工分页")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:employee:query')")
    public CommonResult<PageResult<EmployeeRespVO>> getEmployeePage(@Validated EmployeePageReqVO pageReqVO) {
        PageResult<EmployeeDO> pageResult = employeeService.getEmployeePage(pageReqVO);
        PageResult<EmployeeRespVO> result = BeanUtils.toBean(pageResult, EmployeeRespVO.class);
        // 批量补充关联信息
        populateRelations(result.getList());
        return success(result);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出员工 Excel")
    @PreAuthorize("@ss.hasPermission('pharmacy:base:employee:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportEmployee(HttpServletResponse response, @Validated EmployeePageReqVO reqVO) throws IOException {
        reqVO.setPageSize(cn.iocoder.yudao.framework.common.pojo.PageParam.PAGE_SIZE_NONE);
        List<EmployeeDO> list = employeeService.getEmployeePage(reqVO).getList();
        List<EmployeeRespVO> respList = BeanUtils.toBean(list, EmployeeRespVO.class);
        populateRelations(respList);
        ExcelUtils.write(response, "员工.xls", "员工列表", EmployeeRespVO.class, respList);
    }

    /**
     * 批量补充门店名称与用户昵称
     */
    private void populateRelations(List<EmployeeRespVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 门店名称
        Set<Long> storeIds = list.stream().map(EmployeeRespVO::getStoreId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> storeMap = new HashMap<>();
        if (!storeIds.isEmpty()) {
            List<StoreDO> stores = storeService.getStoreList(storeIds);
            for (StoreDO store : stores) {
                storeMap.put(store.getId(), store.getStoreName());
            }
        }
        // 用户昵称
        Set<Long> userIds = list.stream().map(EmployeeRespVO::getUserId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        for (EmployeeRespVO vo : list) {
            if (vo.getStoreId() != null) {
                vo.setStoreName(storeMap.get(vo.getStoreId()));
            }
            if (vo.getUserId() != null) {
                AdminUserRespDTO user = userMap.get(vo.getUserId());
                if (user != null) {
                    vo.setUserNickname(user.getNickname());
                }
            }
        }
    }

    private void populateRelations(EmployeeRespVO vo) {
        if (vo == null) {
            return;
        }
        if (vo.getStoreId() != null) {
            StoreDO store = storeService.getStore(vo.getStoreId());
            if (store != null) {
                vo.setStoreName(store.getStoreName());
            }
        }
        if (vo.getUserId() != null) {
            AdminUserRespDTO user = adminUserApi.getUser(vo.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
            }
        }
    }

}
