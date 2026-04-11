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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

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

    public PatientDetailVO buildPatientDetailVO(QueryFormDTO queryFormDTO) {
        String visitNo = queryFormDTO.getVisitNo();
        String registrationNo = queryFormDTO.getRegistrationNo();
        if (StrUtil.isBlank(registrationNo)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "登记号不能为空");
        }

        // 1. Query Patient first (Core Data)
        LambdaQueryWrapper<Patient> patientQueryWrapper = Wrappers.lambdaQuery(Patient.class)
                .eq(Patient::getRegistrationNo, registrationNo)
                .orderByDesc(Patient::getCreateTime)
                .last("LIMIT 1");
        Patient patient = patientMapper.selectOne(patientQueryWrapper);

        if (patient == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "病人不存在");
        }

        // 2. Initialize VO with Patient data
        PatientDetailVO vo = BeanUtil.convert(patient, PatientDetailVO.class);

        // 3. Query Medical Record, Admission, Diagnosis in parallel
        LambdaQueryWrapper<MedicalRecordFrontPage> queryWrapper = Wrappers.lambdaQuery(MedicalRecordFrontPage.class)
                .eq(StrUtil.isNotBlank(visitNo), MedicalRecordFrontPage::getVisitNo, visitNo)
                .eq(MedicalRecordFrontPage::getRegistrationNo, registrationNo)
                .orderByDesc(MedicalRecordFrontPage::getCreateTime)
                .last("LIMIT 1");
        
        LambdaQueryWrapper<AdmissionRecord> admissionQueryWrapper = Wrappers.lambdaQuery(AdmissionRecord.class)
                .eq(StrUtil.isNotBlank(visitNo), AdmissionRecord::getVisitNo, visitNo)
                .eq(AdmissionRecord::getRegistrationNo, registrationNo)
                .orderByDesc(AdmissionRecord::getCreateTime)
                .last("LIMIT 1");

        LambdaQueryWrapper<Diagnosis> diagWrapper = Wrappers.lambdaQuery(Diagnosis.class)
                .eq(StrUtil.isNotBlank(visitNo), Diagnosis::getVisitNo, visitNo)
                .eq(Diagnosis::getRegistrationNo, registrationNo)
                .orderByDesc(Diagnosis::getCreateTime)
                .last("LIMIT 1");

        CompletableFuture<MedicalRecordFrontPage> mrfpFuture = CompletableFuture.supplyAsync(() -> medicalRecordFrontPageMapper.selectOne(queryWrapper));
        CompletableFuture<AdmissionRecord> admissionFuture = CompletableFuture.supplyAsync(() -> admissionRecordMapper.selectOne(admissionQueryWrapper));
        CompletableFuture<Diagnosis> diagFuture = CompletableFuture.supplyAsync(() -> diagnosisMapper.selectOne(diagWrapper));

        CompletableFuture.allOf(mrfpFuture, admissionFuture, diagFuture).join();

        MedicalRecordFrontPage medicalRecordFrontPage = mrfpFuture.join();
        AdmissionRecord admissionRecord = admissionFuture.join();
        Diagnosis diagnosis = diagFuture.join();

        // 4. Merge Medical Record data if available
        if (medicalRecordFrontPage != null) {
            // Use convertIgnoreNull to preserve existing Patient data if MRFP has nulls
            BeanUtil.convertIgnoreNull(medicalRecordFrontPage, vo);
        }

        if (admissionRecord != null) {
            vo.setPastHistory(admissionRecord.getPastHistory());
            vo.setPhysicalExamWeight(admissionRecord.getPhysicalExamWeight());
            vo.setPhysicalExamVitals(admissionRecord.getPhysicalExamVitals());
            vo.setFamilyHistory(admissionRecord.getFamilyHistory());
            vo.setChiefComplaint(admissionRecord.getChiefComplaint());
            vo.setPersonalHistory(admissionRecord.getPersonalHistory());
        }

        if (diagnosis != null) {
            vo.setDeptName(diagnosis.getDeptName());
        }

        return vo;
    }
}
