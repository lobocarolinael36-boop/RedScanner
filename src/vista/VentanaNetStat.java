package vista;

import controlador.GestorNetStat;
import modelos.ConexionRed;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

/**
 * Ventana especializada para las 3 funciones de NetStat
 */
public class VentanaNetStat extends JFrame {
    private final GestorNetStat gestorNetStat;
    
    // Componentes de UI
    private JButton botonConexionesActivas, botonPuertosEscucha, botonEstadisticas;
    private JTabbedPane panelPestanias;
    private JTextArea areaTexto;
    private JTable tablaConexiones;
    private DefaultTableModel modeloTabla;
    private JProgressBar barraProgreso;
    private JLabel etiquetaEstado;
    
    // Colores personalizados (copia del VentanaEscaneoRed)
    private static final Color GRIS_FONDO = new Color(50, 50, 50);      
    private static final Color GRIS_CLARO = new Color(65, 65, 65);      
    private static final Color BLANCO_TEXTO = Color.WHITE;             
    private static final Color ROSA_PROGRESO = new Color(255, 105, 180);

    public VentanaNetStat() {
        this.gestorNetStat = new GestorNetStat();
        inicializarVentana();
        configurarGestorEventos();
    }

    private void inicializarVentana() {
        setTitle("Analizador NetStat - ET 36");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        
        setLayout(new BorderLayout(10, 10));
        
        // Aplicar fondo gris
        getContentPane().setBackground(GRIS_FONDO);
        
        add(crearPanelSuperior(), BorderLayout.NORTH);
        add(crearPanelCentral(), BorderLayout.CENTER);
        add(crearPanelInferior(), BorderLayout.SOUTH);
    }

    private JPanel crearPanelSuperior() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        // Aplicar fondo gris y borde con texto blanco
        panel.setBackground(GRIS_FONDO);
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BLANCO_TEXTO), 
            "Funciones NetStat",
            javax.swing.border.TitledBorder.LEFT,
            javax.swing.border.TitledBorder.TOP,
            new Font("SansSerif", Font.BOLD, 12),
            BLANCO_TEXTO
        ));
        
        // CORRECCIÓN CLAVE: Asignar a las variables de CLASE (this.)
        botonConexionesActivas = crearBotonEstilizado(
            "Conexiones Activas", new Color(40, 167, 69));
        botonPuertosEscucha = crearBotonEstilizado(
            "Puertos en Escucha", new Color(0, 123, 255));
        botonEstadisticas = crearBotonEstilizado(
            "Estadísticas", new Color(108, 117, 125));
        
        // Tooltips informativos (Ahora funcionan porque las variables no son NULL)
        botonConexionesActivas.setToolTipText("Muestra todas las conexiones de red establecidas");
        botonPuertosEscucha.setToolTipText("Muestra puertos locales en espera de conexiones");
        botonEstadisticas.setToolTipText("Muestra estadísticas de protocolos de red");
        
        panel.add(botonConexionesActivas);
        panel.add(botonPuertosEscucha);
        panel.add(botonEstadisticas);
        
        return panel;
    }

    private JButton crearBotonEstilizado(String texto, Color colorFondo) {
        JButton boton = new JButton(texto);
        boton.setBackground(colorFondo);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorFondo.darker()),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        return boton;
    }

    private JTabbedPane crearPanelCentral() {
        panelPestanias = new JTabbedPane();
        // Aplicar fondo gris y texto blanco a las pestañas
        panelPestanias.setBackground(GRIS_FONDO);
        panelPestanias.setForeground(BLANCO_TEXTO);
        
        // Pestaña 1: Tabla de conexiones
        JPanel pestaniaTabla = new JPanel(new BorderLayout());
        pestaniaTabla.setBackground(GRIS_CLARO); // Fondo de la pestaña
        modeloTabla = new DefaultTableModel(new String[]{
            "Protocolo", "Dirección Local", "Puerto Local", 
            "Dirección Remota", "Puerto Remoto", "Estado", "PID", "Proceso"
        }, 0);
        
        tablaConexiones = new JTable(modeloTabla);
        tablaConexiones.setAutoCreateRowSorter(true);
        tablaConexiones.setFillsViewportHeight(true);
        
        // Estilo de la tabla
        tablaConexiones.setBackground(GRIS_CLARO);
        tablaConexiones.setForeground(BLANCO_TEXTO);
        tablaConexiones.setGridColor(GRIS_FONDO);
        tablaConexiones.getTableHeader().setBackground(GRIS_FONDO);
        tablaConexiones.getTableHeader().setForeground(BLANCO_TEXTO);
        
        // Configurar anchos de columnas
        tablaConexiones.getColumnModel().getColumn(0).setPreferredWidth(60);  // Protocolo
        tablaConexiones.getColumnModel().getColumn(1).setPreferredWidth(120); // Dir Local
        tablaConexiones.getColumnModel().getColumn(2).setPreferredWidth(80);  // Puerto Local
        tablaConexiones.getColumnModel().getColumn(3).setPreferredWidth(120); // Dir Remota
        tablaConexiones.getColumnModel().getColumn(4).setPreferredWidth(80);  // Puerto Remoto
        tablaConexiones.getColumnModel().getColumn(5).setPreferredWidth(100); // Estado
        tablaConexiones.getColumnModel().getColumn(6).setPreferredWidth(60);  // PID
        tablaConexiones.getColumnModel().getColumn(7).setPreferredWidth(150); // Proceso
        
        JScrollPane scrollPaneTabla = new JScrollPane(tablaConexiones);
        scrollPaneTabla.getViewport().setBackground(GRIS_CLARO);
        pestaniaTabla.add(scrollPaneTabla, BorderLayout.CENTER);
        panelPestanias.addTab("Conexiones", pestaniaTabla);
        
        // Pestaña 2: Área de texto para estadísticas
        JPanel pestaniaTexto = new JPanel(new BorderLayout());
        pestaniaTexto.setBackground(GRIS_CLARO); // Fondo de la pestaña
        areaTexto = new JTextArea();
        areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaTexto.setEditable(false);
        // Estilo del área de texto
        areaTexto.setBackground(GRIS_CLARO);
        areaTexto.setForeground(BLANCO_TEXTO);
        
        JScrollPane scrollPaneTexto = new JScrollPane(areaTexto);
        scrollPaneTexto.getViewport().setBackground(GRIS_CLARO);
        pestaniaTexto.add(scrollPaneTexto, BorderLayout.CENTER);
        panelPestanias.addTab("Estadísticas", pestaniaTexto);
        
        return panelPestanias;
    }

    private JPanel crearPanelInferior() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(GRIS_FONDO);
        
        barraProgreso = new JProgressBar();
        barraProgreso.setStringPainted(true);
        barraProgreso.setVisible(false);
        // Aplicar color rosa
        barraProgreso.setForeground(ROSA_PROGRESO);
        barraProgreso.setBackground(GRIS_CLARO);
        
        etiquetaEstado = new JLabel("Seleccione una función NetStat");
        etiquetaEstado.setForeground(BLANCO_TEXTO);
        etiquetaEstado.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        panel.add(barraProgreso, BorderLayout.CENTER);
        panel.add(etiquetaEstado, BorderLayout.SOUTH);
        
        return panel;
    }

    private void configurarGestorEventos() {
        botonConexionesActivas.addActionListener(e -> ejecutarFuncionNetStat(1));
        botonPuertosEscucha.addActionListener(e -> ejecutarFuncionNetStat(2));
        botonEstadisticas.addActionListener(e -> ejecutarFuncionNetStat(3));
        
        gestorNetStat.agregarListener(new GestorNetStat.NetStatListener() {
            @Override
            public void onNetStatIniciado(String funcion) {
                SwingUtilities.invokeLater(() -> {
                    barraProgreso.setVisible(true);
                    barraProgreso.setIndeterminate(true);
                    etiquetaEstado.setText("Ejecutando " + obtenerNombreFuncion(funcion) + "...");
                    habilitarBotones(false);
                });
            }

            @Override
            public void onConexionEncontrada(ConexionRed conexion) {
                // No actualiza la tabla aquí para evitar sobrecarga de UI
            }

            @Override
            public void onNetStatCompletado(String funcion, int totalElementos) {
                SwingUtilities.invokeLater(() -> {
                    barraProgreso.setVisible(false);
                    barraProgreso.setIndeterminate(false);
                    
                    // CORRECCIÓN: Si es Estadísticas, el total se manejará en mostrarEstadisticas()
                    String mensaje;
                    if (funcion.equals("ESTADISTICAS_PROTOCOLOS")) {
                        mensaje = String.format("%s completado.", obtenerNombreFuncion(funcion));
                    } else {
                        mensaje = String.format("%s completado - %d elementos encontrados",
                            obtenerNombreFuncion(funcion), totalElementos);
                    }
                    etiquetaEstado.setText(mensaje);
                    habilitarBotones(true);
                });
            }

            @Override
            public void onErrorNetStat(String error) {
                SwingUtilities.invokeLater(() -> {
                    barraProgreso.setVisible(false);
                    etiquetaEstado.setText("Error: " + error);
                    habilitarBotones(true);
                    JOptionPane.showMessageDialog(VentanaNetStat.this,
                        error, "Error NetStat", JOptionPane.ERROR_MESSAGE);
                });
            }
        });
    }

    private void ejecutarFuncionNetStat(int funcion) {
        new Thread(() -> {
            switch (funcion) {
                case 1 -> mostrarConexionesActivas();
                case 2 -> mostrarPuertosEscucha();
                case 3 -> mostrarEstadisticas();
            }
        }, "NetStat-Thread").start();
    }

    private void mostrarConexionesActivas() {
        List<ConexionRed> conexiones = gestorNetStat.obtenerConexionesActivas();
        SwingUtilities.invokeLater(() -> {
            modeloTabla.setRowCount(0);
            for (ConexionRed conexion : conexiones) {
                modeloTabla.addRow(new Object[]{
                    conexion.getProtocolo(),
                    conexion.getDireccionLocal(),
                    conexion.getPuertoLocal(),
                    conexion.getDireccionRemota(),
                    conexion.getPuertoRemoto(),
                    conexion.getEstado(),
                    conexion.getPid() > 0 ? conexion.getPid() : "N/A",
                    conexion.getNombreProceso()
                });
            }
            panelPestanias.setSelectedIndex(0); // Cambiar a pestaña de tabla
        });
    }

    private void mostrarPuertosEscucha() {
        List<ConexionRed> puertos = gestorNetStat.obtenerPuertosEscucha();
        SwingUtilities.invokeLater(() -> {
            modeloTabla.setRowCount(0);
            for (ConexionRed puerto : puertos) {
                modeloTabla.addRow(new Object[]{
                    puerto.getProtocolo(),
                    puerto.getDireccionLocal(),
                    puerto.getPuertoLocal(),
                    puerto.getDireccionRemota(),
                    puerto.getPuertoRemoto(),
                    puerto.getEstado(),
                    puerto.getPid() > 0 ? puerto.getPid() : "N/A",
                    puerto.getNombreProceso()
                });
            }
            panelPestanias.setSelectedIndex(0);
        });
    }

    private void mostrarEstadisticas() {
        // Obtenemos la salida de texto CRUDA del comando netstat -s
        String salidaCompleta = gestorNetStat.obtenerEstadisticasProtocolosRaw(); 
        
        SwingUtilities.invokeLater(() -> {
            
            if (salidaCompleta != null && !salidaCompleta.trim().isEmpty()) {
                areaTexto.setText(salidaCompleta);
            } else {
                areaTexto.setText("No se obtuvieron estadísticas de protocolos. Verifique los permisos o si el comando 'netstat -s' funciona en su sistema.");
            }
            
            // Ajustamos la etiqueta de estado con el número de caracteres/datos
            int totalChars = (salidaCompleta != null) ? salidaCompleta.length() : 0;
            etiquetaEstado.setText(String.format(
                "Estadísticas de Protocolos completado - %d caracteres de datos", totalChars));
            
            panelPestanias.setSelectedIndex(1); // Cambiar a pestaña de estadísticas
        });
    }

    private String obtenerNombreFuncion(String funcion) {
        return switch (funcion) {
            case "CONEXIONES_ACTIVAS" -> "Conexiones Activas";
            case "PUERTOS_ESPERA" -> "Puertos en Escucha";
            case "ESTADISTICAS_PROTOCOLOS" -> "Estadísticas de Protocolos";
            default -> "Función NetStat";
        };
    }

    private void habilitarBotones(boolean habilitar) {
        botonConexionesActivas.setEnabled(habilitar);
        botonPuertosEscucha.setEnabled(habilitar);
        botonEstadisticas.setEnabled(habilitar);
    }
}