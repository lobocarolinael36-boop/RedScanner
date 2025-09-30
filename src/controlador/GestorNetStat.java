package controlador;

import modelos.ConexionRed;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gestor profesional de NetStat con 3 funciones específicas:
 * 1. Conexiones activas establecidas
 * 2. Puertos en escucha locales
 * 3. Estadísticas de protocolos (devuelve la salida cruda para robustez)
 */
public class GestorNetStat {
    // Patrones de regex se mantienen para las funciones 1 y 2
    private static final Pattern TCP_UDP_PATTERN = Pattern.compile(
        "(TCP|UDP)\\s+([^\\s]+):(\\d+)\\s+([^\\s]+):(\\d+)\\s+(\\w+)"
    );
    
    private final List<NetStatListener> listeners = new CopyOnWriteArrayList<>();
    
    public interface NetStatListener {
        void onNetStatIniciado(String funcion);
        void onConexionEncontrada(ConexionRed conexion);
        void onNetStatCompletado(String funcion, int totalConexiones);
        void onErrorNetStat(String error);
    }

    public void agregarListener(NetStatListener listener) {
        listeners.add(listener);
    }

    public void removerListener(NetStatListener listener) {
        listeners.remove(listener);
    }

    // ====================================================================
    // NUEVO MÉTODO AUXILIAR PARA OBTENER SALIDA CRUDA (RAW OUTPUT)
    // ====================================================================
    
    /**
     * Ejecuta netstat con un argumento específico y devuelve toda la salida de texto.
     */
    public String obtenerSalidaNetstatRaw(String argumento) {
        StringBuilder output = new StringBuilder();
        try {
            // Usar "cmd /c" en Windows para asegurar la ejecución correcta de netstat
            String comando = isWindows() ? "cmd /c netstat " + argumento : "netstat " + argumento;
            Process process = Runtime.getRuntime().exec(comando);
            
            // Usamos la codificación del sistema para manejar caracteres en español (Windows)
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            notifyError("Error ejecutando netstat " + argumento + ": " + e.getMessage());
            return "ERROR al ejecutar comando netstat. Verifique permisos o el comando.\n" + e.getMessage();
        }
        return output.toString();
    }
    
    // ====================================================================
    // FUNCIONES ORIGINALES (1 y 2)
    // ====================================================================

    /**
     * FUNCIÓN 1: Obtener conexiones activas establecidas
     */
    public List<ConexionRed> obtenerConexionesActivas() {
        notifyIniciado("CONEXIONES_ACTIVAS");
        List<ConexionRed> conexiones = new ArrayList<>();
        
        try {
            // Se usa '-ano' para obtener PID en Windows y '-tupan' para Linux/macOS
            String comando = isWindows() ? "netstat -ano" : "netstat -tupan"; 
            Process process = Runtime.getRuntime().exec(comando);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    ConexionRed conexion = parsearLineaNetstat(line);
                    if (conexion != null && 
                        (conexion.getEstado() == ConexionRed.EstadoConexion.ESTABLISHED ||
                         conexion.getEstado() == ConexionRed.EstadoConexion.CLOSE_WAIT)) {
                        conexiones.add(conexion);
                        notifyConexionEncontrada(conexion);
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            notifyError("Error obteniendo conexiones activas: " + e.getMessage());
        }
        
        notifyCompletado("CONEXIONES_ACTIVAS", conexiones.size());
        return conexiones;
    }

    /**
     * FUNCIÓN 2: Obtener puertos en escucha locales
     */
    public List<ConexionRed> obtenerPuertosEscucha() {
        notifyIniciado("PUERTOS_ESPERA");
        List<ConexionRed> puertos = new ArrayList<>();
        
        try {
            String comando = isWindows() ? "netstat -ano" : "netstat -tupan";
            Process process = Runtime.getRuntime().exec(comando);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    ConexionRed conexion = parsearLineaNetstat(line);
                    if (conexion != null && 
                        conexion.getEstado() == ConexionRed.EstadoConexion.LISTENING) {
                        puertos.add(conexion);
                        notifyConexionEncontrada(conexion);
                    }
                }
            }
            
            process.waitFor();
        } catch (Exception e) {
            notifyError("Error obteniendo puertos en escucha: " + e.getMessage());
        }
        
        notifyCompletado("PUERTOS_ESPERA", puertos.size());
        return puertos;
    }

    // ====================================================================
    // FUNCIÓN 3: ESTADÍSTICAS (MODIFICADA)
    // ====================================================================
    
    /**
     * FUNCIÓN 3 (REAL): Obtener estadísticas de protocolos como texto crudo.
     * Este es el método que usarás en VentanaNetStat.java.
     */
    public String obtenerEstadisticasProtocolosRaw() {
        notifyIniciado("ESTADISTICAS_PROTOCOLOS");
        
        // Obtenemos la salida cruda de netstat -s
        String estadisticasRaw = obtenerSalidaNetstatRaw("-s");
        
        // Notificamos completado. Usamos la longitud del texto como indicador de éxito.
        notifyCompletado("ESTADISTICAS_PROTOCOLOS", estadisticasRaw.length());
        
        return estadisticasRaw;
    }
    
    /**
     * FUNCIÓN 3 (COMPATIBILIDAD): Mantiene la firma original pero devuelve un mapa vacío.
     * Necesario para mantener la estructura de la clase aunque el método sea obsoleto.
     */
    public Map<String, Integer> obtenerEstadisticasProtocolos() {
        // Llamamos al método Raw para que se ejecute la lógica de notificación y ejecución
        obtenerEstadisticasProtocolosRaw();
        return new HashMap<>(); 
    }
    
    // ====================================================================
    // MÉTODOS AUXILIARES
    // ====================================================================

    private ConexionRed parsearLineaNetstat(String linea) {
        try {
            Matcher matcher = TCP_UDP_PATTERN.matcher(linea);
            if (matcher.find()) { 
                ConexionRed.TipoProtocolo protocolo = ConexionRed.TipoProtocolo.valueOf(matcher.group(1));
                String dirLocal = matcher.group(2);
                int puertoLocal = Integer.parseInt(matcher.group(3));
                String dirRemota = matcher.group(4);
                int puertoRemoto = Integer.parseInt(matcher.group(5));
                ConexionRed.EstadoConexion estado = parsearEstado(matcher.group(6));
                
                int pid = extraerPID(linea);
                String proceso = obtenerNombreProceso(pid);
                
                return new ConexionRed(protocolo, dirLocal, puertoLocal, 
                                      dirRemota, puertoRemoto, estado, pid, proceso);
            }
        } catch (Exception e) {
            // Ignorar líneas que no se pueden parsear, como encabezados
        }
        return null;
    }

    private ConexionRed.EstadoConexion parsearEstado(String estadoStr) {
        try {
            return ConexionRed.EstadoConexion.valueOf(estadoStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ConexionRed.EstadoConexion.ESTABLISHED; 
        }
    }

    private int extraerPID(String linea) {
        try {
            String[] partes = linea.trim().split("\\s+");
            // El PID está en la última columna de netstat -ano
            return Integer.parseInt(partes[partes.length - 1]);
        } catch (Exception e) {
            return -1;
        }
    }

    private String obtenerNombreProceso(int pid) {
        if (pid <= 0) return "N/A";
        
        try {
            String comando = isWindows() ? "tasklist /FI \"PID eq " + pid + "\"" : "ps -p " + pid + " -o comm=";
            Process process = Runtime.getRuntime().exec(comando);
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                if (isWindows()) {
                    // Saltar líneas de encabezado en Windows (2 líneas)
                    reader.readLine();
                    reader.readLine();
                    line = reader.readLine();
                    if (line != null) {
                        String[] partes = line.split("\\s+");
                        return partes.length > 0 ? partes[0] : "N/A";
                    }
                } else {
                    line = reader.readLine();
                    return line != null ? line.trim() : "N/A";
                }
            }
        } catch (Exception e) {
            return "N/A";
        }
        return "N/A";
    }

    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    private void notifyIniciado(String funcion) {
        for (NetStatListener listener : listeners) {
            listener.onNetStatIniciado(funcion);
        }
    }

    private void notifyConexionEncontrada(ConexionRed conexion) {
        for (NetStatListener listener : listeners) {
            listener.onConexionEncontrada(conexion);
        }
    }

    private void notifyCompletado(String funcion, int total) {
        for (NetStatListener listener : listeners) {
            listener.onNetStatCompletado(funcion, total);
        }
    }

    private void notifyError(String error) {
        for (NetStatListener listener : listeners) {
            listener.onErrorNetStat(error);
        }
    }
}