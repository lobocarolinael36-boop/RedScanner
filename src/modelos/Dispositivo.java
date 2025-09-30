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
		
		