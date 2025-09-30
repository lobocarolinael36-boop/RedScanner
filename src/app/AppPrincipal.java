package app;

import modelos.Dispositivo;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.List;
import java.util.stream.Collectors;

public class AppPrincipal extends JFrame {

    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    private JTextField campoIPInicio, campoIPFin;
    private JProgressBar progressBar;
    private JButton botonEscanear, botonDetener, botonBuscar, botonLimpiar, botonGuardarCSV;
    private JLabel labelResumen;
    private JComboBox<String> comboFiltro;
    private JSpinner spinnerTimeoutPing, spinnerTimeoutPuerto, spinnerHilos;

    private List<Dispositivo> dispositivosEscaneados;

    public AppPrincipal() {
    	
        setTitle("Escáner de Red Avanzado");
        setSize(1000, 580);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Modelo de tabla
        modeloTabla = new DefaultTableModel() {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 2 ? Boolean.class : String.class;
            }
        };
        modeloTabla.addColumn("IP");
        modeloTabla.addColumn("Nombre");
        modeloTabla.addColumn("Activo");
        modeloTabla.addColumn("Tiempo (ms)");
        modeloTabla.addColumn("Puertos Abiertos");

        tablaResultados = new JTable(modeloTabla);
        tablaResultados.setAutoCreateRowSorter(true);
        tablaResultados.setFillsViewportHeight(true);

        initComponents();
        setupLayout();
    }

    private void initComponents() {
        campoIPInicio = new JTextField(14);
        campoIPFin = new JTextField(14);

        // Valores por defecto típicos de LAN
        campoIPInicio.setText("192.168.1.1");
        campoIPFin.setText("192.168.1.254");

        // Controles de configuración
        spinnerTimeoutPing = new JSpinner(new SpinnerNumberModel(1000, 100, 10000, 100));
        spinnerTimeoutPuerto = new JSpinner(new SpinnerNumberModel(500, 100, 10000, 100));
        spinnerHilos = new JSpinner(new SpinnerNumberModel(10, 1, 200, 1));

        botonEscanear = new JButton("Escanear");
        botonDetener = new JButton("Detener");
        botonDetener.setEnabled(false);

        botonGuardarCSV = new JButton("Guardar CSV");
        botonLimpiar = new JButton("Limpiar");
        botonBuscar = new JButton("Buscar");

        progressBar = new JProgressBar(0, 100);
        progressBar.setVisible(false);

        labelResumen = new JLabel("Equipos: 0 | Activos: 0");
        comboFiltro = new JComboBox<>(new String[]{"Todos", "Activos", "Inactivos"});

        // Listeners
        botonEscanear.addActionListener(e -> escanearRed());
        botonDetener.addActionListener(e -> detenerEscaneo());
        botonGuardarCSV.addActionListener(this::guardarCSV);
        botonLimpiar.addActionListener(e -> limpiarBusqueda());
        botonBuscar.addActionListener(e -> buscarDispositivo());
        comboFiltro.addActionListener(e -> aplicarFiltro());

        // Validación “en vivo” mientras se escribe
        campoIPInicio.getDocument().addDocumentListener((SimpleDocumentListener) e -> marcarValidezCampo(campoIPInicio));
        campoIPFin.getDocument().addDocumentListener((SimpleDocumentListener) e -> marcarValidezCampo(campoIPFin));
    }

    private void setupLayout() {
        JPanel panelSuperior = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Fila 1 — Rango de IP
        gbc.gridx = 0; gbc.gridy = 0;
        panelSuperior.add(new JLabel("IP Inicio:"), gbc);
        gbc.gridx = 1;
        panelSuperior.add(campoIPInicio, gbc);
        gbc.gridx = 2;
        panelSuperior.add(new JLabel("IP Fin:"), gbc);
        gbc.gridx = 3;
        panelSuperior.add(campoIPFin, gbc);

        // Fila 2 — Configuraciones
        gbc.gridx = 0; gbc.gridy = 1;
        panelSuperior.add(new JLabel("Timeout ping (ms):"), gbc);
        gbc.gridx = 1;
        panelSuperior.add(spinnerTimeoutPing, gbc);
        gbc.gridx = 2;
        panelSuperior.add(new JLabel("Timeout puerto (ms):"), gbc);
        gbc.gridx = 3;
        panelSuperior.add(spinnerTimeoutPuerto, gbc);

        // Fila 3 — Hilos / Filtro / Progreso
        gbc.gridx = 0; gbc.gridy = 2;
        panelSuperior.add(new JLabel("Hilos:"), gbc);
        gbc.gridx = 1;
        panelSuperior.add(spinnerHilos, gbc);
        gbc.gridx = 2;
        panelSuperior.add(new JLabel("Filtrar:"), gbc);
        gbc.gridx = 3;
        panelSuperior.add(comboFiltro, gbc);

        // Progreso ocupa toda la fila 4
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 4;
        panelSuperior.add(progressBar, gbc);
        gbc.gridwidth = 1;

        // Botones
        JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panelBotones.add(botonEscanear);
        panelBotones.add(botonDetener);
        panelBotones.add(botonGuardarCSV);
        panelBotones.add(botonBuscar);
        panelBotones.add(botonLimpiar);

        // Inferior: botones + resumen (evitamos usar SOUTH y PAGE_END por separado)
        JPanel panelInferior = new JPanel(new BorderLayout());
        panelInferior.add(panelBotones, BorderLayout.WEST);
        JPanel panelResumen = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        panelResumen.add(labelResumen);
        panelInferior.add(panelResumen, BorderLayout.EAST);

        setLayout(new BorderLayout(10, 10));
        add(panelSuperior, BorderLayout.NORTH);
        add(new JScrollPane(tablaResultados), BorderLayout.CENTER);
        add(panelInferior, BorderLayout.SOUTH);
    }

    private void marcarValidezCampo(JTextField campo) {
        String ip = campo.getText().trim();
        boolean ok = validarIP(ip);
        campo.setBackground(ok ? Color.WHITE : new Color(255, 230, 230));
        campo.setToolTipText(ok ? null : "IP inválida (formato: 0-255.0-255.0-255.0-255)");
    }

    private void buscarDispositivo() {
        String ipABuscar = JOptionPane.showInputDialog(this,
                "Ingrese la IP a buscar:",
                "Buscar dispositivo",
                JOptionPane.QUESTION_MESSAGE);
 
        if (ipABuscar == null || ipABuscar.trim().isEmpty()) return;

        if (!validarIP(ipABuscar)) {
            JOptionPane.showMessageDialog(this, "IP inválida", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        botonBuscar.setEnabled(false);

        final int timeoutPing = (int) spinnerTimeoutPing.getValue();
        final int timeoutPuerto = (int) spinnerTimeoutPuerto.getValue();

        new Thread(() -> {
            Dispositivo dispositivo = EscanerRed.escanearDispositivo(ipABuscar, timeoutPing, timeoutPuerto);
            SwingUtilities.invokeLater(() -> {
                modeloTabla.setRowCount(0);
                modeloTabla.addRow(new Object[]{
                        dispositivo.getIp(),
                        dispositivo.getNombre(),
                        dispositivo.isActivo(),
                        dispositivo.isActivo() ? dispositivo.getTiempoRespuesta() + " ms" : "-",
                        dispositivo.getPuertosFormateados()
                });
                progressBar.setIndeterminate(false);
                progressBar.setVisible(false);
                botonBuscar.setEnabled(true);
                actualizarResumenIndividual(dispositivo);
            });
        }).start();
    }

    private void actualizarResumenIndividual(Dispositivo dispositivo) {
        labelResumen.setText(String.format("Equipo: %s | Estado: %s | Tiempo: %s",
                dispositivo.getIp(),
                dispositivo.isActivo() ? "Activo" : "Inactivo",
                dispositivo.isActivo() ? dispositivo.getTiempoRespuesta() + " ms" : "-"));
    }

    private void limpiarBusqueda() {
            modeloTabla.setRowCount(0);
            labelResumen.setText("Equipos: 0 | Activos: 0");
        
    }

    private boolean validarIP(String ip) {
        return EscanerRed.validarIP(ip);
    }

    private void escanearRed() {
        String ipInicio = campoIPInicio.getText().trim();
        String ipFin = campoIPFin.getText().trim();

        if (!validarIP(ipInicio) || !validarIP(ipFin) || !EscanerRed.validarRangoIPs(ipInicio, ipFin)) {
            JOptionPane.showMessageDialog(this, "Rango de IPs inválido", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        modeloTabla.setRowCount(0);
        progressBar.setValue(0);
        progressBar.setVisible(true);
        progressBar.setIndeterminate(false);
        botonEscanear.setEnabled(false);
        botonDetener.setEnabled(true);
        labelResumen.setText("Escaneando...");

        final int hilos = (int) spinnerHilos.getValue();
        final int timeoutPing = (int) spinnerTimeoutPing.getValue();
        final int timeoutPuerto = (int) spinnerTimeoutPuerto.getValue();

        new Thread(() -> {
            try {
                dispositivosEscaneados = EscanerRed.escanearRangoAvanzado(
                        ipInicio, ipFin, hilos, timeoutPing, timeoutPuerto, progressBar
                );

                SwingUtilities.invokeLater(() -> {
                    actualizarTabla(dispositivosEscaneados);
                    actualizarResumen();
                    finalizarEscaneoUI();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(AppPrincipal.this,
                            "Error durante el escaneo: " + e.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    finalizarEscaneoUI();
                });
            }
        }).start();
    }

    private void actualizarTabla(List<Dispositivo> dispositivos) {
        modeloTabla.setRowCount(0);
        for (Dispositivo dispositivo : dispositivos) {
            modeloTabla.addRow(new Object[]{
                    dispositivo.getIp(),
                    dispositivo.getNombre(),
                    dispositivo.isActivo(),
                    dispositivo.isActivo() ? dispositivo.getTiempoRespuesta() + " ms" : "-",
                    dispositivo.getPuertosFormateados()
            });
        }
    }

    private void aplicarFiltro() {
        if (dispositivosEscaneados == null) return;

        String filtro = (String) comboFiltro.getSelectedItem();
        List<Dispositivo> dispositivosFiltrados;

        switch (filtro) {
            case "Activos":
                dispositivosFiltrados = dispositivosEscaneados.stream()
                        .filter(Dispositivo::isActivo)
                        .collect(Collectors.toList());
                break;
            case "Inactivos":
                dispositivosFiltrados = dispositivosEscaneados.stream()
                        .filter(d -> !d.isActivo())
                        .collect(Collectors.toList());
                break;
            default:
                dispositivosFiltrados = dispositivosEscaneados;
        }

        actualizarTabla(dispositivosFiltrados);
    }

    private void actualizarResumen() {
        if (dispositivosEscaneados == null) return;

        long total = dispositivosEscaneados.size();
        long activos = dispositivosEscaneados.stream().filter(Dispositivo::isActivo).count();

        labelResumen.setText(String.format("Equipos: %d | Activos: %d | Inactivos: %d",
                total, activos, total - activos));
    }

    private void detenerEscaneo() {
        EscanerRed.detenerEscaneo();
        finalizarEscaneoUI();
        JOptionPane.showMessageDialog(this,
                "Escaneo detenido por el usuario",
                "Información", JOptionPane.INFORMATION_MESSAGE);
    }

    private void finalizarEscaneoUI() {
        progressBar.setVisible(false);
        botonEscanear.setEnabled(true);
        botonDetener.setEnabled(false);
    }

    private void guardarCSV(ActionEvent event) {
        if (dispositivosEscaneados == null || dispositivosEscaneados.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No hay datos para guardar",
                    "Advertencia", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Guardar resultados como CSV");

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String filePath = fileChooser.getSelectedFile().getAbsolutePath();
            if (!filePath.toLowerCase().endsWith(".csv")) filePath += ".csv";

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                writer.write("IP,Nombre,Activo,Tiempo (ms),Puertos Abiertos\n");
                for (Dispositivo dispositivo : dispositivosEscaneados) {
                    String nombre = dispositivo.getNombre() != null ? dispositivo.getNombre() : "";
                    nombre = nombre.replace("\"", "\"\"");
                    String puertos = dispositivo.getPuertosFormateados().replace("\"", "\"\"");
                    writer.write(String.format("\"%s\",\"%s\",%s,%s,\"%s\"\n",
                            dispositivo.getIp(),
                            nombre,
                            dispositivo.isActivo(),
                            dispositivo.isActivo() ? dispositivo.getTiempoRespuesta() : "",
                            puertos));
                }

                JOptionPane.showMessageDialog(this,
                        "Archivo guardado exitosamente en: " + filePath,
                        "Guardado exitoso", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Error al guardar el archivo: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void main(String[] args) {
    	try {
            // Look and Feel Nimbus
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");

            // Fondo oscuro y letras blancas
            UIManager.put("control", new Color(45, 45, 45)); 
            UIManager.put("info", new Color(60, 60, 60));
            UIManager.put("nimbusBase", new Color(60, 60, 60));
            UIManager.put("nimbusBlueGrey", new Color(80, 80, 80));
            UIManager.put("Button.background", new Color(70, 70, 70));

            // Barra de progreso en rosa hot
            UIManager.put("nimbusOrange", new Color(255, 105, 180)); // rosa fuerte
            UIManager.put("ProgressBar.foreground", new Color(255, 105, 180));
            UIManager.put("ProgressBar.selectionForeground", Color.WHITE);
            UIManager.put("ProgressBar.selectionBackground", Color.WHITE);

        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> new AppPrincipal().setVisible(true));
    }

    // --- Utilidad para document listener sin verbosidad ---
    @FunctionalInterface
    interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update(javax.swing.event.DocumentEvent e);
        @Override default void insertUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void removeUpdate(javax.swing.event.DocumentEvent e) { update(e); }
        @Override default void changedUpdate(javax.swing.event.DocumentEvent e) { update(e); }
    }
}
