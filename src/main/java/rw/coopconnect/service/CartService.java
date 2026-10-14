package rw.coopconnect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.coopconnect.domain.Cart;
import rw.coopconnect.domain.CartItem;
import rw.coopconnect.domain.ProductListing;
import rw.coopconnect.domain.User;
import rw.coopconnect.exception.InsufficientStockException;
import rw.coopconnect.exception.ResourceNotFoundException;
import rw.coopconnect.repository.CartItemRepository;
import rw.coopconnect.repository.CartRepository;
import rw.coopconnect.repository.ProductListingRepository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * FR-06: Shopping cart management for buyers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductListingRepository listingRepository;

    @Transactional
    public Cart getOrCreateCart(User buyer) {
        return cartRepository.findByBuyerId(buyer.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().buyer(buyer).build();
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public void addItemToCart(User buyer, Long listingId, BigDecimal quantity) {
        Cart cart = getOrCreateCart(buyer);
        ProductListing listing = listingRepository.findById(listingId)
                .orElseThrow(() -> new ResourceNotFoundException("Product listing not found with ID: " + listingId));

        if (!listing.isActive()) {
            throw new IllegalStateException("Product listing is no longer active.");
        }

        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndListingId(cart.getId(), listingId);

        BigDecimal newTotalQty = quantity;
        if (existingItemOpt.isPresent()) {
            newTotalQty = existingItemOpt.get().getQuantity().add(quantity);
        }

        if (newTotalQty.compareTo(listing.getAvailableQuantity()) > 0) {
            throw new InsufficientStockException("Requested quantity (" + newTotalQty + " " + listing.getUnit() +
                    ") exceeds available stock (" + listing.getAvailableQuantity() + " " + listing.getUnit() + ").");
        }

        if (existingItemOpt.isPresent()) {
            CartItem item = existingItemOpt.get();
            item.setQuantity(newTotalQty);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .listing(listing)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(newItem);
        }
        log.info("Buyer {} added {} of listing ID {} to cart", buyer.getEmail(), quantity, listingId);
    }

    @Transactional
    public void updateItemQuantity(User buyer, Long cartItemId, BigDecimal quantity) {
        Cart cart = getOrCreateCart(buyer);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new IllegalArgumentException("Cart item does not belong to buyer's cart.");
        }

        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            cartItemRepository.delete(item);
            return;
        }

        if (quantity.compareTo(item.getListing().getAvailableQuantity()) > 0) {
            throw new InsufficientStockException("Quantity (" + quantity + ") exceeds available stock (" +
                    item.getListing().getAvailableQuantity() + ").");
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }

    @Transactional
    public void removeItemFromCart(User buyer, Long cartItemId) {
        Cart cart = getOrCreateCart(buyer);
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found with ID: " + cartItemId));

        if (item.getCart().getId().equals(cart.getId())) {
            cartItemRepository.delete(item);
        }
    }

    @Transactional
    public void clearCart(User buyer) {
        Cart cart = getOrCreateCart(buyer);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    public BigDecimal calculateCartTotal(Cart cart) {
        return cart.getItems().stream()
                .map(item -> item.getListing().getPricePerUnit().multiply(item.getQuantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
