package com.lapzone.api.user;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "rol")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_rol")
    private UUID id;

    @Column(name = "nombre_rol", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "descripcion")
    private String description;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
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
}