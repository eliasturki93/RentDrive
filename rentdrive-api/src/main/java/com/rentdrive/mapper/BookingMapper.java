package com.rentdrive.mapper;

import com.rentdrive.dto.response.BookingResponse;
import com.rentdrive.dto.response.BookingSummaryResponse;
import com.rentdrive.entity.Booking;
import com.rentdrive.entity.Payment;
import com.rentdrive.entity.VehiclePhoto;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper {

    public BookingResponse toResponse(Booking b) {
        var v       = b.getVehicle();
        var renter  = b.getRenter();
        var store   = b.getStore();
        var profile = renter.getProfile();
        var payment = b.getPayment();

        String primaryPhoto = v.getPhotos().stream()
                .filter(VehiclePhoto::isPrimary).map(VehiclePhoto::getUrl)
                .findFirst()
                .orElse(v.getPhotos().isEmpty() ? null : v.getPhotos().get(0).getUrl());

        String renterName = profile != null
                ? profile.getFirstName() + " " + profile.getLastName() : "—";

        BookingResponse.PaymentInfo paymentInfo = payment == null ? null :
                new BookingResponse.PaymentInfo(
                        payment.getId(), payment.getMethod(), payment.getStatus(),
                        payment.getDepositStatus(), payment.getTransactionRef());

        return new BookingResponse(
                b.getId(), b.getStatus(),
                b.getStartDate(), b.getEndDate(), b.getTotalDays(),
                b.getPricePerDay(), b.getSubtotal(), b.getCommission(),
                b.getDepositAmount(), b.getTotalAmount(),
                b.getCancellationReason(),
                b.getConfirmedAt(), b.getStartedAt(), b.getCompletedAt(), b.getCreatedAt(),
                new BookingResponse.VehicleInfo(v.getId(), v.getBrand(), v.getModel(), v.getYear(), primaryPhoto),
                new BookingResponse.RenterInfo(renter.getId(), renterName, renter.getPhone()),
                new BookingResponse.StoreInfo(store.getId(), store.getName(), store.getPhone()),
                paymentInfo);
    }

    public BookingSummaryResponse toSummary(Booking b) {
        var v   = b.getVehicle();
        var profile = b.getRenter().getProfile();

        String primaryPhoto = v.getPhotos().stream()
                .filter(VehiclePhoto::isPrimary).map(VehiclePhoto::getUrl)
                .findFirst()
                .orElse(v.getPhotos().isEmpty() ? null : v.getPhotos().get(0).getUrl());

        return new BookingSummaryResponse(
                b.getId(), b.getStatus(),
                b.getStartDate(), b.getEndDate(), b.getTotalAmount(), b.getCreatedAt(),
                v.getBrand() + " " + v.getModel() + " " + v.getYear(),
                primaryPhoto,
                profile != null ? profile.getFirstName() + " " + profile.getLastName() : "—",
                b.getStore().getName());
    }
}
