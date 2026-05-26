package com.syfe.finance.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.syfe.finance.dto.GoalListResponse;
import com.syfe.finance.dto.GoalRequest;
import com.syfe.finance.dto.GoalResponse;
import com.syfe.finance.dto.GoalUpdateRequest;
import com.syfe.finance.dto.MessageResponse;
import com.syfe.finance.service.SavingsGoalService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalController {

    private final SavingsGoalService savingsGoalService;

    public SavingsGoalController(SavingsGoalService savingsGoalService) {
        this.savingsGoalService = savingsGoalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse create(@Valid @RequestBody GoalRequest request) {
        return savingsGoalService.create(request);
    }

    @GetMapping
    public GoalListResponse getAll() {
        return savingsGoalService.getAll();
    }

    @GetMapping("/{id}")
    public GoalResponse get(@PathVariable Long id) {
        return savingsGoalService.get(id);
    }

    @PutMapping("/{id}")
    public GoalResponse update(@PathVariable Long id, @RequestBody GoalUpdateRequest request) {
        return savingsGoalService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Long id) {
        return savingsGoalService.delete(id);
    }
}
