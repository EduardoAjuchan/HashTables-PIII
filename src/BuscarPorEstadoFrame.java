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

public class BuscarPorEstadoFrame extends JFrame {
    private JTextField txtIdEstado;
    private JTextArea txtResultado;
    private JLabel lblTiempoBusqueda;
    private JButton btnBuscar, btnExportar, btnRegresar;

    private Map<Integer, String> estados;
    private Map<Integer, String> paises;
    private Map<Integer, Set<String>> ciudadesPorEstado;
    private Map<Integer, Integer> estadoPaisMap;

    public BuscarPorEstadoFrame() {
        setTitle("Buscar por Estado");
        setSize(600, 400);
        setLayout(new BoxLayout(getContentPane(), BoxLayout.Y_AXIS));

        txtIdEstado = new JTextField();
        txtResultado = new JTextArea();
        lblTiempoBusqueda = new JLabel("Tiempo de búsqueda: ");
        btnBuscar = new JButton("Buscar");
        btnExportar = new JButton("Exportar a Excel");
        btnRegresar = new JButton("Regresar al Menú");

        add(new JLabel("ID del Estado:"));
        add(txtIdEstado);
        add(btnBuscar);
        add(new JScrollPane(txtResultado));
        add(lblTiempoBusqueda);
        add(btnExportar);
        add(btnRegresar);

        btnBuscar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buscarEstado();
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
        estados = new HashMap<>();
        paises = new HashMap<>();
        ciudadesPorEstado = new HashMap<>();
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

                if (!ciudadesPorEstado.containsKey(id)) {
                    ciudadesPorEstado.put(id, new HashSet<>());
                }
            }

            // Cargar ciudades
            rs = stmt.executeQuery("SELECT id, nome, estado_id FROM Cidades");
            while (rs.next()) {
                int id = rs.getInt("id");
                String nome = rs.getString("nome");
                int estadoId = rs.getInt("estado_id");

                if (ciudadesPorEstado.containsKey(estadoId)) {
                    ciudadesPorEstado.get(estadoId).add(nome);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void buscarEstado() {
        String idEstadoStr = txtIdEstado.getText();
        if (idEstadoStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, ingrese el ID del estado.");
            return;
        }

        int idEstado;
        try {
            idEstado = Integer.parseInt(idEstadoStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID del estado inválido. Por favor, ingrese un número entero.");
            return;
        }

        long startTime = System.nanoTime();
        StringBuilder resultado = new StringBuilder();

        if (estados.containsKey(idEstado)) {
            String estado = estados.get(idEstado);
            int paisId = estadoPaisMap.get(idEstado);
            String pais = paises.get(paisId);

            resultado.append("Estado: ").append(estado).append("\n")
                    .append("País: ").append(pais).append("\n");

            Set<String> ciudades = ciudadesPorEstado.get(idEstado);
            if (ciudades != null && !ciudades.isEmpty()) {
                for (String ciudad : ciudades) {
                    resultado.append("  Ciudad: ").append(ciudad).append("\n");
                }
            } else {
                resultado.append("  No hay ciudades registradas para este estado.\n");
            }
        } else {
            resultado.append("No se encontró el estado con ID: ").append(idEstado).append("\n");
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
            for (String line : lines) {
                if (line.startsWith("Estado: ")) {
                    currentState = line.split(": ")[1];
                } else if (line.startsWith("País: ")) {
                    currentCountry = line.split(": ")[1];
                } else if (line.startsWith("  Ciudad: ")) {
                    Row row = sheet.createRow(rownum++);
                    row.createCell(0).setCellValue(currentCountry);
                    row.createCell(1).setCellValue(currentState);
                    row.createCell(2).setCellValue(line.split(": ")[1]);
                }
            }

            // Guardar el archivo
            try (FileOutputStream out = new FileOutputStream(new File("resultado_estado.xlsx"))) {
                workbook.write(out);
                JOptionPane.showMessageDialog(this, "Resultados exportados a resultado_estado.xlsx");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new BuscarPorEstadoFrame();
    }
}
