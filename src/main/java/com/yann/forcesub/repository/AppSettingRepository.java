package com.yann.forcesub.repository;

import com.yann.forcesub.entity.AppSetting;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.io.Flushable;
import java.util.List;

@Repository
public interface AppSettingRepository extends ReactiveMongoRepository<AppSetting, String> {

    Flux<AppSetting> getAppSettingByKey(String key);
}
