package com.placement.portal.offer;

import java.security.Principal;
import java.util.List;

public interface IOfferService {
    OfferDto issueOffer(IssueOfferDto dto);
    OfferDto issueOffer(IssueOfferDto dto, Principal principal);
    List<OfferDto> getAllOffers();
    OfferDto getOfferById(String offerId);
    OfferDto getOfferByApplicationId(String applicationId);
    List<OfferDto> getOffersByStudent(String studentId);
    OfferDto acceptOffer(String offerId, String studentId);
}
