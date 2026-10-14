package rw.coopconnect.service;

import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rw.coopconnect.domain.Order;
import rw.coopconnect.domain.OrderItem;
import rw.coopconnect.repository.OrderRepository;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FR-12: Cooperative reporting and CSV export.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final OrderRepository orderRepository;

    public byte[] exportOrdersToCsv(LocalDateTime start, LocalDateTime end) {
        List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {

            // Header row
            writer.writeNext(new String[]{
                    "Order Number",
                    "Buyer Name",
                    "Buyer Phone",
                    "Status",
                    "Total Amount (RWF)",
                    "Items Summary",
                    "Created Date"
            });

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Order order : orders) {
                StringBuilder itemsSummary = new StringBuilder();
                for (OrderItem item : order.getItems()) {
                    if (!itemsSummary.isEmpty()) itemsSummary.append("; ");
                    itemsSummary.append(item.getListing().getTitle())
                            .append(" (")
                            .append(item.getQuantity())
                            .append(" ")
                            .append(item.getListing().getUnit())
                            .append(")");
                }

                writer.writeNext(new String[]{
                        order.getOrderNumber(),
                        order.getBuyer().getFullName(),
                        order.getBuyer().getPhone(),
                        order.getStatus().name(),
                        order.getTotalAmount().toPlainString(),
                        itemsSummary.toString(),
                        order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : ""
                });
            }

            writer.flush();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate CSV export", e);
            throw new RuntimeException("CSV export generation failed: " + e.getMessage(), e);
        }
    }
}
