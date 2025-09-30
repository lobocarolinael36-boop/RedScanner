package app;

import modelos.Dispositivo;

import javax.swing.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.io.BufferedReader;
import java.io.InputStreamReader;
<<<<<<< HEAD
=======
// [ADICIÓN]
import java.io.IOException;
>>>>>>> 6e71926a047611456a20f4160b6cc0275edf1aca

public class EscanerRed {

    private static final List<Integer> PUERTOS_PREDETERMINADOS = Arrays.asList(21, 22, 80, 443, 3389);

    private static final AtomicBoolean escaneoEnCurso = new AtomicBoolean(false);
    private static final AtomicBoolean detenerEscaneo = new AtomicBoolean(false);
    private static ExecutorService executorService;

    // --- API principal ---
    public static List<Dispositivo> escanearRangoAvanzado(
            String ipInicio,
            String ipFin,
            int hilos,
            int timeoutPingMs,
            int timeoutPuertoMs,
            JProgressBar progressBar
    ) {
        return escanearRango(ipInicio, ipFin, hilos, timeoutPingMs, timeoutPuertoMs, progressBar);
    }

    private static List<Dispositivo> escanearRango(
            String ipInicio,
            String ipFin,
            int hilos,
            int timeoutPingMs,
            int timeoutPuertoMs,
            JProgressBar progressBar
    ) {
        inicializarEscaneo();
        List<Dispositivo> dispositivos = new CopyOnWriteArrayList<>();

        try {
            long inicio = ipToLong(ipInicio);
            long fin = ipToLong(ipFin);
            long totalIPs = fin - inicio + 1;

            AtomicLong ipsEscaneadas = new AtomicLong(0);
            executorService = Executors.newFixedThreadPool(hilos);
            List<Future<?>> futures = new ArrayList<>();

            for (long i = inicio; i <= fin && !detenerEscaneo.get(); i++) {
                final String ipActual = longToIp(i);
                futures.add(executorService.submit(() -> {
                    if (detenerEscaneo.get()) return;
                    Dispositivo d = escanearDispositivo(ipActual, timeoutPingMs, timeoutPuertoMs);
                    dispositivos.add(d);
                    if (progressBar != null) {
                        actualizarProgreso(progressBar, ipsEscaneadas.incrementAndGet(), totalIPs);
                    }
                }));
            }

            esperarFinalizacion(futures);
        } catch (Exception e) {
            manejarError(e);
        } finally {
            finalizarEscaneo();
        }

        dispositivos.sort(Comparator.comparing(Dispositivo::getIp));
        return dispositivos;
    }

    // --- Escaneo por IP ---
    public static Dispositivo escanearDispositivo(String ip, int timeoutPingMs, int timeoutPuertoMs) {
        Dispositivo dispositivo = new Dispositivo();
        dispositivo.setIp(ip);

        try {
            // 1) Ping de sistema
            long start = System.currentTimeMillis();
            boolean activo = pingSistema(ip, timeoutPingMs);
            long tiempo = System.currentTimeMillis() - start;

            dispositivo.setActivo(activo);
            dispositivo.setTiempoRespuesta(tiempo);

            if (activo) {
                // 2) Nombre por nslookup (sistema) y fallback Java
                String nombre = obtenerNombreHost(ip);
                dispositivo.setNombre(nombre);

                // 3) Puertos comunes abiertos
                dispositivo.setPuertosAbiertos(escanearPuertos(ip, timeoutPuertoMs));
            } else {
                dispositivo.setNombre("No encontrado");
            }
        } catch (Exception e) {
            dispositivo.setNombre("Error: " + e.getMessage());
        }
        return dispositivo;
    }

<<<<<<< HEAD
    // Ping usando comando del SO (Windows / Unix). Si se excede el timeout, se mata el proceso.
    private static boolean pingSistema(String ip, int timeoutMs) {
        ProcessBuilder pb;
        if (esWindows()) {
            // -n 1 una solicitud, -w timeout en ms
            pb = new ProcessBuilder("cmd.exe", "/c", "ping -n 1 -w " + timeoutMs + " " + ip);
        } else {
            // Unix (Linux/macOS): -c 1 una solicitud. Timeout se maneja con waitFor(timeout)
            pb = new ProcessBuilder("bash", "-lc", "ping -c 1 " + ip);
        }
        try {
            Process p = pb.start();
            boolean finished = p.waitFor(timeoutMs + 200L, TimeUnit.MILLISECONDS);
=======
    /**
     * Netstat 1: Muestra conexiones activas con formato numérico y ID de proceso.
     * Equivalente a `netstat -ano` (Windows) o `netstat -anp` (Linux/macOS - puede requerir sudo).
     * @return La salida del comando Netstat -ano / -anp.
     */
    public static String obtenerConexionesActivas() {
        String comando;
        if (esWindows()) {
            comando = "netstat -ano";
        } else {
            // Linux/macOS - intentar con privilegios reducidos primero
            comando = "netstat -an";
        }
        return ejecutarComandoSistema(comando);
    }

    /**
     * Netstat 2: Muestra estadísticas por protocolo (TCP, UDP, ICMP, etc.).
     * Equivalente a `netstat -s`.
     * @return La salida del comando Netstat -s.
     */
    public static String obtenerEstadisticasProtocolo() {
        String comando = "netstat -s";
        return ejecutarComandoSistema(comando);
    }

    /**
     * Netstat 3: Muestra la tabla de enrutamiento de la red.
     * Equivalente a `netstat -r` o `route print` (Windows).
     * @return La salida del comando Netstat -r.
     */
    public static String obtenerTablaEnrutamiento() {
        String comando;
        if (esWindows()) {
            comando = "route print"; // Alternativa común para la tabla de enrutamiento en Windows
        } else {
            comando = "netstat -r"; // Tabla de enrutamiento en Unix
        }
        return ejecutarComandoSistema(comando);
    }

    /**
     * Netstat 4: Muestra las interfaces de red y estadísticas.
     * Equivalente a `netstat -i` o `ipconfig` en Windows.
     * @return La salida del comando de interfaces de red.
     */
    public static String obtenerInterfacesRed() {
        String comando;
        if (esWindows()) {
            comando = "ipconfig";
        } else {
            comando = "netstat -i";
        }
        return ejecutarComandoSistema(comando);
    }
    
    
    private static boolean pingSistema(String ip, int timeoutMs) {
        ProcessBuilder pb;
        try {
            if (esWindows()) {
                pb = new ProcessBuilder("cmd.exe", "/c", "ping -n 1 -w " + timeoutMs + " " + ip);
            } else {
                // Unix/Linux/macOS - convertir timeoutMs a segundos para ping
                int timeoutSec = Math.max(1, timeoutMs / 1000);
                pb = new ProcessBuilder("bash", "-c", "ping -c 1 -W " + timeoutSec + " " + ip);
            }
            
            Process p = pb.start();
            boolean finished = p.waitFor(timeoutMs + 1000L, TimeUnit.MILLISECONDS);
>>>>>>> 6e71926a047611456a20f4160b6cc0275edf1aca
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
<<<<<<< HEAD
            int exit = p.exitValue();
            // En la mayoría de plataformas: 0 = éxito
            return exit == 0;
=======
            return p.exitValue() == 0;
>>>>>>> 6e71926a047611456a20f4160b6cc0275edf1aca
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean esWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

<<<<<<< HEAD
=======
    /**
     * Ejecuta un comando del sistema y devuelve su salida.
     * @param comando El comando a ejecutar (ej: "netstat -ano").
     * @return La salida del comando como una cadena de texto.
     */
    private static String ejecutarComandoSistema(String comando) {
        ProcessBuilder pb;
        try {
            if (esWindows()) {
                pb = new ProcessBuilder("cmd.exe", "/c", comando);
            } else {
                pb = new ProcessBuilder("bash", "-c", comando);
            }

            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return "Error: Comando excedió el tiempo límite";
            }

            // Leer salida estándar
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            // Si no hay salida, leer error
            if (output.length() == 0 && process.exitValue() != 0) {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String errorLine;
                    while ((errorLine = errorReader.readLine()) != null) {
                        output.append("Error: ").append(errorLine).append("\n");
                    }
                }
            }

            return output.length() > 0 ? output.toString() : "Comando ejecutado (sin salida)";
        } catch (Exception e) {
            return "Error ejecutando '" + comando + "': " + e.getMessage();
        }
    }
    
>>>>>>> 6e71926a047611456a20f4160b6cc0275edf1aca
    // Nombre por nslookup (sistema) y fallback a Java
    private static String obtenerNombreHost(String ip) {
        String nombre = ejecutarComandoNSLookup(ip);
        if (nombre == null || nombre.isEmpty() || "Nombre no disponible".equals(nombre)) {
            nombre = obtenerNombreHostJava(ip);
        }
        return nombre;
    }

    private static String ejecutarComandoNSLookup(String ip) {
        try {
            ProcessBuilder pb = esWindows()
                    ? new ProcessBuilder("cmd.exe", "/c", "nslookup " + ip)
                    : new ProcessBuilder("bash", "-lc", "nslookup " + ip);
            Process process = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            String lastName = null;

            while ((line = reader.readLine()) != null) {
                // Buscamos patrones típicos
                // Ej: "name = host.example.com." o "Nombre: host.example.com"
                line = line.trim();
                if (line.contains("name =")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) lastName = parts[1].trim().replaceAll("\\.$", "");
                } else if (line.toLowerCase().startsWith("nombre:")) {
                    lastName = line.substring(line.indexOf(':') + 1).trim().replaceAll("\\.$", "");
                }
            }
            process.waitFor(2, TimeUnit.SECONDS);
            return lastName != null ? lastName : "Nombre no disponible";
        } catch (Exception e) {
            return "Nombre no disponible";
        }
    }

    private static String obtenerNombreHostJava(String ip) {
        try {
            return InetAddress.getByName(ip).getHostName();
        } catch (Exception e) {
            return "Nombre no disponible";
        }
    }

    private static List<Integer> escanearPuertos(String ip, int timeoutPuertoMs) {
        List<Integer> puertosAbiertos = new ArrayList<>();
        for (int puerto : PUERTOS_PREDETERMINADOS) {
            if (puertoEstaAbierto(ip, puerto, timeoutPuertoMs)) {
                puertosAbiertos.add(puerto);
            }
        }
        return puertosAbiertos;
    }

    private static boolean puertoEstaAbierto(String ip, int puerto, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, puerto), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // --- Gestión de escaneo y progreso ---
    private static void inicializarEscaneo() {
        escaneoEnCurso.set(true);
        detenerEscaneo.set(false);
    }

    private static void actualizarProgreso(JProgressBar progressBar, long completados, long total) {
        SwingUtilities.invokeLater(() -> {
            int progreso = (int) ((completados * 100) / total);
            progressBar.setValue(progreso);
        });
    }

    private static void esperarFinalizacion(List<Future<?>> futures) {
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                if (!detenerEscaneo.get()) e.printStackTrace();
            }
        }
    }

    private static void manejarError(Exception e) {
        if (!detenerEscaneo.get()) e.printStackTrace();
    }

    private static void finalizarEscaneo() {
        if (executorService != null) {
            executorService.shutdownNow();
            try {
                executorService.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        escaneoEnCurso.set(false);
    }

    public static void detenerEscaneo() {
        detenerEscaneo.set(true);
        finalizarEscaneo();
    }

    public static boolean estaEscaneando() {
        return escaneoEnCurso.get();
    }

    // --- Validación y utilidades de IP ---
    public static boolean validarIP(String ip) {
        if (ip == null) return false;
        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) return false;
        try {
            for (String p : parts) {
                if (p.isEmpty() || (p.length() > 1 && p.startsWith("0") && !p.equals("0"))) {
                    // evitamos octetos tipo "001" (estilo confuso) salvo "0"
                }
                int v = Integer.parseInt(p);
                if (v < 0 || v > 255) return false;
            }
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    public static long ipToLong(String ip) {
        if (!validarIP(ip)) {
<<<<<<< HEAD
            throw new IllegalArgumentException("Dirección IP inválida: " + ip);
        }
        String[] o = ip.split("\\.");
        return (Long.parseLong(o[0]) << 24)
                | (Long.parseLong(o[1]) << 16)
                | (Long.parseLong(o[2]) << 8)
                | Long.parseLong(o[3]);
    }

    public static boolean validarRangoIPs(String ipInicio, String ipFin) {
        try {
            long ini = ipToLong(ipInicio);
            long fin = ipToLong(ipFin);
            return fin >= ini;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static String longToIp(long ip) {
        return ((ip >> 24) & 0xFF) + "."
                + ((ip >> 16) & 0xFF) + "."
                + ((ip >> 8) & 0xFF) + "."
                + (ip & 0xFF);
    }
=======
            throw new IllegalArgumentException("IP inválida para conversión: " + ip);
        }
        String[] parts = ip.trim().split("\\.");
        // [CORRECCIÓN/COMPLETADO] Completar la lógica de conversión
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = result | (Long.parseLong(parts[i]) << (24 - (8 * i)));
        }
        return result & 0xFFFFFFFFL; // Asegurar que sea unsigned (positivo)
    }

    public static String longToIp(long ipLong) {
        return ((ipLong >> 24) & 0xFF) + "." +
               ((ipLong >> 16) & 0xFF) + "." +
               ((ipLong >> 8) & 0xFF) + "." +
               (ipLong & 0xFF);
    }

    public static boolean validarRangoIPs(String ipInicio, String ipFin) {
        if (!validarIP(ipInicio) || !validarIP(ipFin)) return false;
        try {
            long inicio = ipToLong(ipInicio);
            long fin = ipToLong(ipFin);
            return inicio <= fin;
        } catch (Exception e) {
            return false;
        }
    }
>>>>>>> 6e71926a047611456a20f4160b6cc0275edf1aca
}
