package com.nlpai4h.healthydemobacked.service.helper;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.nlpai4h.healthydemobacked.common.exception.BusinessException;
import com.nlpai4h.healthydemobacked.common.result.ErrorCode;
import com.nlpai4h.healthydemobacked.mapper.AdmissionRecordMapper;
import com.nlpai4h.healthydemobacked.mapper.DiagnosisMapper;
import com.nlpai4h.healthydemobacked.mapper.MedicalRecordFrontPageMapper;
import com.nlpai4h.healthydemobacked.mapper.PatientMapper;
import com.nlpai4h.healthydemobacked.model.dto.QueryFormDTO;
import com.nlpai4h.healthydemobacked.model.entity.AdmissionRecord;
import com.nlpai4h.healthydemobacked.model.entity.Diagnosis;
import com.nlpai4h.healthydemobacked.model.entity.MedicalRecordFrontPage;
import com.nlpai4h.healthydemobacked.model.entity.Patient;
import com.nlpai4h.healthydemobacked.model.vo.PatientDetailVO;
import com.nlpai4h.healthydemobacked.utils.BeanUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 患者数据组装助手类
 * 负责统一查询、组装患者相关的所有详情数据，封装为VO返回
 */
@Component
@Slf4j
public class PatientDataHelper {

    @Resource
    private MedicalRecordFrontPageMapper medicalRecordFrontPageMapper;
    @Resource
    private PatientMapper patientMapper;
    @Resource
    private AdmissionRecordMapper admissionRecordMapper;
    @Resource
    private DiagnosisMapper diagnosisMapper;

    /**
     * 构建患者详情VO对象
     * 根据登记号+就诊号，异步并行查询患者、病案、入院、诊断信息并组装返回
     *
     * @param queryFormDTO 查询条件DTO（包含登记号、就诊号）
     * @return 组装完成的患者详情VO
     * @throws BusinessException 参数为空/患者不存在时抛出业务异常
     */
    public PatientDetailVO buildPatientDetailVO(QueryFormDTO queryFormDTO) {
        // 获取查询条件
        String visitNo = queryFormDTO.getVisitNo();
        String registrationNo = queryFormDTO.getRegistrationNo();

        // 校验核心参数：登记号不能为空
        if (StrUtil.isBlank(registrationNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "登记号不能为空");
        }

        // ====================== 1. 查询患者核心基础信息 ======================
        // 构建查询条件：按登记号精确匹配，按创建时间倒序，取最新1条
        LambdaQueryWrapper<Patient> patientQueryWrapper = Wrappers.lambdaQuery(Patient.class)
                .eq(Patient::getRegistrationNo, registrationNo)
                .orderByDesc(Patient::getCreateTime)
                .last("LIMIT 1");
        Patient patient = patientMapper.selectOne(patientQueryWrapper);

        // 患者不存在，抛出异常
        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "病人不存在");
        }

        // ====================== 2. 初始化VO对象 ======================
        // 将患者基础信息转换为VO
        PatientDetailVO vo = BeanUtil.convert(patient, PatientDetailVO.class);

        // ====================== 3. 构建各表查询条件 ======================
        // 病案首页查询条件：登记号必传，就诊号可选，取最新1条
        LambdaQueryWrapper<MedicalRecordFrontPage> queryWrapper = Wrappers.lambdaQuery(MedicalRecordFrontPage.class)
                .eq(StrUtil.isNotBlank(visitNo), MedicalRecordFrontPage::getVisitNo, visitNo)
                .eq(MedicalRecordFrontPage::getRegistrationNo, registrationNo)
                .orderByDesc(MedicalRecordFrontPage::getCreateTime)
                .last("LIMIT 1");

        // 入院记录查询条件
        LambdaQueryWrapper<AdmissionRecord> admissionQueryWrapper = Wrappers.lambdaQuery(AdmissionRecord.class)
                .eq(StrUtil.isNotBlank(visitNo), AdmissionRecord::getVisitNo, visitNo)
                .eq(AdmissionRecord::getRegistrationNo, registrationNo)
                .orderByDesc(AdmissionRecord::getCreateTime)
                .last("LIMIT 1");

        // 诊断信息查询条件
        LambdaQueryWrapper<Diagnosis> diagWrapper = Wrappers.lambdaQuery(Diagnosis.class)
                .eq(StrUtil.isNotBlank(visitNo), Diagnosis::getVisitNo, visitNo)
                .eq(Diagnosis::getRegistrationNo, registrationNo)
                .orderByDesc(Diagnosis::getCreateTime)
                .last("LIMIT 1");

        // ====================== 4. 异步并行查询（提升接口性能） ======================
        CompletableFuture<MedicalRecordFrontPage> mrfpFuture = CompletableFuture.supplyAsync(() -> medicalRecordFrontPageMapper.selectOne(queryWrapper));
        CompletableFuture<AdmissionRecord> admissionFuture = CompletableFuture.supplyAsync(() -> admissionRecordMapper.selectOne(admissionQueryWrapper));
        CompletableFuture<Diagnosis> diagFuture = CompletableFuture.supplyAsync(() -> diagnosisMapper.selectOne(diagWrapper));

        // 等待所有异步任务执行完成
        CompletableFuture.allOf(mrfpFuture, admissionFuture, diagFuture).join();

        // 获取异步查询结果
        MedicalRecordFrontPage medicalRecordFrontPage = mrfpFuture.join();
        AdmissionRecord admissionRecord = admissionFuture.join();
        Diagnosis diagnosis = diagFuture.join();

        // ====================== 5. 数据合并到VO ======================
        // 合并病案首页数据：忽略空值，避免覆盖已有患者信息
        if (medicalRecordFrontPage != null) {
            BeanUtil.convertIgnoreNull(medicalRecordFrontPage, vo);
        }

        // 合并入院记录数据
        if (admissionRecord != null) {
            vo.setPastHistory(admissionRecord.getPastHistory());
            vo.setPhysicalExamWeight(admissionRecord.getPhysicalExamWeight());
            vo.setPhysicalExamVitals(admissionRecord.getPhysicalExamVitals());
            vo.setFamilyHistory(admissionRecord.getFamilyHistory());
            vo.setChiefComplaint(admissionRecord.getChiefComplaint());
            vo.setPersonalHistory(admissionRecord.getPersonalHistory());
        }

        // 合并诊断科室信息
        if (diagnosis != null) {
            vo.setDeptName(diagnosis.getDeptName());
        }

        // 返回最终组装完成的患者详情VO
        return vo;
    }
}