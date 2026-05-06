package com.travelplanningplatform.controller;

import com.travelplanningplatform.dto.AmountRequest;
import com.travelplanningplatform.dto.BudgetStatusResponse;
import com.travelplanningplatform.dto.BudgetUpdateResponse;
import com.travelplanningplatform.dto.BudgetOverviewDTO;
import com.travelplanningplatform.exception.ResourceNotFoundException;
import com.travelplanningplatform.repository.UserRepository;
import com.travelplanningplatform.service.BudgetTrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/budget")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetTrackingService budgetTrackingService;
    private final UserRepository userRepository;

    @GetMapping("/trip/{tripId}")
    public ResponseEntity<BudgetOverviewDTO> getBudgetOverview(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(budgetTrackingService.getBudgetOverview(tripId, getUserId(userDetails)));
    }

    @GetMapping("/summary")
    public ResponseEntity<List<BudgetOverviewDTO>> getBudgetSummary(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(budgetTrackingService.getAllTripsBudgetSummary(getUserId(userDetails)));
    }

    @PostMapping("/trip/{tripId}/spend")
    public ResponseEntity<BudgetUpdateResponse> updateBudgetSpent(
            @PathVariable Long tripId,
            @RequestBody AmountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = getUserId(userDetails);
        budgetTrackingService.updateBudgetSpent(tripId, userId, request.amount());

        BudgetOverviewDTO overview = budgetTrackingService.getBudgetOverview(tripId, userId);
        return ResponseEntity.ok(new BudgetUpdateResponse("Budget updated successfully", overview));
    }

    @PostMapping("/trip/{tripId}/check")
    public ResponseEntity<Boolean> checkBudget(
            @PathVariable Long tripId,
            @RequestBody AmountRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        return ResponseEntity.ok(
                budgetTrackingService.isBudgetAvailable(tripId, getUserId(userDetails), request.amount())
        );
    }

    @GetMapping("/trip/{tripId}/status")
    public ResponseEntity<BudgetStatusResponse> getBudgetStatus(
            @PathVariable Long tripId,
            @AuthenticationPrincipal UserDetails userDetails) {

        BudgetOverviewDTO overview = budgetTrackingService.getBudgetOverview(tripId, getUserId(userDetails));

        return ResponseEntity.ok(new BudgetStatusResponse(
                overview.getBudgetStatus(),
                overview.getBudgetPercentage(),
                overview.getBudgetSpent(),
                overview.getRemainingBudget()
        ));
    }
    private Long getUserId(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"))
                .getId();
    }
}