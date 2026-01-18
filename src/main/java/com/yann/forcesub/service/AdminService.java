package com.yann.forcesub.service;

import com.yann.forcesub.entity.Admin;
import com.yann.forcesub.event.AdminUpdateEvent;
import com.yann.forcesub.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminService {

    @Value("#{'${admin.id}'.split(',')}")
    private List<Long> adminIdList;

    private final AdminRepository adminRepository;
    private final Set<Long> adminIds = new HashSet<>();

    public void saveAdmin(Long id) {
        adminIds.add(id);
        adminRepository.save(new Admin(id));
    }

    public Mono<Boolean> isAdmin(Long id) {
        return Mono.just(adminIds.contains(id));
    }


    @EventListener(AdminUpdateEvent.class)
    public void updateAdmin(AdminUpdateEvent event) {
        if (event.isDelete()) {
            adminIds.remove(event.getId());
        } else {
            adminIds.add(event.getId());
        }
    }

    @PostConstruct
    public void init() {
        for (Long id : adminIdList) {
            saveAdmin(id);
        }
    }

}