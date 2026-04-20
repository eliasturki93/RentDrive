package com.rentdrive.service.impl;

import com.rentdrive.dto.request.AddPhotoRequest;
import com.rentdrive.dto.request.CreateVehicleRequest;
import com.rentdrive.dto.request.UpdateVehicleRequest;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.dto.response.VehicleResponse;
import com.rentdrive.dto.response.VehicleSummaryResponse;
import com.rentdrive.entity.Store;
import com.rentdrive.entity.Vehicle;
import com.rentdrive.entity.VehiclePhoto;
import com.rentdrive.enums.FuelType;
import com.rentdrive.enums.StoreStatus;
import com.rentdrive.enums.Transmission;
import com.rentdrive.enums.VehicleCategory;
import com.rentdrive.enums.VehicleStatus;
import com.rentdrive.exception.ConflictException;
import com.rentdrive.exception.ForbiddenException;
import com.rentdrive.exception.ResourceNotFoundException;
import com.rentdrive.exception.ValidationException;
import com.rentdrive.mapper.VehicleMapper;
import com.rentdrive.repository.StoreRepository;
import com.rentdrive.repository.VehiclePhotoRepository;
import com.rentdrive.repository.VehicleRepository;
import com.rentdrive.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private static final int MAX_PHOTOS = 10;

    private final VehicleRepository      vehicleRepository;
    private final VehiclePhotoRepository photoRepository;
    private final StoreRepository        storeRepository;
    private final VehicleMapper          vehicleMapper;

    @Override
    @Transactional
    public VehicleResponse create(UUID ownerId, CreateVehicleRequest req) {
        Store store = storeRepository.findByOwnerIdWithOwner(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "owner=" + ownerId));

        if (store.getStatus() != StoreStatus.APPROVED) {
            throw new ForbiddenException(
                    "Votre store doit être validé par un admin avant d'ajouter des véhicules.");
        }

        Vehicle v = new Vehicle();
        v.setStore(store);
        v.setBrand(req.brand().trim());
        v.setModel(req.model().trim());
        v.setYear(req.year());
        v.setCategory(req.category());
        v.setTransmission(req.transmission());
        v.setFuelType(req.fuelType());
        v.setSeats(req.seats());
        v.setMileage(req.mileage());
        v.setPricePerDay(req.pricePerDay());
        v.setPricePerWeek(req.pricePerWeek());
        v.setDepositAmount(req.depositAmount() != null ? req.depositAmount() : BigDecimal.ZERO);
        v.setFeatures(req.features());
        v.setDescription(req.description());
        v.setWilaya(req.wilaya());
        v.setStatus(VehicleStatus.PENDING_REVIEW);

        Vehicle saved = vehicleRepository.save(v);
        log.info("Véhicule créé : {} (store={})", saved.getId(), store.getId());
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleSummaryResponse> getMyVehicles(UUID ownerId) {
        Store store = storeRepository.findByOwnerIdWithOwner(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "owner=" + ownerId));
        return vehicleRepository.findByStoreId(store.getId())
                .stream().map(vehicleMapper::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getById(UUID vehicleId) {
        return vehicleMapper.toResponse(findWithDetails(vehicleId));
    }

    @Override
    @Transactional
    public VehicleResponse update(UUID ownerId, UUID vehicleId, UpdateVehicleRequest req) {
        checkOwnership(ownerId, vehicleId);
        Vehicle v = findWithDetails(vehicleId);

        if (StringUtils.hasText(req.brand()))       v.setBrand(req.brand().trim());
        if (StringUtils.hasText(req.model()))       v.setModel(req.model().trim());
        if (req.year() != null)                     v.setYear(req.year());
        if (req.category() != null)                 v.setCategory(req.category());
        if (req.transmission() != null)             v.setTransmission(req.transmission());
        if (req.fuelType() != null)                 v.setFuelType(req.fuelType());
        if (req.seats() != null)                    v.setSeats(req.seats());
        if (req.mileage() != null)                  v.setMileage(req.mileage());
        if (req.pricePerDay() != null)              v.setPricePerDay(req.pricePerDay());
        if (req.pricePerWeek() != null)             v.setPricePerWeek(req.pricePerWeek());
        if (req.depositAmount() != null)            v.setDepositAmount(req.depositAmount());
        if (req.features() != null)                 v.setFeatures(req.features());
        if (StringUtils.hasText(req.description())) v.setDescription(req.description());
        if (StringUtils.hasText(req.wilaya()))      v.setWilaya(req.wilaya());

        log.info("Véhicule {} mis à jour", vehicleId);
        return vehicleMapper.toResponse(v);
    }

    @Override
    @Transactional
    public void delete(UUID ownerId, UUID vehicleId) {
        checkOwnership(ownerId, vehicleId);
        Vehicle v = findWithDetails(vehicleId);

        if (v.getStatus() == VehicleStatus.RENTED) {
            throw new ConflictException("Impossible de supprimer un véhicule en cours de location.");
        }

        vehicleRepository.delete(v);
        log.info("Véhicule {} supprimé", vehicleId);
    }

    @Override
    @Transactional
    public VehicleResponse addPhoto(UUID ownerId, UUID vehicleId, AddPhotoRequest req) {
        checkOwnership(ownerId, vehicleId);
        Vehicle v = findWithDetails(vehicleId);

        if (photoRepository.countByVehicleId(vehicleId) >= MAX_PHOTOS) {
            throw new ValidationException(
                    "Maximum " + MAX_PHOTOS + " photos par véhicule.",
                    Map.of("photos", "Limite atteinte"));
        }

        if (req.isPrimary()) {
            photoRepository.clearPrimaryFlag(vehicleId);
        }

        VehiclePhoto photo = new VehiclePhoto();
        photo.setVehicle(v);
        photo.setUrl(req.url());
        photo.setPrimary(req.isPrimary());
        photo.setOrderIndex(req.orderIndex() != null ? req.orderIndex() : (byte) v.getPhotos().size());

        photoRepository.save(photo);

        // Recharger pour avoir la liste à jour
        return vehicleMapper.toResponse(findWithDetails(vehicleId));
    }

    @Override
    @Transactional
    public VehicleResponse deletePhoto(UUID ownerId, UUID vehicleId, UUID photoId) {
        checkOwnership(ownerId, vehicleId);

        VehiclePhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo", photoId));

        if (!photo.getVehicle().getId().equals(vehicleId)) {
            throw new ForbiddenException("Cette photo n'appartient pas à ce véhicule.");
        }

        photoRepository.delete(photo);
        log.info("Photo {} supprimée du véhicule {}", photoId, vehicleId);
        return vehicleMapper.toResponse(findWithDetails(vehicleId));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VehicleSummaryResponse> search(
            String wilaya, VehicleCategory category, Transmission transmission,
            FuelType fuelType, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {

        return PageResponse.of(
                vehicleRepository.findAvailable(wilaya, category, transmission,
                                                fuelType, minPrice, maxPrice, pageable)
                                 .map(vehicleMapper::toSummary));
    }

    @Override
    @Transactional
    public VehicleResponse updateStatus(UUID vehicleId, VehicleStatus status) {
        Vehicle v = findWithDetails(vehicleId);
        VehicleStatus previous = v.getStatus();
        v.setStatus(status);
        log.info("Véhicule {} : {} → {} (admin)", vehicleId, previous, status);
        return vehicleMapper.toResponse(v);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VehicleSummaryResponse> listAll(VehicleStatus status, Pageable pageable) {
        return PageResponse.of(
                vehicleRepository.findAllForAdmin(status, pageable)
                                 .map(vehicleMapper::toSummary));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Vehicle findWithDetails(UUID id) {
        return vehicleRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", id));
    }

    private void checkOwnership(UUID ownerId, UUID vehicleId) {
        if (!vehicleRepository.existsByIdAndStoreOwnerId(vehicleId, ownerId)) {
            throw new ForbiddenException("Vous n'êtes pas propriétaire de ce véhicule.");
        }
    }
}
