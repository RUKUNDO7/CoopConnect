package rw.coopconnect.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.*;
import rw.coopconnect.domain.enums.*;
import rw.coopconnect.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Seeds initial demo data for Rwanda agricultural cooperatives, categories, and test user personas.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CooperativeRepository cooperativeRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final ProductCategoryRepository categoryRepository;
    private final HarvestRepository harvestRepository;
    private final ProductListingRepository listingRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data, skipping seeder.");
            return;
        }

        log.info("Starting CoopConnect initial data seeding...");

        String defaultHashedPassword = passwordEncoder.encode("password123");

        // 1. Admin
        User admin = User.builder()
                .email("admin@coopconnect.rw")
                .passwordHash(defaultHashedPassword)
                .fullName("System Administrator")
                .phone("0788111111")
                .role(Role.ADMIN)
                .enabled(true)
                .build();
        userRepository.save(admin);

        // 2. Cooperative Managers
        User managerMusanze = User.builder()
                .email("manager@musanze.rw")
                .passwordHash(defaultHashedPassword)
                .fullName("Kamanzi Patrick")
                .phone("0788222222")
                .role(Role.COOPERATIVE_MANAGER)
                .enabled(true)
                .build();
        userRepository.save(managerMusanze);

        User managerHuye = User.builder()
                .email("manager@huye.rw")
                .passwordHash(defaultHashedPassword)
                .fullName("Mukamugema Chantal")
                .phone("0788333333")
                .role(Role.COOPERATIVE_MANAGER)
                .enabled(true)
                .build();
        userRepository.save(managerHuye);

        // 3. Cooperatives
        Cooperative coopMusanze = Cooperative.builder()
                .name("Musanze Potato Farmers Cooperative (COAPOV)")
                .district("Musanze")
                .sector("Kinigi")
                .description("Specialized in volcanic soil Irish potatoes and organic vegetables in Northern Rwanda.")
                .manager(managerMusanze)
                .build();
        cooperativeRepository.save(coopMusanze);

        Cooperative coopHuye = Cooperative.builder()
                .name("Huye Grain & Coffee Cooperative (KOPAKAMA)")
                .district("Huye")
                .sector("Ngoma")
                .description("Leading producer of high-altitude Arabica coffee and premium quality dry beans.")
                .manager(managerHuye)
                .build();
        cooperativeRepository.save(coopHuye);

        Cooperative coopNyagatare = Cooperative.builder()
                .name("Nyagatare Maize & Dairy Cooperative (COVANYA)")
                .district("Nyagatare")
                .sector("Mimuri")
                .description("Eastern province grain producers cooperative producing premium yellow and white maize.")
                .build();
        cooperativeRepository.save(coopNyagatare);

        // 4. Farmers
        User farmerJean = User.builder()
                .email("farmer.jean@coopconnect.rw")
                .passwordHash(defaultHashedPassword)
                .fullName("Jean Baptiste Bizimana")
                .phone("0788444444")
                .role(Role.FARMER)
                .enabled(true)
                .build();
        userRepository.save(farmerJean);

        FarmerProfile profileJean = FarmerProfile.builder()
                .user(farmerJean)
                .nationalId("1198580012345678")
                .district("Musanze")
                .sector("Kinigi")
                .cooperative(coopMusanze)
                .approvalStatus(ApprovalStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
                .approvedBy(managerMusanze)
                .build();
        farmerProfileRepository.save(profileJean);

        User farmerMarie = User.builder()
                .email("farmer.marie@coopconnect.rw")
                .passwordHash(defaultHashedPassword)
                .fullName("Marie Claire Uwase")
                .phone("0788555555")
                .role(Role.FARMER)
                .enabled(true)
                .build();
        userRepository.save(farmerMarie);

        FarmerProfile profileMarie = FarmerProfile.builder()
                .user(farmerMarie)
                .nationalId("1199080098765432")
                .district("Huye")
                .sector("Ngoma")
                .cooperative(coopHuye)
                .approvalStatus(ApprovalStatus.APPROVED)
                .approvedAt(LocalDateTime.now())
                .approvedBy(managerHuye)
                .build();
        farmerProfileRepository.save(profileMarie);

        User farmerPending = User.builder()
                .email("farmer.claudine@coopconnect.rw")
                .passwordHash(defaultHashedPassword)
                .fullName("Claudine Mugiraneza")
                .phone("0788666666")
                .role(Role.FARMER)
                .enabled(true)
                .build();
        userRepository.save(farmerPending);

        FarmerProfile profilePending = FarmerProfile.builder()
                .user(farmerPending)
                .nationalId("1199580055443322")
                .district("Musanze")
                .sector("Kinigi")
                .cooperative(coopMusanze)
                .approvalStatus(ApprovalStatus.PENDING)
                .build();
        farmerProfileRepository.save(profilePending);

        // 5. Buyer
        User buyer = User.builder()
                .email("buyer@kigali.rw")
                .passwordHash(defaultHashedPassword)
                .fullName("Eric Habimana (FreshMart Kigali)")
                .phone("0788777777")
                .role(Role.BUYER)
                .enabled(true)
                .build();
        userRepository.save(buyer);

        Cart buyerCart = Cart.builder().buyer(buyer).build();
        cartRepository.save(buyerCart);

        // 6. Transporter
        User transporter = User.builder()
                .email("transporter@express.rw")
                .passwordHash(defaultHashedPassword)
                .fullName("Rwanda Logistics Express (Alexis Mugabo)")
                .phone("0788888888")
                .role(Role.TRANSPORTER)
                .enabled(true)
                .build();
        userRepository.save(transporter);

        // 7. Product Categories
        ProductCategory catPotatoes = ProductCategory.builder()
                .name("Irish Potatoes (Ibirayi)")
                .nameRw("Ibirayi bya Kinigi")
                .description("Kinigi, Victoria, and Peka varieties grown in the rich volcanic soil of Northern Rwanda.")
                .build();
        categoryRepository.save(catPotatoes);

        ProductCategory catMaize = ProductCategory.builder()
                .name("Maize / Corn (Ibigori)")
                .nameRw("Ibigori")
                .description("Sun-dried, Grade A whole grain maize ideal for flour milling and animal feed.")
                .build();
        categoryRepository.save(catMaize);

        ProductCategory catBeans = ProductCategory.builder()
                .name("Climbing Beans (Ibishyimbo)")
                .nameRw("Ibishyimbo")
                .description("Nutritious and protein-rich Rwandan climbing and bush beans.")
                .build();
        categoryRepository.save(catBeans);

        ProductCategory catCoffee = ProductCategory.builder()
                .name("Arabica Specialty Coffee (Ikawa)")
                .nameRw("Ikawa y'U Rwanda")
                .description("Hand-picked specialty parchment and green coffee beans.")
                .build();
        categoryRepository.save(catCoffee);

        ProductCategory catBananas = ProductCategory.builder()
                .name("Cooking Bananas (Ibitoki)")
                .nameRw("Ibitoki")
                .description("Fresh green plantains and cooking bananas harvested directly from highland farms.")
                .build();
        categoryRepository.save(catBananas);

        // 8. Harvests
        Harvest harvest1 = Harvest.builder()
                .farmer(farmerJean)
                .category(catPotatoes)
                .quantity(new BigDecimal("3500.00"))
                .unit("kg")
                .expectedDate(LocalDate.now().plusDays(2))
                .qualityGrade(QualityGrade.GRADE_A)
                .status(HarvestStatus.VERIFIED)
                .verifiedBy(managerMusanze)
                .build();
        harvestRepository.save(harvest1);

        Harvest harvest2 = Harvest.builder()
                .farmer(farmerMarie)
                .category(catBeans)
                .quantity(new BigDecimal("1200.00"))
                .unit("kg")
                .expectedDate(LocalDate.now().plusDays(5))
                .qualityGrade(QualityGrade.GRADE_A)
                .status(HarvestStatus.VERIFIED)
                .verifiedBy(managerHuye)
                .build();
        harvestRepository.save(harvest2);

        Harvest harvest3 = Harvest.builder()
                .farmer(farmerMarie)
                .category(catCoffee)
                .quantity(new BigDecimal("800.00"))
                .unit("kg")
                .expectedDate(LocalDate.now().plusDays(7))
                .qualityGrade(QualityGrade.GRADE_A)
                .status(HarvestStatus.VERIFIED)
                .verifiedBy(managerHuye)
                .build();
        harvestRepository.save(harvest3);

        Harvest harvest4 = Harvest.builder()
                .farmer(farmerJean)
                .category(catPotatoes)
                .quantity(new BigDecimal("1500.00"))
                .unit("kg")
                .expectedDate(LocalDate.now().plusDays(10))
                .qualityGrade(QualityGrade.GRADE_B)
                .status(HarvestStatus.PENDING)
                .build();
        harvestRepository.save(harvest4);

        // 9. Published Marketplace Listings
        ProductListing listing1 = ProductListing.builder()
                .harvest(harvest1)
                .cooperative(coopMusanze)
                .title("Kinigi High-Grade Irish Potatoes")
                .pricePerUnit(new BigDecimal("480.00"))
                .currency("RWF")
                .availableQuantity(new BigDecimal("3200.00"))
                .unit("kg")
                .photoUrl("/img/potatoes.jpg")
                .active(true)
                .build();
        listingRepository.save(listing1);

        ProductListing listing2 = ProductListing.builder()
                .harvest(harvest2)
                .cooperative(coopHuye)
                .title("Premium Red Climbing Beans (Umutuku)")
                .pricePerUnit(new BigDecimal("950.00"))
                .currency("RWF")
                .availableQuantity(new BigDecimal("1150.00"))
                .unit("kg")
                .photoUrl("/img/beans.jpg")
                .active(true)
                .build();
        listingRepository.save(listing2);

        ProductListing listing3 = ProductListing.builder()
                .harvest(harvest3)
                .cooperative(coopHuye)
                .title("Huye Bourbon Arabica Parchment Coffee")
                .pricePerUnit(new BigDecimal("3400.00"))
                .currency("RWF")
                .availableQuantity(new BigDecimal("750.00"))
                .unit("kg")
                .photoUrl("/img/coffee.jpg")
                .active(true)
                .build();
        listingRepository.save(listing3);

        log.info("CoopConnect data seeding completed successfully with personas:");
        log.info("Admin: admin@coopconnect.rw / password123");
        log.info("Manager Musanze: manager@musanze.rw / password123");
        log.info("Manager Huye: manager@huye.rw / password123");
        log.info("Farmer Jean: farmer.jean@coopconnect.rw / password123");
        log.info("Buyer: buyer@kigali.rw / password123");
        log.info("Transporter: transporter@express.rw / password123");
    }
}
