<<<<<<< HEAD
		// File: src/modelo/Dispositivo.java
		package modelos;
		
		import java.util.Objects;
		
		public class Dispositivo {
		    private final String direccionIP;
		    private final String nombreHost;
		    private final boolean enLinea;
		    private final long tiempoRespuesta;
		
		    public Dispositivo(String direccionIP, String nombreHost, boolean enLinea, long tiempoRespuesta) {
		        this.direccionIP = direccionIP;
		        this.nombreHost = nombreHost == null ? "" : nombreHost;
		        this.enLinea = enLinea;
		        this.tiempoRespuesta = tiempoRespuesta;
		    }
		
		    // Getters
		    public String getDireccionIP() { return direccionIP; }
		    public String getNombreHost() { return nombreHost; }
		    public boolean estaEnLinea() { return enLinea; }
		    public long getTiempoRespuesta() { return tiempoRespuesta; }
		
		    @Override
		    public String toString() {
		        return String.format("%s (%s) - %s ms", nombreHost.isEmpty() ? "-" : nombreHost, direccionIP, tiempoRespuesta);
		    }
		
		    @Override
		    public boolean equals(Object o) {
		        if (this == o) return true;
		        if (!(o instanceof Dispositivo)) return false;
		        Dispositivo that = (Dispositivo) o;
		        return Objects.equals(direccionIP, that.direccionIP);
		    }
		
		    @Override
		    public int hashCode() {
		        return Objects.hash(direccionIP);
		    }
		}
		
		
=======
package modelos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Dispositivo {
    private String ip;
    private String nombre;
    private boolean activo;
    private long tiempoRespuesta;
    private List<Integer> puertosAbiertos;

    public Dispositivo() {
        this.puertosAbiertos = new ArrayList<>();
    }

    public Dispositivo(String ip, String nombre, boolean activo, long tiempoRespuesta) {
        this();
        this.ip = ip;
        this.nombre = nombre;
        this.activo = activo;
        this.tiempoRespuesta = tiempoRespuesta;
    }

    // Getters y Setters
    public String getIp() {
        return ip != null ? ip : "IP no disponible";
    }

    public void setIp(String ip) {
        this.ip = Objects.requireNonNull(ip, "La IP no puede ser nula");
    }

    public String getNombre() {
        return nombre != null ? nombre : "Nombre no disponible";
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public long getTiempoRespuesta() {
        return tiempoRespuesta;
    }

    public void setTiempoRespuesta(long tiempoRespuesta) {
        this.tiempoRespuesta = Math.max(0, tiempoRespuesta);
    }

    public List<Integer> getPuertosAbiertos() {
        return new ArrayList<>(puertosAbiertos);
    }

    public void setPuertosAbiertos(List<Integer> puertosAbiertos) {
        this.puertosAbiertos.clear();
        if (puertosAbiertos != null) {
            this.puertosAbiertos.addAll(puertosAbiertos);
        }
    }

    // Helpers para UI/CSV
    public String getEstadoFormateado() {
        return activo ? "Activo (" + tiempoRespuesta + " ms)" : "Inactivo";
    }

    public String getPuertosFormateados() {
        return puertosAbiertos == null || puertosAbiertos.isEmpty() ? "Ninguno" : puertosAbiertos.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Dispositivo)) return false;
        Dispositivo that = (Dispositivo) o;
        return Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }

    @Override
    public String toString() {
        return String.format(
                "IP: %-15s | Nombre: %-20s | Estado: %-15s | Puertos: %s",
                ip, nombre, getEstadoFormateado(), getPuertosFormateados()
        );
    }

    // Builder para creaciones flexibles (opcional)
    public static class Builder {
        private final Dispositivo dispositivo = new Dispositivo();

        public Builder conIp(String ip) {
            dispositivo.setIp(ip);
            return this;
        }

        public Builder conNombre(String nombre) {
            dispositivo.setNombre(nombre);
            return this;
        }

        public Builder activo(boolean activo) {
            dispositivo.setActivo(activo);
            return this;
        }

        public Builder conTiempoRespuesta(long tiempo) {
            dispositivo.setTiempoRespuesta(tiempo);
            return this;
        }

        public Builder conPuertos(List<Integer> puertos) {
            dispositivo.setPuertosAbiertos(puertos);
            return this;
        }

        public Dispositivo construir() {
            return dispositivo;
        }
    }
}
>>>>>>> 6e71926a047611456a20f4160b6cc0275edf1aca
