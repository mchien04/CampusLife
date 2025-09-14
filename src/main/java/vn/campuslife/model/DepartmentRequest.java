package vn.campuslife.model;

import lombok.Data;
import vn.campuslife.enumeration.DepartmentType;

@Data
public class DepartmentRequest {
    private String name;
    private DepartmentType type;
    private String description;
}


