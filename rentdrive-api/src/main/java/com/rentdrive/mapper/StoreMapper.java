package com.rentdrive.mapper;

import com.rentdrive.dto.response.StoreResponse;
import com.rentdrive.dto.response.StoreSummaryResponse;
import com.rentdrive.entity.Profile;
import com.rentdrive.entity.Store;
import com.rentdrive.entity.User;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public StoreResponse toResponse(Store store) {
        User    owner   = store.getOwner();
        Profile profile = owner.getProfile();

        String fullName  = profile != null
                ? profile.getFirstName() + " " + profile.getLastName() : "—";
        String avatarUrl = profile != null ? profile.getAvatarUrl() : null;

        return new StoreResponse(
                store.getId(),
                store.getName(),
                store.getType(),
                store.getStatus(),
                store.getDescription(),
                store.getLogoUrl(),
                store.getPhone(),
                store.getAddress(),
                store.getCity(),
                store.getWilaya(),
                store.getRating(),
                store.getReviewCount(),
                new StoreResponse.OwnerInfo(owner.getId(), fullName, avatarUrl),
                store.getCreatedAt()
        );
    }

    public StoreSummaryResponse toSummary(Store store) {
        return new StoreSummaryResponse(
                store.getId(),
                store.getName(),
                store.getType(),
                store.getStatus(),
                store.getLogoUrl(),
                store.getWilaya(),
                store.getCity(),
                store.getRating(),
                store.getReviewCount()
        );
    }
}
