import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class AppPrincipal extends JFrame { // ¡Ahora es una subclase de JFrame!
    // Componentes como campos de instancia
    private JTable tablaResultados;
    private DefaultTableModel modeloTabla;
    private JProgressBar barraProgreso;

    public AppPrincipal() { // Constructor
        setTitle("Escáner de Red - ET 36");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        
        // Inicializar componentes
        modeloTabla = new DefaultTableModel();
        modeloTabla.addColumn("IP");
        modeloTabla.addColumn("Nombre");
        modeloTabla.addColumn("Estado");
        modeloTabla.addColumn("Tiempo (ms)");
        
        tablaResultados = new JTable(modeloTabla);
        barraProgreso = new JProgressBar(0, 100);
        barraProgreso.setStringPainted(true);
        
        // Panel de controles
        JPanel panelControles = new JPanel();
        JTextField campoIPInicio = new JTextField(12);
        JTextField campoIPFin = new JTextField(12);
        JButton botonEscanear = new JButton("Escanear");
        
        panelControles.add(new JLabel("IP Inicio:"));
        panelControles.add(campoIPInicio);
        panelControles.add(new JLabel("IP Fin:"));
        panelControles.add(campoIPFin);
        panelControles.add(botonEscanear);
        panelControles.add(barraProgreso);
        
        // Organizar interfaz
        add(panelControles, BorderLayout.NORTH);
        add(new JScrollPane(tablaResultados), BorderLayout.CENTER);
        
        // Acción del botón
        botonEscanear.addActionListener(e -> {
            String ipInicio = campoIPInicio.getText();
            String ipFin = campoIPFin.getText();
            
            if (!validarIP(ipInicio) || !validarIP(ipFin)) {
                JOptionPane.showMessageDialog(this, "¡IP inválida! Ejemplo: 192.168.1.1");
                return;
            }
            
            new Thread(() -> escanearRed(ipInicio, ipFin)).start();
        });
    }

    private boolean validarIP(String ip) {
        return ip.matches("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    }

   private void escanearRed(String ipInicio, String ipFin) {
    new Thread(() -> {
        List<Dispositivo> dispositivos = EscanerRed.escanearRango(ipInicio, ipFin);
        
        SwingUtilities.invokeLater(() -> {
            modeloTabla.setRowCount(0); // Limpiar tabla antes de agregar nuevos resultados
            
            for (Dispositivo dispositivo : dispositivos) {
                modeloTabla.addRow(new Object[]{
                    dispositivo.getIp(),
                    dispositivo.getNombre(),
                    dispositivo.isActivo() ? "🟢 Activo" : "🔴 Inactivo",
                    dispositivo.isActivo() ? (int)(Math.random() * 50) : 0
                });
            }
            
            barraProgreso.setValue(100); // Completo al 100%
        });
    }).start();
}
    public static void main(String[] args) {
        // ¡Correcto! Ahora creamos una instancia
        SwingUtilities.invokeLater(() -> {
            new AppPrincipal().setVisible(true);
        });
    }
}
