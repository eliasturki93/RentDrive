package com.rentdrive.service.impl;

import com.rentdrive.dto.request.CreateStoreRequest;
import com.rentdrive.dto.request.UpdateStoreRequest;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.StoreResponse;
import com.rentdrive.dto.response.StoreSummaryResponse;
import com.rentdrive.entity.Store;
import com.rentdrive.entity.User;
import com.rentdrive.enums.StoreStatus;
import com.rentdrive.enums.StoreType;
import com.rentdrive.exception.ConflictException;
import com.rentdrive.exception.ForbiddenException;
import com.rentdrive.exception.ResourceNotFoundException;
import com.rentdrive.mapper.StoreMapper;
import com.rentdrive.repository.StoreRepository;
import com.rentdrive.repository.UserRepository;
import com.rentdrive.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final UserRepository  userRepository;
    private final StoreMapper     storeMapper;

    @Override
    @Transactional
    public StoreResponse createStore(UUID ownerId, CreateStoreRequest request) {
        if (storeRepository.existsByOwnerId(ownerId)) {
            throw new ConflictException("Vous possédez déjà un store.");
        }

        User owner = userRepository.findByIdWithAllRelations(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", ownerId));

        boolean canOwn = owner.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("BAILLEUR")
                            || r.getName().name().equals("AGENCE"));
        if (!canOwn) {
            throw new ForbiddenException("Seuls les BAILLEUR et AGENCE peuvent créer un store.");
        }

        Store store = new Store();
        store.setOwner(owner);
        store.setName(request.name().trim());
        store.setType(request.type());
        store.setStatus(StoreStatus.PENDING);
        store.setDescription(request.description());
        store.setPhone(request.phone());
        store.setAddress(request.address());
        store.setCity(request.city());
        store.setWilaya(request.wilaya());

        Store saved = storeRepository.save(store);
        log.info("Store créé : {} (owner={})", saved.getId(), ownerId);
        return storeMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getMyStore(UUID ownerId) {
        Store store = storeRepository.findByOwnerIdWithOwner(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "owner=" + ownerId));
        return storeMapper.toResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse updateMyStore(UUID ownerId, UpdateStoreRequest request) {
        Store store = storeRepository.findByOwnerIdWithOwner(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "owner=" + ownerId));

        if (StringUtils.hasText(request.name()))        store.setName(request.name().trim());
        if (StringUtils.hasText(request.description())) store.setDescription(request.description());
        if (StringUtils.hasText(request.logoUrl()))     store.setLogoUrl(request.logoUrl());
        if (StringUtils.hasText(request.phone()))       store.setPhone(request.phone());
        if (StringUtils.hasText(request.address()))     store.setAddress(request.address());
        if (StringUtils.hasText(request.city()))        store.setCity(request.city());
        if (StringUtils.hasText(request.wilaya()))      store.setWilaya(request.wilaya());

        log.info("Store {} mis à jour par owner {}", store.getId(), ownerId);
        return storeMapper.toResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getById(UUID storeId) {
        Store store = storeRepository.findByIdWithOwner(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        return storeMapper.toResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreSummaryResponse> listApproved(String wilaya, StoreType type, Pageable pageable) {
        return PageResponse.of(
                storeRepository.findApproved(wilaya, type, pageable)
                               .map(storeMapper::toSummary));
    }

    @Override
    @Transactional
    public StoreResponse updateStatus(UUID storeId, StoreStatus status) {
        Store store = storeRepository.findByIdWithOwner(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));

        StoreStatus previous = store.getStatus();
        store.setStatus(status);

        log.info("Store {} : {} → {} (admin)", storeId, previous, status);
        return storeMapper.toResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StoreSummaryResponse> listAll(StoreStatus status, Pageable pageable) {
        return PageResponse.of(
                storeRepository.findAllForAdmin(status, pageable)
                               .map(storeMapper::toSummary));
    }
}
