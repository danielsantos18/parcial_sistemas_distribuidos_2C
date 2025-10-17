package com.inferno.catalog_service.util;

import lombok.Data;

@Data
public class ServiceInfo {
    private String id;
    private String categoria;
    private String proveedor;
    private String servicio;
    private String plan;
    private double precioMensual;
    private String detalles;
    private String estado;
}
