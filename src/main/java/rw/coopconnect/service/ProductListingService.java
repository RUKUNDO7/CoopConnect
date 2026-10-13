package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.Cooperative;
import rw.coopconnect.domain.Harvest;
import rw.coopconnect.domain.ProductListing;
import rw.coopconnect.domain.User;
import rw.coopconnect.domain.enums.HarvestStatus;
import rw.coopconnect.dto.request.ProductListingRequest;
import rw.coopconnect.dto.response.ProducePriceStat;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.HarvestRepository;
import rw.coopconnect.repository.ProductListingRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * FR-04, FR-05, FR-11: Produce listing management, search, and price indexing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductListingService {

    private final ProductListingRepository listingRepository;
    private final HarvestRepository harvestRepository;
    private final AuditLogService auditLogService;

    public Page<ProductListing> getActiveListings(Pageable pageable) {
        return listingRepository.findByActiveTrue(pageable);
    }

    public Page<ProductListing> searchListings(
            Long categoryId,
            String district,
            Long cooperativeId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String search,
            Pageable pageable) {
        return listingRepository.searchListings(categoryId, district, cooperativeId, minPrice, maxPrice, search, pageable);
    }

    public ProductListing getListingById(Long id) {
        return listingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product listing not found with ID: " + id));
    }

    public List<ProductListing> getListingsByCooperative(Long cooperativeId) {
        return listingRepository.findByCooperativeId(cooperativeId);
    }

    @Transactional
    public ProductListing createListing(Cooperative cooperative, User manager, ProductListingRequest request) {
        Harvest harvest = harvestRepository.findById(request.getHarvestId())
                .orElseThrow(() -> new ResourceNotFoundException("Harvest not found with ID: " + request.getHarvestId()));

        if (harvest.getStatus() != HarvestStatus.VERIFIED) {
            throw new IllegalStateException("Only verified harvests can be converted into marketplace listings.");
        }

        ProductListing listing = ProductListing.builder()
                .harvest(harvest)
                .cooperative(cooperative)
                .title(request.getTitle())
                .pricePerUnit(request.getPricePerUnit())
                .currency("RWF")
                .availableQuantity(request.getAvailableQuantity())
                .unit(request.getUnit())
                .photoUrl(request.getPhotoUrl())
                .active(request.isActive())
                .build();

        ProductListing saved = listingRepository.save(listing);

        auditLogService.logAction(
                manager,
                "CREATE_LISTING",
                "ProductListing",
                saved.getId(),
                "Published listing: " + saved.getTitle() + " at " + saved.getPricePerUnit() + " RWF/" + saved.getUnit()
        );
        log.info("Manager {} created listing ID {}", manager.getEmail(), saved.getId());
        return saved;
    }

    @Transactional
    public void toggleListingStatus(Long listingId, User manager) {
        ProductListing listing = getListingById(listingId);
        listing.setActive(!listing.isActive());
        listingRepository.save(listing);

        auditLogService.logAction(
                manager,
                "TOGGLE_LISTING_STATUS",
                "ProductListing",
                listing.getId(),
                "Listing " + listing.getId() + " active status set to: " + listing.isActive()
        );
    }

    public List<ProducePriceStat> getPriceDashboardData() {
        List<Object[]> rawStats = listingRepository.findAveragePriceByCropAndDistrict();
        List<ProducePriceStat> stats = new ArrayList<>();
        for (Object[] row : rawStats) {
            String crop = (String) row[0];
            String district = (String) row[1];
            Double avgPriceDouble = (Double) row[2];
            BigDecimal avgPrice = avgPriceDouble != null ?
                    BigDecimal.valueOf(avgPriceDouble).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            stats.add(new ProducePriceStat(crop, district, avgPrice));
        }
        return stats;
    }
}
