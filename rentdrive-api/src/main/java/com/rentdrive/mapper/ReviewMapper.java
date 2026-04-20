package com.rentdrive.mapper;

import com.rentdrive.dto.response.ReviewResponse;
import com.rentdrive.entity.Profile;
import com.rentdrive.entity.Review;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    public ReviewResponse toResponse(Review r) {
        Profile profile = r.getAuthor().getProfile();
        String fullName = profile != null
                ? profile.getFirstName() + " " + profile.getLastName() : "—";
        return new ReviewResponse(
                r.getId(),
                r.getBooking().getId(),
                r.getType(),
                r.getRating(),
                r.getComment(),
                new ReviewResponse.AuthorInfo(r.getAuthor().getId(), fullName),
                r.getCreatedAt());
    }
}
