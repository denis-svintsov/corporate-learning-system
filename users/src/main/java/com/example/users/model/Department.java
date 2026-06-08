package com.example.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "department", schema = "users")
public class Department {

    @Id
    @Column(name = "department_id")
    private String departmentId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "manager_id")
    private String managerId;

    @Column(name = "parent_department_id")
    private String parentDepartmentId;
}
