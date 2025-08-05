package modelos;

public class Dispositivo {
    private String ip;
    private String nombre;
    private boolean activo;
    
    // Getters y Setters
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
