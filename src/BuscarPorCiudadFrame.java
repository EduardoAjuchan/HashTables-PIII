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

public class BuscarPorCiudadFrame extends JFrame {
    private JTextField txtIdCiudad;
    private JTextArea txtResultado;
    private JLabel lblTiempoBusqueda;
    private JButton btnBuscar, btnExportar, btnRegresar;

    private Map<Integer, String> ciudades;
    private Map<Integer, String> estados;
    private Map<Integer, String> paises;
    private Map<Integer, Integer> ciudadEstadoMap;
    private Map<Integer, Integer> estadoPaisMap;

    public BuscarPorCiudadFrame() {
        setTitle("Buscar por Ciudad");
        setSize(600, 400);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        txtIdCiudad = new JTextField();
        txtResultado = new JTextArea();
        lblTiempoBusqueda = new JLabel("Tiempo de búsqueda: ");
        btnBuscar = new JButton("Buscar");
        btnExportar = new JButton("Exportar a Excel");
        btnRegresar = new JButton("Regresar al Menú");

        add(new JLabel("ID de la Ciudad: (Actualmente los id desde 251 a 300 no estan presentes)"));
        add(txtIdCiudad);
        add(btnBuscar);
        add(new JScrollPane(txtResultado));
        add(lblTiempoBusqueda);
        add(btnExportar);
        add(btnRegresar);

        btnBuscar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buscarCiudad();
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
        ciudades = new HashMap<>();
        estados = new HashMap<>();
        paises = new HashMap<>();
        ciudadEstadoMap = new HashMap<>();
        estadoPaisMap = new HashMap<>();

        try (Connection conn = DriverManager.getConnection("jdbc:sqlserver://EDOLAP\\SQLEXPRESS:1433;database=paises_bd;encrypt=false", "soporte", "123456")) {
            // Cargar países
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id, nombre FROM Paises");
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                paises.put(id, nombre);
            }

            // Cargar estados
            rs = stmt.executeQuery("SELECT id, nombre, pais_id FROM Estados");
            while (rs.next()) {
                int id = rs.getInt("id");
                String nombre = rs.getString("nombre");
                int paisId = rs.getInt("pais_id");

                estados.put(id, nombre);
                estadoPaisMap.put(id, paisId);
            }

            // Cargar ciudades
            rs = stmt.executeQuery("SELECT id, nome, estado_id FROM Cidades");
            while (rs.next()) {
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                int estadoId = rs.getInt("estado_id");

                ciudades.put(id, nome);
                ciudadEstadoMap.put(id, estadoId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buscarCiudad() {
        String idCiudadStr = txtIdCiudad.getText();
        if (idCiudadStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese el ID de la ciudad.");
            return;
        }

        int idCiudad;
        try {
            idCiudad = Integer.parseInt(idCiudadStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID de la ciudad inválido. Por favor, ingrese un número entero.");
            return;
        }

        long startTime = System.nanoTime();
        StringBuilder resultado = new StringBuilder();

        if (ciudades.containsKey(idCiudad)) {
            String ciudad = ciudades.get(idCiudad);
            int estadoId = ciudadEstadoMap.get(idCiudad);
            String estado = estados.get(estadoId);
            int paisId = estadoPaisMap.get(estadoId);
            String pais = paises.get(paisId);

            resultado.append("Ciudad: ").append(ciudad).append("\n")
                    .append("Estado: ").append(estado).append("\n")
                    .append("País: ").append(pais).append("\n");
        } else {
            resultado.append("No se encontró la ciudad con ID: ").append(idCiudad).append("\n");
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
            String currentCountry = "";
            String currentState = "";
            String currentCity = "";
            for (String line : lines) {
                if (line.startsWith("Ciudad: ")) {
                    currentCity = line.split(": ")[1];
                } else if (line.startsWith("Estado: ")) {
                    currentState = line.split(": ")[1];
                } else if (line.startsWith("País: ")) {
                    currentCountry = line.split(": ")[1];
                }
                if (!currentCity.isEmpty() && !currentState.isEmpty() && !currentCountry.isEmpty()) {
                    Row row = sheet.createRow(rownum++);
                    row.createCell(0).setCellValue(currentCountry);
                    row.createCell(1).setCellValue(currentState);
                    row.createCell(2).setCellValue(currentCity);
                    currentCity = "";
                }
            }

            // Guardar el archivo
            try (FileOutputStream out = new FileOutputStream(new File("resultado_ciudad.xlsx"))) {
                workbook.write(out);
                JOptionPane.showMessageDialog(this, "Resultados exportados a resultado_ciudad.xlsx");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new BuscarPorCiudadFrame();
    }
}
