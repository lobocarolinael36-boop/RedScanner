package modelos;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

/**
 * Clase de utilidad (Modelo) encargada de la ejecución de comandos de red
 * de bajo nivel (Netstat, ss) en el sistema operativo.
 * Maneja la diferencia entre Windows y sistemas basados en Unix (Linux/macOS).
 */
public class AnalizadorNetstat {

    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    
    // Comando básico para obtener conexiones activas y pids
    private static final String[] CMD_CONEXIONES_WIN = {"cmd.exe", "/c", "netstat -ano"};
    // En Linux/macOS se prefiere 'ss' por ser más rápido que 'netstat'
    private static final String[] CMD_CONEXIONES_UNIX = {"/bin/sh", "-c", "ss -tunap"}; 
    
    // Comando para obtener estadísticas de protocolos (paquetes, errores, etc.)
    private static final String[] CMD_ESTADISTICAS_WIN = {"cmd.exe", "/c", "netstat -s"};
    private static final String[] CMD_ESTADISTICAS_UNIX = {"/bin/sh", "-c", "netstat -s"};
    
    /**
     * Ejecuta el comando para obtener todas las conexiones activas.
     * @return El texto crudo (raw) de la salida del comando.
     */
    public static String obtenerConexionesActivas() {
        return ejecutarComando(IS_WINDOWS ? CMD_CONEXIONES_WIN : CMD_CONEXIONES_UNIX);
    }
    
    /**
     * Ejecuta el comando para obtener las estadísticas de protocolo.
     * @return El texto crudo (raw) de la salida del comando.
     */
    public static String obtenerEstadisticasProtocolo() {
        return ejecutarComando(IS_WINDOWS ? CMD_ESTADISTICAS_WIN : CMD_ESTADISTICAS_UNIX);
    }

    /**
     * Lógica central para la ejecución de comandos del sistema.
     * @param comando Comando a ejecutar como array de strings.
     * @return La salida completa del comando o un mensaje de error.
     */
    private static String ejecutarComando(String[] comando) {
        StringBuilder output = new StringBuilder();
        try {
            // Se usa ProcessBuilder para más control, aunque Runtime.exec() también funcionaría.
            ProcessBuilder pb = new ProcessBuilder(comando);
            Process p = pb.start();
            
            // Capturar la salida estándar
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append(System.lineSeparator());
                }
            }
            
            // Capturar la salida de error (importante para detectar "Acceso denegado")
            try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    output.append("Error: ").append(errorLine).append(System.lineSeparator());
                }
            }
            
            // Esperar a que el proceso termine
            p.waitFor();
            
        } catch (Exception e) {
            return "Error: Excepción al ejecutar el comando: " + e.getMessage();
        }
        
        return output.toString();
    }
}
