package com.vtcorp.store.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.List;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "order")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

    @Id
    private String orderId;
    @JsonFormat(pattern = "dd-MM-yyyy")
    private Date createdDate;
    private String status;
    private String cusName;
    private String cusMail;
    private String cusPhone;
    private String cusCity;
    private String cusDistrict;
    private String cusWard;
    private String cusStreet;
    private Integer totalQuantity;
    private Double totalPrice;
    private Double postDiscountPrice;
    private Integer totalPoint;
    private String trackingCode;

    @ManyToOne
    @JoinColumn(name = "fk_username")
    private User user;

    @ManyToOne
    @JoinColumn(name = "fk_voucher_id")
    private Voucher voucher;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderDetail> orderDetails;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GiftIncluding> giftIncludings;

}
