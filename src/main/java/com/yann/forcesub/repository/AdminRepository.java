package com.yann.forcesub.repository;

import com.yann.forcesub.entity.Admin;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends ReactiveMongoRepository<Admin, Long> {
}
