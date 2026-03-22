package com.example.uavbackend.auth;

import com.example.uavbackend.auth.dto.DepartmentDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
  private final DepartmentService departmentService;

  @GetMapping
  public List<DepartmentDto> list() {
    return departmentService.list();
  }

  @PostMapping
  public DepartmentDto create(@RequestBody DepartmentService.CreateDepartmentRequest request) {
    return departmentService.create(request);
  }

  @PutMapping("/{id}")
  public DepartmentDto update(
      @PathVariable("id") Long id, @RequestBody DepartmentService.CreateDepartmentRequest request) {
    return departmentService.update(id, request);
  }

  @DeleteMapping("/{id}")
  public void delete(@PathVariable("id") Long id) {
    departmentService.delete(id);
  }
}
