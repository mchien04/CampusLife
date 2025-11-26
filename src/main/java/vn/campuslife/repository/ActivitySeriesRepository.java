package vn.campuslife.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.campuslife.entity.ActivitySeries;

@Repository
public interface ActivitySeriesRepository extends JpaRepository<ActivitySeries, Long> {
}

