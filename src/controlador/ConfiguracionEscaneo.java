// File: src/controlador/ConfiguracionEscaneo.java
package controlador;

import java.util.prefs.Preferences;


public class ConfiguracionEscaneo {
    private static final String PREF_NODE = "escaner_red/config";
    private static final String KEY_IP_BASE = "ip_base";
    private static final String KEY_TIEMPO_ESPERA = "tiempo_espera";
    private static final String KEY_ULTIMA_RED = "ultima_red";

    private final Preferences prefs;

    public ConfiguracionEscaneo() {
        prefs = Preferences.userRoot().node(PREF_NODE);
    }

    public void guardarIpBase(String ipBase) {
        if (ipBase == null) return;
        prefs.put(KEY_IP_BASE, ipBase);
    }

    public String cargarIpBase() {
        return prefs.get(KEY_IP_BASE, "192.168.1.");
    }

    public void guardarTiempoEspera(int ms) {
        prefs.putInt(KEY_TIEMPO_ESPERA, Math.max(100, ms));
    }

    public int cargarTiempoEspera() {
        return prefs.getInt(KEY_TIEMPO_ESPERA, 1000);
    }

    public void guardarUltimaRed(String red) {
        if (red == null) return;
        prefs.put(KEY_ULTIMA_RED, red);
    }

    public String cargarUltimaRed() {
        return prefs.get(KEY_ULTIMA_RED, "");
    }

    public void limpiarConfiguracion() {
        try {
            prefs.clear();
        } catch (Exception e) {
            System.err.println("Error limpiando configuración: " + e.getMessage());
        }
    }
}

