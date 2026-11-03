package com.dasisiheung.repository;

import com.dasisiheung.entity.privacy.MonthlyUsageStat;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MonthlyUsageStatRepository extends JpaRepository<MonthlyUsageStat, MonthlyUsageStat.Key> {

    @Query("""
            select s from MonthlyUsageStat s
            where s.key.statMonth between :from and :to
            order by s.key.statMonth, s.key.region, s.key.activityType
            """)
    List<MonthlyUsageStat> findBetween(@Param("from") LocalDate fromMonth, @Param("to") LocalDate toMonth);
}
