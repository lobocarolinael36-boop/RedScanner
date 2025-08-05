import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class EscanerRed {
    // Tiempo de espera para el ping (en milisegundos)
    private static final int TIMEOUT = 1000;

    public static List<Dispositivo> escanearRango(String ipInicio, String ipFin) {
        List<Dispositivo> dispositivos = new ArrayList<>();
        
        try {
            // Convertir las IPs a formato numérico para poder iterar
            long inicio = ipToLong(ipInicio);
            long fin = ipToLong(ipFin);
            
            // Validar que el rango sea correcto
            if (inicio > fin) {
                long temp = inicio;
                inicio = fin;
                fin = temp;
            }
            
            // Escanear cada IP en el rango
            for (long i = inicio; i <= fin; i++) {
                String ipActual = longToIp(i);
                Dispositivo dispositivo = new Dispositivo();
                dispositivo.setIp(ipActual);
                
                // Verificar si la IP está activa
                boolean activo = verificarConexion(ipActual, TIMEOUT);
                dispositivo.setActivo(activo);
                
                // Obtener nombre del host si está activo
                if (activo) {
                    dispositivo.setNombre(obtenerNombreHost(ipActual));
                } else {
                    dispositivo.setNombre("Desconocido");
                }
                
                dispositivos.add(dispositivo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return dispositivos;
    }

    // Método auxiliar para convertir IP a número
    private static long ipToLong(String ip) {
        String[] octetos = ip.split("\\.");
        return (Long.parseLong(octetos[0]) << 24) + 
               (Long.parseLong(octetos[1]) << 16) + 
               (Long.parseLong(octetos[2]) << 8) + 
               Long.parseLong(octetos[3]);
    }

    // Método auxiliar para convertir número a IP
    private static String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "." + 
               ((ip >> 16) & 0xFF) + "." + 
               ((ip >> 8) & 0xFF) + "." + 
               (ip & 0xFF);
    }

    public static boolean verificarConexion(String ip, int timeout) {
        try {
            InetAddress direccion = InetAddress.getByName(ip);
            return direccion.isReachable(timeout);
        } catch (Exception e) {
            return false;
        }
    }

    public static String obtenerNombreHost(String ip) {
        try {
            return InetAddress.getByName(ip).getHostName();
        } catch (Exception e) {
            return "No identificado";
        }
    }
}
