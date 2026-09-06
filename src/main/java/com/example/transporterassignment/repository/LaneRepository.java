package com.example.transporterassignment.repository;

import com.example.transporterassignment.model.Lane;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LaneRepository extends JpaRepository<Lane, Long> {
}
