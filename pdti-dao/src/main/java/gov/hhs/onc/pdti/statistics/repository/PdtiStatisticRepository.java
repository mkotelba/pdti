package gov.hhs.onc.pdti.statistics.repository;

import gov.hhs.onc.pdti.statistics.entity.PDTIStatisticsEntity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PdtiStatisticRepository extends JpaRepository<PDTIStatisticsEntity, Long>{

}
