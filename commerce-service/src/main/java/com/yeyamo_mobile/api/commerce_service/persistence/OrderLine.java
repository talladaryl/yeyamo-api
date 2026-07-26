package com.yeyamo_mobile.api.commerce_service.persistence;
import jakarta.persistence.*;import java.math.*;import java.util.*;
@Entity @Table(name="commerce_order_lines")public class OrderLine{@Id public UUID id;@Column(name="order_id")public UUID orderId;@Column(name="product_id")public String productId;public String description;public int quantity;@Column(name="unit_price")public BigDecimal unitPrice;@Column(name="line_total")public BigDecimal lineTotal;@Column(name="price_snapshot")public String priceSnapshot;}
