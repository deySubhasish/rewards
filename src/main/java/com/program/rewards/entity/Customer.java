package com.program.rewards.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "CUSTOMERS")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "join_date", nullable = false)
    private LocalDate joinDate;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = false)
    private String address;

    public Customer(String name, String email, LocalDate joinDate, String phone, String address) {
        this.name = name;
        this.email = email;
        this.joinDate = joinDate;
        this.phone = phone;
        this.address = address;
    }
}
