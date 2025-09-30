package controlador;

import modelos.Dispositivo;
import controlador.ValidadorIP;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Escáner de red profesional con gestión avanzada de hilos y métricas
 */
public class EscanerRed {
    private static final int MAX_HILOS = 50;
    
    private final String ipInicio;
    private final String ipFin;
    private final int tiempoEsperaMs;
    private final int maxHilos;
    private final List<EscanerRedListener> listeners;
    private volatile boolean escaneoEnCurso = false;
    private Future<?> tareaEscaneo;
    
    private final AtomicInteger ipsEscaneadas = new AtomicInteger(0);
    private final AtomicInteger ipsActivas = new AtomicInteger(0);
    private long tiempoInicioEscaneo;

    public interface EscanerRedListener {
        void onEscaneoIniciado(int totalIps);
        void onHostDescubierto(Dispositivo dispositivo);
        void onProgreso(int completados, int total, double porcentaje);
        void onErrorEscaneo(String ip, String error);
        void onEscaneoCompletado(int totalActivos, long duracionMs);
        void onEscaneoCancelado();
    }

    public EscanerRed(String ipInicio, String ipFin, int tiempoEsperaMs) {
        this(ipInicio, ipFin, tiempoEsperaMs, MAX_HILOS);
    }
    
    public EscanerRed(String ipInicio, String ipFin, int tiempoEsperaMs, int maxHilos) {
        if (!ValidadorIP.esIPValida(ipInicio) || !ValidadorIP.esIPValida(ipFin)) {
            throw new IllegalArgumentException("Rango de IP inválido");
        }
        
        this.ipInicio = ipInicio;
        this.ipFin = ipFin;
        this.tiempoEsperaMs = Math.max(100, tiempoEsperaMs);
        this.maxHilos = Math.max(1, Math.min(maxHilos, MAX_HILOS));
        this.listeners = new CopyOnWriteArrayList<>();
    }

    public void agregarListener(EscanerRedListener listener) {
        listeners.add(listener);
    }

    public void removerListener(EscanerRedListener listener) {
        listeners.remove(listener);
    }

    public void iniciarEscaneo() {
        if (escaneoEnCurso) {
            throw new IllegalStateException("Escaneo ya en curso");
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        tareaEscaneo = executor.submit(this::ejecutarEscaneo);
        executor.shutdown();
    }

    public void cancelarEscaneo() {
        if (escaneoEnCurso && tareaEscaneo != null) {
            tareaEscaneo.cancel(true);
            escaneoEnCurso = false;
            notifyEscaneoCancelado();
        }
    }

    public boolean isEscaneoEnCurso() {
        return escaneoEnCurso;
    }

    private void ejecutarEscaneo() {
        escaneoEnCurso = true;
        ipsEscaneadas.set(0);
        ipsActivas.set(0);
        
        try {
            long inicio = ValidadorIP.ipToLong(ipInicio);
            long fin = ValidadorIP.ipToLong(ipFin);
            
            if (fin < inicio) {
                long temp = inicio;
                inicio = fin;
                fin = temp;
            }

            int totalIps = (int) (fin - inicio + 1);
            tiempoInicioEscaneo = System.currentTimeMillis();
            
            notifyEscaneoIniciado(totalIps);

            ExecutorService executor = Executors.newFixedThreadPool(maxHilos);
            CompletionService<Dispositivo> completionService = 
                new ExecutorCompletionService<>(executor);

            // Enviar todas las tareas de escaneo
            for (long ip = inicio; ip <= fin; ip++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                
                final String ipStr = ValidadorIP.longToIp(ip);
                completionService.submit(() -> escanearDireccionIP(ipStr));
            }

            // Recolectar resultados
            int completados = 0;
            for (int i = 0; i < totalIps; i++) {
                if (Thread.currentThread().isInterrupted()) {
                    break;
                }
                
                try {
                    Future<Dispositivo> future = completionService.take();
                    Dispositivo dispositivo = future.get();
                    
                    completados++;
                    ipsEscaneadas.incrementAndGet();
                    
                    if (dispositivo != null && dispositivo.estaEnLinea()) {
                        ipsActivas.incrementAndGet();
                        notifyHostDescubierto(dispositivo);
                    }
                    
                    double porcentaje = (completados * 100.0) / totalIps;
                    notifyProgreso(completados, totalIps, porcentaje);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (ExecutionException e) {
                    notifyErrorEscaneo("IP_" + i, e.getCause().getMessage());
                }
            }

            executor.shutdownNow();
            
            if (!Thread.currentThread().isInterrupted()) {
                long duracion = System.currentTimeMillis() - tiempoInicioEscaneo;
                notifyEscaneoCompletado(ipsActivas.get(), duracion);
            }
            
        } finally {
            escaneoEnCurso = false;
        }
    }

    private Dispositivo escanearDireccionIP(String ip) {
        try {
            long inicio = System.currentTimeMillis();
            InetAddress direccion = InetAddress.getByName(ip);
            boolean alcanzable = direccion.isReachable(tiempoEsperaMs);
            long fin = System.currentTimeMillis();
            
            String nombreHost = "";
            try {
                nombreHost = direccion.getCanonicalHostName();
                if (nombreHost.equals(ip)) {
                    nombreHost = "";
                }
            } catch (Exception e) {
                // Ignorar errores de resolución de nombre
            }
            
            return new Dispositivo(ip, nombreHost, alcanzable, alcanzable ? (fin - inicio) : 0);
            
        } catch (Exception e) {
            notifyErrorEscaneo(ip, e.getMessage());
            return new Dispositivo(ip, "", false, 0);
        }
    }

    private void notifyEscaneoIniciado(int totalIps) {
        for (EscanerRedListener listener : listeners) {
            listener.onEscaneoIniciado(totalIps);
        }
    }

    private void notifyHostDescubierto(Dispositivo dispositivo) {
        for (EscanerRedListener listener : listeners) {
            listener.onHostDescubierto(dispositivo);
        }
    }

    private void notifyProgreso(int completados, int total, double porcentaje) {
        for (EscanerRedListener listener : listeners) {
            listener.onProgreso(completados, total, porcentaje);
        }
    }

    private void notifyErrorEscaneo(String ip, String error) {
        for (EscanerRedListener listener : listeners) {
            listener.onErrorEscaneo(ip, error);
        }
    }

    private void notifyEscaneoCompletado(int totalActivos, long duracionMs) {
        for (EscanerRedListener listener : listeners) {
            listener.onEscaneoCompletado(totalActivos, duracionMs);
        }
    }

    private void notifyEscaneoCancelado() {
        for (EscanerRedListener listener : listeners) {
            listener.onEscaneoCancelado();
        }
    }
}