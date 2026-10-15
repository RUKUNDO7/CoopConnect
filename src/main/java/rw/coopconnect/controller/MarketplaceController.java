package rw.coopconnect.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import rw.coopconnect.domain.ProductListing;
import rw.coopconnect.repository.CooperativeRepository;
import rw.coopconnect.repository.ProductCategoryRepository;
import rw.coopconnect.service.ProductListingService;

import java.math.BigDecimal;

/**
 * FR-04, FR-05, FR-11: Public Marketplace browsing, searching, produce detail, and Price Index.
 * NFR-02: Paginated results for fast response.
 */
@Controller
@RequiredArgsConstructor
public class MarketplaceController {

    private final ProductListingService listingService;
    private final ProductCategoryRepository categoryRepository;
    private final CooperativeRepository cooperativeRepository;

    @GetMapping("/products")
    public String listProducts(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "district", required = false) String district,
            @RequestParam(value = "cooperativeId", required = false) Long cooperativeId,
            @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
            @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "12") int size,
            Model model) {

        String trimmedDistrict = (district != null && !district.trim().isEmpty()) ? district.trim() : null;
        String trimmedSearch = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        Page<ProductListing> listingPage = listingService.searchListings(
                categoryId, trimmedDistrict, cooperativeId, minPrice, maxPrice, trimmedSearch,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        model.addAttribute("listings", listingPage.getContent());
        model.addAttribute("page", listingPage);
        model.addAttribute("categories", categoryRepository.findAll());
        model.addAttribute("cooperatives", cooperativeRepository.findAll());

        // Echo current filters
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedDistrict", trimmedDistrict);
        model.addAttribute("selectedCooperativeId", cooperativeId);
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("search", trimmedSearch);

        return "marketplace/list";
    }

    @GetMapping("/products/{id}")
    public String viewProduct(@PathVariable("id") Long id, Model model) {
        ProductListing listing = listingService.getListingById(id);
        model.addAttribute("listing", listing);
        return "marketplace/detail";
    }

    @GetMapping("/price-dashboard")
    public String priceDashboard(Model model) {
        model.addAttribute("priceStats", listingService.getPriceDashboardData());
        return "marketplace/price-dashboard";
    }
}
