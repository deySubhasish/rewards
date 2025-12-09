package com.program.rewards.repository;

import com.program.rewards.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.customerId = :customerId AND t.amount > :amount")
    List<Transaction> findByCustomerIdAndStatusAndAmountGreaterThan(
            @Param("customerId") Long customerId,
            @Param("status") String status, 
            @Param("amount") Double amount);
            
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.customerId = :customerId AND t.amount > :amount " +
           "AND t.transactionDate BETWEEN :startDate AND :endDate")
    List<Transaction> findByCustomerIdAndStatusAndAmountGreaterThanAndDateBetween(
            @Param("customerId") Long customerId,
            @Param("status") String status, 
            @Param("amount") Double amount,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
            
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.customerId = :customerId AND t.amount > :amount " +
           "AND t.transactionDate >= :startDate")
    List<Transaction> findByCustomerIdAndStatusAndAmountGreaterThanAndDateAfter(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("amount") Double amount,
            @Param("startDate") LocalDateTime startDate);
            
    @Query("SELECT t FROM Transaction t WHERE t.status = :status AND t.customerId = :customerId AND t.amount > :amount " +
           "AND t.transactionDate <= :endDate")
    List<Transaction> findByCustomerIdAndStatusAndAmountGreaterThanAndDateBefore(
            @Param("customerId") Long customerId,
            @Param("status") String status,
            @Param("amount") Double amount,
            @Param("endDate") LocalDateTime endDate);

}
