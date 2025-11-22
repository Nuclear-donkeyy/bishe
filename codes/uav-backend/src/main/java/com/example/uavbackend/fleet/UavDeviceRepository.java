package com.example.uavbackend.fleet;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UavDeviceRepository extends JpaRepository<UavDevice, Long> {
  Optional<UavDevice> findByUavCode(String uavCode);

  Page<UavDevice> findAllByStatusIn(List<UavStatus> statuses, Pageable pageable);

  List<UavDevice> findByStatus(UavStatus status);
}
