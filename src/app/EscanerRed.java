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
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            int exit = p.exitValue();
            // En la mayoría de plataformas: 0 = éxito
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean esWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }

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
}
