import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.util.*;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class BuscarPorPaisFrame extends JFrame {
    private JTextField txtCodigoISO;
    private JTextArea txtResultado;
    private JButton btnBuscar, btnExportar, btnRegresar;
    private JLabel lblTiempoBusqueda;

    private Map<String, String> paises;
    private Map<Integer, String> estados;
    private Map<Integer, Set<String>> ciudadesPorEstado;
    private Map<String, Set<Integer>> estadosPorPais;

    public BuscarPorPaisFrame() {
        setTitle("Buscar por País");
        setSize(600, 400);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        txtCodigoISO = new JTextField();
        txtResultado = new JTextArea();
        lblTiempoBusqueda = new JLabel("Tiempo de búsqueda: ");
        btnBuscar = new JButton("Buscar");
        btnExportar = new JButton("Exportar a Excel");
        btnRegresar = new JButton("Regresar al Menú");

        add(new JLabel("Código ISO del País:"));
        add(txtCodigoISO);
        add(btnBuscar);
        add(new JScrollPane(txtResultado));
        add(lblTiempoBusqueda);
        add(btnExportar);
        add(btnRegresar);

        btnBuscar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buscarPais();
            }
        });

        btnExportar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exportarResultadoAExcel();
            }
        });

        btnRegresar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose(); // Cierra esta ventana
            }
        });

        cargarDatos();
        setVisible(true);
    }

    private void cargarDatos() {
        paises = new HashMap<>();
        estados = new HashMap<>();
        ciudadesPorEstado = new HashMap<>();
        estadosPorPais = new HashMap<>();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://EDOLAP\\SQLEXPRESS:1433;database=paises_bd;encrypt=false", "soporte", "123456")) {
            // Cargar países
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, cod_iso, nombre FROM Paises");
            Map<Integer, String> paisIdToCodIso = new HashMap<>();
            while (rs.next()) {
                int id = rs.getInt("id");
                String codIso = rs.getString("cod_iso");
                String nombre = rs.getString("nombre");
                paises.put(codIso, nombre);
                paisIdToCodIso.put(id, codIso);
                estadosPorPais.put(codIso, new HashSet<>());
            }

            // Cargar estados
            rs = stmt.executeQuery("SELECT id, nombre, pais_id FROM Estados");
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                int paisId = rs.getInt("pais_id");

                estados.put(id, nombre);

                String codIso = paisIdToCodIso.get(paisId);
                if (codIso != null) {
                    estadosPorPais.get(codIso).add(id);
                } else {
                    System.out.println("No se encontró el país con id: " + paisId);
                }

                ciudadesPorEstado.put(id, new HashSet<>());
            }

            // Cargar ciudades
            rs = stmt.executeQuery("SELECT id, nome, estado_id FROM Cidades");
            while (rs.next()) {
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                int estadoId = rs.getInt("estado_id");
                ciudadesPorEstado.get(estadoId).add(nome);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buscarPais() {
        String codigoISO = txtCodigoISO.getText();
        long startTime = System.nanoTime();
        StringBuilder resultado = new StringBuilder();

        if (paises.containsKey(codigoISO)) {
            String pais = paises.get(codigoISO);
            resultado.append("País: ").append(pais).append("\n");
            for (Integer estadoId : estadosPorPais.get(codigoISO)) {
                String estado = estados.get(estadoId);
                resultado.append("  Estado: ").append(estado).append("\n");
                for (String ciudad : ciudadesPorEstado.get(estadoId)) {
                    resultado.append("    Ciudad: ").append(ciudad).append("\n");
                }
            }
        } else {
            resultado.append("No se encontró el país con código ISO: ").append(codigoISO).append("\n");
        }

        long endTime = System.nanoTime();
        long duration = endTime - startTime;
        double durationInMilliseconds = duration / 1_000_000.0;  // Convertir a milisegundos
        resultado.append("\nTiempo de búsqueda: ").append(durationInMilliseconds).append(" ms");
        txtResultado.setText(resultado.toString());
        lblTiempoBusqueda.setText("Tiempo de búsqueda: " + durationInMilliseconds + " ms");

    }

    private void exportarResultadoAExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            XSSFSheet sheet = workbook.createSheet("Resultados");

            // Crear encabezados
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("País");
            headerRow.createCell(1).setCellValue("Estado");
            headerRow.createCell(2).setCellValue("Ciudad");

            // Agregar datos de la tabla
            String[] lines = txtResultado.getText().split("\n");
            int rownum = 1;
            for (String line : lines) {
                if (line.startsWith("País: ") || line.startsWith("  Estado: ") || line.startsWith("    Ciudad: ")) {
                    Row row = sheet.createRow(rownum++);
                    String[] parts = line.trim().split(": ");
                    if (parts[0].equals("País")) {
                        row.createCell(0).setCellValue(parts[1]);
                    } else if (parts[0].equals("Estado")) {
                        row.createCell(1).setCellValue(parts[1]);
                    } else if (parts[0].equals("Ciudad")) {
                        row.createCell(2).setCellValue(parts[1]);
                    }
                }
            }

            // Guardar el archivo
            try (FileOutputStream out = new FileOutputStream(new File("resultado_pais.xlsx"))) {
                workbook.write(out);
                JOptionPane.showMessageDialog(this, "Resultados exportados a resultado_pais.xlsx");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new BuscarPorPaisFrame();
    }
}
