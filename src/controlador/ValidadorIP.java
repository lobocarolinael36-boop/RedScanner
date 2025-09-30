package controlador;

import java.util.regex.Pattern;
import java.util.StringTokenizer;

/**
 * Utilidades avanzadas para validación y manipulación de direcciones IP
 * Incluye validación IPv4, conversiones y cálculos de red
 */
public class ValidadorIP {
    // Patrón regex para validación IPv4
    private static final Pattern PATRON_IPV4 = Pattern.compile(
        "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
        "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // Direcciones IP especiales/reservadas
    private static final String[] IPS_RESERVADAS = {
        "0.", "127.", "169.254.", "224.", "240."
    };
    
    /**
     * Valida si una cadena es una dirección IPv4 válida
     */
    public static boolean esIPValida(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }
        
        String ipLimpia = ip.trim();
        
        // Validación básica con regex
        if (!PATRON_IPV4.matcher(ipLimpia).matches()) {
            return false;
        }
        
        // Validación adicional de cada octeto
        return validarOctetos(ipLimpia);
    }
    
    /**
     * Valida los octetos individuales de una IP
     */
    private static boolean validarOctetos(String ip) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(ip, ".");
            int contador = 0;
            
            while (tokenizer.hasMoreTokens()) {
                int octeto = Integer.parseInt(tokenizer.nextToken());
                if (octeto < 0 || octeto > 255) {
                    return false;
                }
                contador++;
            }
            
            return contador == 4; // Debe tener exactamente 4 octetos
            
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Convierte una dirección IP en formato string a long
     */
    public static long ipToLong(String ip) {
        if (!esIPValida(ip)) {
            throw new IllegalArgumentException("Dirección IP inválida: " + ip);
        }
        
        long resultado = 0;
        String[] octetos = ip.split("\\.");
        
        for (int i = 0; i < 4; i++) {
            resultado <<= 8;
            resultado |= Integer.parseInt(octetos[i]) & 0xFF;
        }
        
        return resultado & 0xFFFFFFFFL;
    }
    
    /**
     * Convierte un long a dirección IP en formato string
     */
    public static String longToIp(long ip) {
        if (ip < 0 || ip > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("Valor long fuera de rango para IP: " + ip);
        }
        
        return ((ip >> 24) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }
    
    /**
     * Valida si una IP está en un rango específico
     */
    public static boolean estaEnRango(String ip, String inicio, String fin) {
        try {
            long ipLong = ipToLong(ip);
            long inicioLong = ipToLong(inicio);
            long finLong = ipToLong(fin);
            
            return ipLong >= inicioLong && ipLong <= finLong;
            
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Verifica si una IP es una dirección reservada o especial
     */
    public static boolean esIPReservada(String ip) {
        if (!esIPValida(ip)) {
            return false;
        }
        
        for (String reservada : IPS_RESERVADAS) {
            if (ip.startsWith(reservada)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Obtiene la dirección de red basada en una IP y máscara
     */
    public static String obtenerDireccionRed(String ip, String mascara) {
        if (!esIPValida(ip) || !esIPValida(mascara)) {
            throw new IllegalArgumentException("IP o máscara inválida");
        }
        
        long ipLong = ipToLong(ip);
        long mascaraLong = ipToLong(mascara);
        long direccionRed = ipLong & mascaraLong;
        
        return longToIp(direccionRed);
    }
    
    /**
     * Calcula la dirección de broadcast
     */
    public static String obtenerBroadcast(String ip, String mascara) {
        if (!esIPValida(ip) || !esIPValida(mascara)) {
            throw new IllegalArgumentException("IP o máscara inválida");
        }
        
        long ipLong = ipToLong(ip);
        long mascaraLong = ipToLong(mascara);
        long wildcard = ~mascaraLong & 0xFFFFFFFFL;
        long broadcast = ipLong | wildcard;
        
        return longToIp(broadcast);
    }
    
    /**
     * Valida un rango completo de IPs
     */
    public static boolean esRangoValido(String inicio, String fin) {
        if (!esIPValida(inicio) || !esIPValida(fin)) {
            return false;
        }
        
        long inicioLong = ipToLong(inicio);
        long finLong = ipToLong(fin);
        
        return inicioLong <= finLong;
    }
    
    /**
     * Calcula la cantidad de IPs en un rango
     */
    public static int calcularTotalIPsEnRango(String inicio, String fin) {
        if (!esRangoValido(inicio, fin)) {
            return 0;
        }
        
        long inicioLong = ipToLong(inicio);
        long finLong = ipToLong(fin);
        
        return (int) (finLong - inicioLong + 1);
    }
    
    /**
     * Obtiene la siguiente IP en la secuencia
     */
    public static String obtenerSiguienteIP(String ip) {
        if (!esIPValida(ip)) {
            throw new IllegalArgumentException("IP inválida: " + ip);
        }
        
        long ipLong = ipToLong(ip);
        if (ipLong == 0xFFFFFFFFL) { // 255.255.255.255
            throw new IllegalArgumentException("No hay siguiente IP después de 255.255.255.255");
        }
        
        return longToIp(ipLong + 1);
    }
    
    /**
     * Obtiene la IP anterior en la secuencia
     */
    public static String obtenerIPAnterior(String ip) {
        if (!esIPValida(ip)) {
            throw new IllegalArgumentException("IP inválida: " + ip);
        }
        
        long ipLong = ipToLong(ip);
        if (ipLong == 0) { // 0.0.0.0
            throw new IllegalArgumentException("No hay IP anterior a 0.0.0.0");
        }
        
        return longToIp(ipLong - 1);
    }
    
    /**
     * Verifica si una IP es privada (RFC 1918)
     */
    public static boolean esIPPrivada(String ip) {
        if (!esIPValida(ip)) {
            return false;
        }
        
        return ip.startsWith("10.") ||
               ip.startsWith("192.168.") ||
               (ip.startsWith("172.") && estaEnRango(ip, "172.16.0.0", "172.31.255.255"));
    }
    
    /**
     * Obtiene la clase de la IP (A, B, C, D, E)
     */
    public static char obtenerClaseIP(String ip) {
        if (!esIPValida(ip)) {
            return 'X'; // Inválida
        }
        
        int primerOcteto = Integer.parseInt(ip.split("\\.")[0]);
        
        if (primerOcteto >= 1 && primerOcteto <= 126) return 'A';
        if (primerOcteto >= 128 && primerOcteto <= 191) return 'B';
        if (primerOcteto >= 192 && primerOcteto <= 223) return 'C';
        if (primerOcteto >= 224 && primerOcteto <= 239) return 'D';
        if (primerOcteto >= 240 && primerOcteto <= 255) return 'E';
        
        return 'X';
    }
}