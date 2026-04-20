package com.rentdrive.service.impl;

import com.rentdrive.dto.request.CancelBookingRequest;
import com.rentdrive.dto.request.CreateBookingRequest;
import com.rentdrive.dto.response.BookingResponse;
import com.rentdrive.dto.response.BookingSummaryResponse;
import com.rentdrive.dto.response.PageResponse;
import com.rentdrive.entity.Booking;
import com.rentdrive.entity.Payment;
import com.rentdrive.entity.Store;
import com.rentdrive.entity.User;
import com.rentdrive.entity.Vehicle;
import com.rentdrive.enums.BookingStatus;
import com.rentdrive.enums.DepositStatus;
import com.rentdrive.enums.PaymentStatus;
import com.rentdrive.enums.StoreType;
import com.rentdrive.enums.VehicleStatus;
import com.rentdrive.exception.ConflictException;
import com.rentdrive.exception.ForbiddenException;
import com.rentdrive.exception.ResourceNotFoundException;
import com.rentdrive.exception.ValidationException;
import com.rentdrive.mapper.BookingMapper;
import com.rentdrive.repository.BookingRepository;
import com.rentdrive.repository.StoreRepository;
import com.rentdrive.repository.UserRepository;
import com.rentdrive.repository.VehicleRepository;
import com.rentdrive.service.BookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.rentdrive.enums.BookingStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final BigDecimal COMMISSION_PRIVATE = new BigDecimal("0.10");
    private static final BigDecimal COMMISSION_AGENCY  = new BigDecimal("0.14");

    private final BookingRepository bookingRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository    userRepository;
    private final StoreRepository   storeRepository;
    private final BookingMapper     bookingMapper;

    @Override
    @Transactional
    public BookingResponse create(UUID renterId, CreateBookingRequest req) {
        // Validation des dates
        if (!req.endDate().isAfter(req.startDate())) {
            throw new ValidationException(
                    "La date de fin doit être après la date de début.",
                    Map.of("endDate", "Doit être postérieure à startDate"));
        }

        Vehicle vehicle = vehicleRepository.findByIdWithDetails(req.vehicleId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle", req.vehicleId()));

        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new ConflictException("Ce véhicule n'est pas disponible à la location.");
        }

        // Vérification de chevauchement (utilise l'index composite)
        boolean overlap = bookingRepository.hasOverlap(
                req.vehicleId(), req.startDate(), req.endDate(),
                List.of(PENDING, CONFIRMED, IN_PROGRESS));
        if (overlap) {
            throw new ConflictException(
                    "Le véhicule est déjà réservé sur cette période.");
        }

        User  renter = userRepository.findById(renterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", renterId));
        Store store  = vehicle.getStore();

        // Calcul financier
        long totalDays = req.startDate().until(req.endDate()).getDays();
        if (totalDays < 1) totalDays = 1;

        BigDecimal pricePerDay = vehicle.getPricePerDay();
        BigDecimal subtotal    = pricePerDay.multiply(BigDecimal.valueOf(totalDays));
        BigDecimal commRate    = store.getType() == StoreType.AGENCY
                ? COMMISSION_AGENCY : COMMISSION_PRIVATE;
        BigDecimal commission  = subtotal.multiply(commRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal deposit     = vehicle.getDepositAmount();
        BigDecimal total       = subtotal.add(commission).add(deposit);

        // Booking
        Booking booking = new Booking();
        booking.setVehicle(vehicle);
        booking.setRenter(renter);
        booking.setStore(store);
        booking.setStartDate(req.startDate());
        booking.setEndDate(req.endDate());
        booking.setTotalDays((short) totalDays);
        booking.setPricePerDay(pricePerDay);
        booking.setSubtotal(subtotal);
        booking.setCommission(commission);
        booking.setDepositAmount(deposit);
        booking.setTotalAmount(total);
        booking.setStatus(PENDING);

        // Payment initial
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(total);
        payment.setMethod(req.paymentMethod());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setDepositStatus(DepositStatus.HELD);
        booking.setPayment(payment);

        Booking saved = bookingRepository.save(booking);
        log.info("Réservation créée : {} (vehicle={}, renter={})",
                saved.getId(), req.vehicleId(), renterId);

        return bookingMapper.toResponse(findWithDetails(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getById(UUID callerId, UUID bookingId) {
        Booking booking = findWithDetails(bookingId);
        boolean isRenter = booking.getRenter().getId().equals(callerId);
        boolean isOwner  = bookingRepository.existsByIdAndStoreOwnerId(bookingId, callerId);
        if (!isRenter && !isOwner) {
            throw new ForbiddenException("Accès non autorisé à cette réservation.");
        }
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingSummaryResponse> getMyBookings(UUID renterId, Pageable pageable) {
        return PageResponse.of(
                bookingRepository.findByRenterId(renterId, pageable)
                                 .map(bookingMapper::toSummary));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingSummaryResponse> getStoreBookings(UUID ownerId,
                                                                  BookingStatus status,
                                                                  Pageable pageable) {
        Store store = getStoreForOwner(ownerId);
        return PageResponse.of(
                bookingRepository.findByStoreId(store.getId(), status, pageable)
                                 .map(bookingMapper::toSummary));
    }

    @Override
    @Transactional
    public BookingResponse confirm(UUID ownerId, UUID bookingId) {
        checkStoreOwnership(ownerId, bookingId);
        Booking booking = findWithDetails(bookingId);
        requireStatus(booking, PENDING);

        booking.setStatus(CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        log.info("Réservation {} confirmée", bookingId);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse cancel(UUID callerId, UUID bookingId, CancelBookingRequest req) {
        Booking booking = findWithDetails(bookingId);

        boolean isRenter = bookingRepository.existsByIdAndRenterId(bookingId, callerId);
        boolean isOwner  = bookingRepository.existsByIdAndStoreOwnerId(bookingId, callerId);
        if (!isRenter && !isOwner) {
            throw new ForbiddenException("Vous n'êtes pas autorisé à annuler cette réservation.");
        }

        if (booking.getStatus() == IN_PROGRESS || booking.getStatus() == COMPLETED) {
            throw new ConflictException(
                    "Impossible d'annuler une réservation en cours ou terminée.");
        }
        if (booking.getStatus() == CANCELLED) {
            throw new ConflictException("Cette réservation est déjà annulée.");
        }

        booking.setStatus(CANCELLED);
        booking.setCancellationReason(req != null ? req.reason() : null);

        if (booking.getPayment() != null) {
            booking.getPayment().setStatus(PaymentStatus.REFUNDED);
            booking.getPayment().setDepositStatus(DepositStatus.RELEASED);
        }

        log.info("Réservation {} annulée par {}", bookingId, callerId);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse start(UUID ownerId, UUID bookingId) {
        checkStoreOwnership(ownerId, bookingId);
        Booking booking = findWithDetails(bookingId);
        requireStatus(booking, CONFIRMED);

        booking.setStatus(IN_PROGRESS);
        booking.setStartedAt(LocalDateTime.now());
        booking.getVehicle().setStatus(VehicleStatus.RENTED);

        log.info("Réservation {} démarrée", bookingId);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional
    public BookingResponse complete(UUID ownerId, UUID bookingId) {
        checkStoreOwnership(ownerId, bookingId);
        Booking booking = findWithDetails(bookingId);
        requireStatus(booking, IN_PROGRESS);

        booking.setStatus(COMPLETED);
        booking.setCompletedAt(LocalDateTime.now());
        booking.getVehicle().setStatus(VehicleStatus.AVAILABLE);

        Payment payment = booking.getPayment();
        if (payment != null) {
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setDepositStatus(DepositStatus.RELEASED);
        }

        log.info("Réservation {} terminée", bookingId);
        return bookingMapper.toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<BookingSummaryResponse> listAll(BookingStatus status, Pageable pageable) {
        return PageResponse.of(
                bookingRepository.findAllForAdmin(status, pageable)
                                 .map(bookingMapper::toSummary));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Booking findWithDetails(UUID id) {
        return bookingRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private Store getStoreForOwner(UUID ownerId) {
        return storeRepository.findByOwnerIdWithOwner(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "owner=" + ownerId));
    }

    private void checkStoreOwnership(UUID ownerId, UUID bookingId) {
        if (!bookingRepository.existsByIdAndStoreOwnerId(bookingId, ownerId)) {
            throw new ForbiddenException("Cette réservation n'appartient pas à votre store.");
        }
    }

    private void requireStatus(Booking booking, BookingStatus expected) {
        if (booking.getStatus() != expected) {
            throw new ConflictException(String.format(
                    "Action impossible : statut actuel '%s', attendu '%s'.",
                    booking.getStatus(), expected));
        }
    }
}
