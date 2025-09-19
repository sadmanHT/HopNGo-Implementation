package com.hopngo.auth.model;

import com.hopngo.auth.entity.User;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Role entity representing user roles in the system
 */
@Entity
@Table(name = "roles")
public class Role {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", unique = true, nullable = false)
    private String name;
    
    @Column(name = "description")
    private String description;
    
    // Note: Users reference roles via enum, not entity relationship
    // @ManyToMany(mappedBy = "roles")
    // private Set<User> users = new HashSet<>();
    
    // Default constructor
    public Role() {}
    
    // Constructor with name
    public Role(String name) {
        this.name = name;
    }
    
    // Constructor with name and description
    public Role(String name, String description) {
        this.name = name;
        this.description = description;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    // Note: Users reference roles via enum, not entity relationship
    // public Set<User> getUsers() {
    //     return users;
    // }
    // 
    // public void setUsers(Set<User> users) {
    //     this.users = users;
    // }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return name != null ? name.equals(role.name) : role.name == null;
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}