package com.nlpai4h.healthydemobacked.controller;

import com.nlpai4h.healthydemobacked.common.context.BaseContext;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.PageResult;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.common.result.Result;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.Patient;
import com.nlpai4h.healthydemobacked.model.entity.Permission;
import com.nlpai4h.healthydemobacked.model.vo.PatientRecordDetailVO;
import com.nlpai4h.healthydemobacked.service.IPatientService;
import com.nlpai4h.healthydemobacked.service.IPermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 患者信息控制器
 * 提供患者基本信息及详细就诊记录的查询和管理接口
 */
@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired
    private IPatientService patientService;

    @Autowired
    private IPermissionService permissionService;

    /**
     * 根据患者ID获取患者基本信息
     *
     * @param id 患者ID
     * @return 包含患者基本信息的响应结果
     */
    @GetMapping("/{id}")
    public Result<Patient> getById(@PathVariable Long id) {
        if (!hasViewPermission()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看病人信息");
        }
        return Result.success(patientService.getById(id));
    }

    /**
     * 获取患者就诊记录详情（包含检验检查、诊断等）
     *
     * @param queryFormDTO 包含查询条件（如就诊号）
     * @return 包含患者记录详细信息的响应结果
     */
    @GetMapping("/detail")
    public Result<PatientRecordDetailVO> getDetail(QueryFormDTO queryFormDTO) {
        if (!hasViewPermission()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看病人信息");
        }
        return Result.success(patientService.getPatientRecordDetail(queryFormDTO));
    }

    /**
     * 分页查询患者列表
     *
     * @param page           当前页码
     * @param pageSize       每页大小
     * @param registrationNo 挂号单号（可选过滤条件）
     * @return 包含分页患者数据的响应结果
     */
    @GetMapping
    public Result<PageResult> pageQuery(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer pageSize,
                                        @RequestParam(required = false) String registrationNo) {
        if (!hasViewPermission()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权限查看病人信息");
        }
        return Result.success(patientService.pageQuery(page, pageSize, registrationNo));
    }

    /**
     * 校验当前用户是否有查看患者信息的权限
     *
     * @return 有权限返回true，否则返回false
     */
    private boolean hasViewPermission() {
        Long userId = BaseContext.getCurrentId();
        if ("admin".equals(BaseContext.getCurrentUser().getRole())) {
            return true;
        }
        List<Permission> permissions = permissionService.getUserPermissions(userId);
        return permissions.stream().anyMatch(p -> "patient:view".equals(p.getCode()));
    }

    /**
     * 新增患者信息
     *
     * @param patient 患者实体信息
     * @return 是否保存成功的响应结果
     */
    @PostMapping
    public Result<Boolean> save(@RequestBody Patient patient) {
        return Result.success(patientService.save(patient));
    }

    /**
     * 更新患者信息
     *
     * @param patient 待更新的患者实体信息
     * @return 是否更新成功的响应结果
     */
    @PutMapping
    public Result<Boolean> update(@RequestBody Patient patient) {
        return Result.success(patientService.updateById(patient));
    }

    /**
     * 删除患者信息
     *
     * @param id 待删除的患者ID
     * @return 是否删除成功的响应结果
     */
    @DeleteMapping("/{id}")
    public Result<Boolean> remove(@PathVariable Long id) {
        return Result.success(patientService.removeById(id));
    }
}
