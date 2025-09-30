package vista;

import controlador.EscanerRed;
import controlador.ConfiguracionEscaneo;
import modelos.Dispositivo;
import controlador.ValidadorIP;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Ventana principal del escáner de red con interfaz profesional
 */
public class VentanaEscaneoRed extends JFrame {
    private final ConfiguracionEscaneo configuracion;
    private EscanerRed escaner;
    
    // Componentes de UI
    private JTextField campoIpInicio, campoIpFin;
    private JSpinner spinnerTimeout;
    private JButton botonIniciar, botonDetener, botonLimpiar;
    private JProgressBar barraProgreso;
    private JLabel etiquetaEstadisticas;
    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    
    // Colores personalizados
    private static final Color GRIS_FONDO = new Color(50, 50, 50);      // Fondo gris oscuro
    private static final Color GRIS_CLARO = new Color(65, 65, 65);      // Fondo de componentes (paneles, campos, tabla)
    private static final Color BLANCO_TEXTO = Color.WHITE;             // Color de texto
    private static final Color ROSA_PROGRESO = new Color(255, 105, 180); // Rosa fuerte para la barra de progreso

    // Métricas
    private final AtomicInteger dispositivosActivos;
    private long tiempoInicioEscaneo;

    public VentanaEscaneoRed() {
        this.configuracion = new ConfiguracionEscaneo();
        this.dispositivosActivos = new AtomicInteger(0);
        inicializarVentana();
        cargarConfiguracion();
    }

    private void inicializarVentana() {
        setTitle("Escáner de Red Profesional - ET 36");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        // 1. CONFIGURACIÓN DEL FONDO PRINCIPAL
        getContentPane().setBackground(GRIS_FONDO);
        
        // Configurar layout principal
        setLayout(new BorderLayout(10, 10));
        
        // 2. Barra de Progreso Arriba (BorderLayout.NORTH)
        add(crearBarraProgresoSuperior(), BorderLayout.NORTH);
        
        // 3. Configuración y Botones (BorderLayout.BEFORE_FIRST_LINE, ahora un panel que va al NORTH)
        JPanel panelConfigYBotones = crearPanelConfiguracionYBotones();
        
        // 4. Panel Principal (Center) - Contenedor para Configuración y Tabla
        // Usaremos un Box Layout en el centro para apilar la configuración y la tabla
        JPanel panelContenedor = new JPanel();
        panelContenedor.setLayout(new BoxLayout(panelContenedor, BoxLayout.Y_AXIS));
        panelContenedor.setBackground(GRIS_FONDO);
        
        panelContenedor.add(panelConfigYBotones);
        panelContenedor.add(crearPanelCentral()); // JScrollPane de la tabla
        
        // Usamos BorderLayout.CENTER para el contenedor de configuración y tabla
        add(panelContenedor, BorderLayout.CENTER);
        
        // 5. Estadísticas abajo (BorderLayout.SOUTH)
        // **Este era el componente faltante o mal ubicado que causaba el error**
        add(crearPanelInferior(), BorderLayout.SOUTH);
        
        // Configurar tabla de resultados
        configurarTabla();
    }
    
    // ====================================================================================
    // 1. CREACIÓN DE BARRA DE PROGRESO SUPERIOR
    // ====================================================================================
    private JPanel crearBarraProgresoSuperior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GRIS_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        barraProgreso = new JProgressBar(0, 100);
        barraProgreso.setStringPainted(true);
        
        // Aplicar color rosa y hacer el texto blanco
        barraProgreso.setBackground(GRIS_CLARO);
        barraProgreso.setForeground(ROSA_PROGRESO);
        barraProgreso.setString("0 %");
        
        // Intentar asegurar que el texto sea blanco
        UIManager.put("ProgressBar.selectionForeground", BLANCO_TEXTO);
        UIManager.put("ProgressBar.selectionBackground", BLANCO_TEXTO);
        
        panel.add(barraProgreso, BorderLayout.CENTER);
        return panel;
    }
    
    // ====================================================================================
    // 2. CREACIÓN DEL PANEL DE CONFIGURACIÓN Y BOTONES (AHORA ES UN PANEL INTERNO)
    // ====================================================================================
    private JPanel crearPanelConfiguracionYBotones() {
        // Usamos un JPanel principal con BoxLayout para apilar los componentes
        JPanel panelPrincipal = new JPanel();
        panelPrincipal.setLayout(new BoxLayout(panelPrincipal, BoxLayout.Y_AXIS));
        panelPrincipal.setBackground(GRIS_FONDO);
        
        // Panel de configuración (IPs y Timeout)
        JPanel panelConfiguracion = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelConfiguracion.setBackground(GRIS_FONDO);
        
        // Panel de botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panelBotones.setBackground(GRIS_FONDO);
        
        // Modificación: Las etiquetas y campos de texto ahora tienen el fondo gris y el texto blanco
        // IP Inicio
        JLabel labelIpInicio = new JLabel("IP Inicio:");
        labelIpInicio.setForeground(BLANCO_TEXTO);
        panelConfiguracion.add(labelIpInicio);
        campoIpInicio = crearCampoTextoEstiloOscuro(12);
        panelConfiguracion.add(campoIpInicio);
        
        // IP Fin
        JLabel labelIpFin = new JLabel("IP Fin:");
        labelIpFin.setForeground(BLANCO_TEXTO);
        panelConfiguracion.add(labelIpFin);
        campoIpFin = crearCampoTextoEstiloOscuro(12);
        panelConfiguracion.add(campoIpFin);
        
        // Timeout
        JLabel labelTimeout = new JLabel("Timeout (ms):");
        labelTimeout.setForeground(BLANCO_TEXTO);
        panelConfiguracion.add(labelTimeout);
        spinnerTimeout = new JSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
        spinnerTimeout.setPreferredSize(new Dimension(80, 24));
        // Estilo del Spinner
        spinnerTimeout.setBackground(GRIS_CLARO);
        spinnerTimeout.getEditor().getComponent(0).setBackground(GRIS_CLARO);
        spinnerTimeout.getEditor().getComponent(0).setForeground(BLANCO_TEXTO);
        panelConfiguracion.add(spinnerTimeout);
        
        // Título del Panel de Configuración
        panelPrincipal.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BLANCO_TEXTO), // Borde blanco
            "Configuración de Escaneo", 
            javax.swing.border.TitledBorder.LEFT, 
            javax.swing.border.TitledBorder.TOP, 
            new Font("SansSerif", Font.BOLD, 12), 
            BLANCO_TEXTO // Texto del título en blanco
        ));

        // Botones (Manteniendo colores originales, pero adaptando el fondo del panel)
        botonIniciar = crearBoton("Iniciar Escaneo", new Color(34, 139, 34), Color.WHITE);
        botonDetener = crearBoton("Detener", new Color(220, 53, 69), Color.WHITE);
        botonLimpiar = crearBoton("Limpiar", GRIS_FONDO.brighter(), BLANCO_TEXTO);
        JButton botonNetstat = crearBoton("Abrir NetStat", new Color(0, 123, 255), Color.WHITE);
        
        botonDetener.setEnabled(false);
        
        panelBotones.add(botonIniciar);
        panelBotones.add(botonDetener);
        panelBotones.add(botonLimpiar);
        panelBotones.add(botonNetstat);
        
        // Action Listeners (se mantienen)
        botonIniciar.addActionListener(this::iniciarEscaneo);
        botonDetener.addActionListener(e -> detenerEscaneo());
        botonLimpiar.addActionListener(e -> limpiarResultados());
        botonNetstat.addActionListener(e -> abrirVentanaNetStat());
        
        panelPrincipal.add(panelConfiguracion);
        panelPrincipal.add(panelBotones);
        
        return panelPrincipal;
    }

    // Helper para crear un JButton con estilos
    private JButton crearBoton(String texto, Color fondo, Color textoColor) {
        JButton boton = new JButton(texto);
        boton.setBackground(fondo);
        boton.setForeground(textoColor);
        boton.setFocusPainted(false);
        return boton;
    }

    // Helper para crear un JTextField con estilos oscuros
    private JTextField crearCampoTextoEstiloOscuro(int columnas) {
        JTextField campo = new JTextField(columnas);
        campo.setBackground(GRIS_CLARO);
        campo.setForeground(BLANCO_TEXTO);
        campo.setCaretColor(BLANCO_TEXTO); // Color del cursor
        campo.setBorder(BorderFactory.createLineBorder(GRIS_FONDO.brighter()));
        return campo;
    }
    
    // ====================================================================================
    // 3. CREACIÓN DEL PANEL CENTRAL DE RESULTADOS (JScrollPane con la tabla)
    // ====================================================================================
    private JScrollPane crearPanelCentral() {
        // Modelo de tabla (sin cambios)
        String[] columnas = {"IP", "Nombre Host", "Estado", "Tiempo Respuesta (ms)", "Timestamp"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        tablaResultados = new JTable(modeloTabla);
        
        // Estilo de la tabla (Fondo gris, texto blanco)
        tablaResultados.setBackground(GRIS_CLARO);
        tablaResultados.setForeground(BLANCO_TEXTO);
        tablaResultados.setGridColor(GRIS_FONDO); // Líneas de la cuadrícula
        tablaResultados.setSelectionBackground(new Color(100, 100, 100)); // Selección oscura
        tablaResultados.setSelectionForeground(BLANCO_TEXTO);

        // Estilo del encabezado de la tabla (Header)
        tablaResultados.getTableHeader().setBackground(GRIS_FONDO);
        tablaResultados.getTableHeader().setForeground(BLANCO_TEXTO);
        tablaResultados.getTableHeader().setFont(tablaResultados.getTableHeader().getFont().deriveFont(Font.BOLD));
        
        tablaResultados.setAutoCreateRowSorter(true);
        tablaResultados.setFillsViewportHeight(true);
        
        // Renderizado personalizado para estados (se adapta para fondo oscuro)
        tablaResultados.getColumnModel().getColumn(2).setCellRenderer(new EstadoCellRenderer());
        
        JScrollPane scrollPane = new JScrollPane(tablaResultados);
        scrollPane.setBackground(GRIS_FONDO);
        scrollPane.getViewport().setBackground(GRIS_CLARO); // Fondo visible detrás de la tabla
        
        // Título del Panel de Resultados
        scrollPane.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BLANCO_TEXTO),
            "Resultados del Escaneo", 
            javax.swing.border.TitledBorder.LEFT, 
            javax.swing.border.TitledBorder.TOP, 
            new Font("SansSerif", Font.BOLD, 12), 
            BLANCO_TEXTO
        ));
        
        return scrollPane;
    }

    // ====================================================================================
    // 4. CREACIÓN DEL PANEL INFERIOR DE ESTADÍSTICAS (Solo la etiqueta)
    // ====================================================================================
    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GRIS_FONDO);
        panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Padding para margen inferior

        // Etiqueta de estadísticas (Aquí se inicializa la variable)
        etiquetaEstadisticas = new JLabel("Listo para escanear"); 
        etiquetaEstadisticas.setForeground(BLANCO_TEXTO);
        
        panel.add(etiquetaEstadisticas, BorderLayout.WEST);
        
        return panel;
    }
    
    // *** El resto de los métodos se mantienen igual ***
    
    private void configurarTabla() {
        // Configurar anchos de columnas
        tablaResultados.getColumnModel().getColumn(0).setPreferredWidth(120); // IP
        tablaResultados.getColumnModel().getColumn(1).setPreferredWidth(200); // Host
        tablaResultados.getColumnModel().getColumn(2).setPreferredWidth(80);  // Estado
        tablaResultados.getColumnModel().getColumn(3).setPreferredWidth(120); // Tiempo
        tablaResultados.getColumnModel().getColumn(4).setPreferredWidth(150); // Timestamp
    }

    private void iniciarEscaneo(ActionEvent e) {
        String ipInicio = campoIpInicio.getText().trim();
        String ipFin = campoIpFin.getText().trim();
        int timeout = (Integer) spinnerTimeout.getValue();
        
        // Validar IPs
        if (!ValidadorIP.esIPValida(ipInicio) || !ValidadorIP.esIPValida(ipFin)) {
            JOptionPane.showMessageDialog(this, 
                "Por favor ingrese direcciones IP válidas", 
                "Error de Validación", 
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        // Guardar configuración
        guardarConfiguracion();
        
        // Preparar UI
        botonIniciar.setEnabled(false);
        botonDetener.setEnabled(true);
        dispositivosActivos.set(0);
        tiempoInicioEscaneo = System.currentTimeMillis();
        // ESTA LÍNEA AHORA FUNCIONARÁ
        etiquetaEstadisticas.setText("Escaneo en progreso...");
        
        // Crear y configurar escáner
        escaner = new EscanerRed(ipInicio, ipFin, timeout);
        escaner.agregarListener(new EscanerRed.EscanerRedListener() {
            @Override
            public void onEscaneoIniciado(int totalIps) {
                SwingUtilities.invokeLater(() -> {
                    barraProgreso.setValue(0);
                    etiquetaEstadisticas.setText(String.format(
                        "Escaneando %d direcciones IP...", totalIps));
                });
            }

            @Override
            public void onHostDescubierto(Dispositivo dispositivo) {
                SwingUtilities.invokeLater(() -> {
                    dispositivosActivos.incrementAndGet();
                    agregarFilaTabla(dispositivo);
                    actualizarEstadisticas();
                });
            }

            @Override
            public void onProgreso(int completados, int total, double porcentaje) {
                SwingUtilities.invokeLater(() -> {
                    barraProgreso.setValue((int) porcentaje);
                    barraProgreso.setString(String.format("%.1f%%", porcentaje)); // Actualizar texto de progreso
                    etiquetaEstadisticas.setText(String.format(
                        "Progreso: %d/%d (%.1f%%) - Activos: %d", 
                        completados, total, porcentaje, dispositivosActivos.get()));
                });
            }

            @Override
            public void onErrorEscaneo(String ip, String error) {
                System.err.println("Error escaneando " + ip + ": " + error);
            }

            @Override
            public void onEscaneoCompletado(int totalActivos, long duracionMs) {
                SwingUtilities.invokeLater(() -> {
                    finalizarEscaneo(totalActivos, duracionMs);
                });
            }

            @Override
            public void onEscaneoCancelado() {
                SwingUtilities.invokeLater(() -> {
                    etiquetaEstadisticas.setText("Escaneo cancelado por el usuario");
                    botonIniciar.setEnabled(true);
                    botonDetener.setEnabled(false);
                });
            }
        });
        
        // Ejecutar en hilo separado
        new Thread(() -> escaner.iniciarEscaneo(), "Escaneo-Thread").start();
    }

    private void detenerEscaneo() {
        if (escaner != null && escaner.isEscaneoEnCurso()) {
            escaner.cancelarEscaneo();
        }
    }

    private void limpiarResultados() {
        modeloTabla.setRowCount(0);
        dispositivosActivos.set(0);
        barraProgreso.setValue(0);
        barraProgreso.setString("0 %");
        etiquetaEstadisticas.setText("Resultados limpiados");
    }

    private void abrirVentanaNetStat() {
        SwingUtilities.invokeLater(() -> {
            VentanaNetStat ventanaNetStat = new VentanaNetStat();
            // Para que la VentanaNetStat también tenga el fondo gris
            ventanaNetStat.getContentPane().setBackground(GRIS_FONDO);
            ventanaNetStat.setVisible(true);
        });
    }

    private void agregarFilaTabla(Dispositivo dispositivo) {
        Object[] fila = {
            dispositivo.getDireccionIP(),
            dispositivo.getNombreHost(),
            dispositivo.estaEnLinea() ? "ACTIVO" : "INACTIVO",
            dispositivo.getTiempoRespuesta(),
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))
        };
        modeloTabla.addRow(fila);
    }

    private void actualizarEstadisticas() {
        // Actualizado en onProgreso
    }

    private void finalizarEscaneo(int totalActivos, long duracionMs) {
        botonIniciar.setEnabled(true);
        botonDetener.setEnabled(false);
        
        double duracionSegundos = duracionMs / 1000.0;
        etiquetaEstadisticas.setText(String.format(
            "Escaneo completado - %d dispositivos activos encontrados en %.2f segundos",
            totalActivos, duracionSegundos));
        
        barraProgreso.setValue(100);
        barraProgreso.setString("100 %");
        
        JOptionPane.showMessageDialog(this,
            String.format("Escaneo completado!\n\nDispositivos activos: %d\nDuración: %.2f segundos",
                totalActivos, duracionSegundos),
            "Escaneo Finalizado",
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void cargarConfiguracion() {
        String ipBase = configuracion.cargarIpBase();
        campoIpInicio.setText(ipBase + "1");
        campoIpFin.setText(ipBase + "254");
        spinnerTimeout.setValue(configuracion.cargarTiempoEspera());
    }

    private void guardarConfiguracion() {
        String ipInicio = campoIpInicio.getText().trim();
        String ipBase = ipInicio.substring(0, ipInicio.lastIndexOf('.') + 1);
        
        configuracion.guardarIpBase(ipBase);
        configuracion.guardarTiempoEspera((Integer) spinnerTimeout.getValue());
    }

    // Renderer personalizado para columna de estado (adaptado a fondo oscuro)
    private static class EstadoCellRenderer extends DefaultTableCellRenderer {
        private static final Color FONDO_OSCURO_ACTIVO = new Color(80, 120, 80); // Fondo verde oscuro suave
        private static final Color FONDO_OSCURO_INACTIVO = new Color(120, 80, 80); // Fondo rojo oscuro suave

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            // Revertir colores por defecto del renderer a los colores oscuros de la tabla
            c.setBackground(VentanaEscaneoRed.GRIS_CLARO);
            c.setForeground(VentanaEscaneoRed.BLANCO_TEXTO);
            
            if (value != null) {
                String estado = value.toString();
                if ("ACTIVO".equals(estado)) {
                    // Si el estado es ACTIVO, aplicamos un color de fondo diferente
                    // y un texto que contraste bien con el fondo oscuro general
                    c.setBackground(FONDO_OSCURO_ACTIVO);
                    c.setForeground(Color.WHITE); 
                } else if ("INACTIVO".equals(estado)) {
                    c.setBackground(FONDO_OSCURO_INACTIVO);
                    c.setForeground(Color.WHITE);
                }
            }
            
            if (isSelected) {
                // Mantener los colores de selección de la tabla
                c.setBackground(table.getSelectionBackground());
                c.setForeground(table.getSelectionForeground());
            }
            
            setHorizontalAlignment(CENTER);
            return c;
        }
    }
}