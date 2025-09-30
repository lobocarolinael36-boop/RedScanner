package modelos;

import java.util.Objects;

public class ConexionRed {
    public enum TipoProtocolo { TCP, UDP }
    public enum EstadoConexion { 
        LISTENING, ESTABLISHED, CLOSE_WAIT, TIME_WAIT, 
        SYN_SENT, SYN_RECEIVED, FIN_WAIT_1, FIN_WAIT_2 
    }

    private final TipoProtocolo protocolo;
    private final String direccionLocal;
    private final int puertoLocal;
    private final String direccionRemota;
    private final int puertoRemoto;
    private final EstadoConexion estado;
    private final int pid;
    private final String nombreProceso;

    public ConexionRed(TipoProtocolo protocolo, String direccionLocal, int puertoLocal,
                      String direccionRemota, int puertoRemoto, EstadoConexion estado,
                      int pid, String nombreProceso) {
        this.protocolo = protocolo;
        this.direccionLocal = direccionLocal;
        this.puertoLocal = puertoLocal;
        this.direccionRemota = direccionRemota;
        this.puertoRemoto = puertoRemoto;
        this.estado = estado;
        this.pid = pid;
        this.nombreProceso = nombreProceso;
    }

    // Getters
    public TipoProtocolo getProtocolo() { return protocolo; }
    public String getDireccionLocal() { return direccionLocal; }
    public int getPuertoLocal() { return puertoLocal; }
    public String getDireccionRemota() { return direccionRemota; }
    public int getPuertoRemoto() { return puertoRemoto; }
    public EstadoConexion getEstado() { return estado; }
    public int getPid() { return pid; }
    public String getNombreProceso() { return nombreProceso; }

    public boolean esLocal() {
        return direccionLocal.startsWith("127.") || 
               direccionLocal.startsWith("192.168.") ||
               direccionLocal.equals("0.0.0.0") ||
               direccionLocal.equals("::1");
    }

    @Override
    public String toString() {
        return String.format("%-5s %-20s:%-5d -> %-20s:%-5d [%-12s] PID: %-6d %s",
            protocolo, direccionLocal, puertoLocal, direccionRemota, puertoRemoto,
            estado, pid, nombreProceso != null ? nombreProceso : "N/A");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConexionRed)) return false;
        ConexionRed that = (ConexionRed) o;
        return puertoLocal == that.puertoLocal &&
               puertoRemoto == that.puertoRemoto &&
               Objects.equals(direccionLocal, that.direccionLocal) &&
               Objects.equals(direccionRemota, that.direccionRemota) &&
               protocolo == that.protocolo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(protocolo, direccionLocal, puertoLocal, direccionRemota, puertoRemoto);
    }
}