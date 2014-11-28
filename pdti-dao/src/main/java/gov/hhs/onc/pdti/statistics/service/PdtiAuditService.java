package gov.hhs.onc.pdti.statistics.service;

import gov.hhs.onc.pdti.statistics.entity.PDTIStatisticsEntity;
import gov.hhs.onc.pdti.statistics.repository.PdtiStatisticRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PdtiAuditService {

	@Autowired
	PdtiStatisticRepository pdtiRPdtiStatisticRepository;
	
	public List<PDTIStatisticsEntity> findAll(){
		return pdtiRPdtiStatisticRepository.findAll();
	}
	
	public PDTIStatisticsEntity findOne(Long id) {
		return pdtiRPdtiStatisticRepository.findOne(id);
	}
	
	public PDTIStatisticsEntity savePdtiStatisticsEntity (PDTIStatisticsEntity entity) {
		return pdtiRPdtiStatisticRepository.save(entity);
	}
}
