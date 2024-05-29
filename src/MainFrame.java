import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Sistema de Búsqueda");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout(5, 1));

        JButton btnBuscarPorPais = new JButton("Buscar por País");
        JButton btnBuscarPorEstado = new JButton("Buscar por Estado");
        JButton btnBuscarPorCiudad = new JButton("Buscar por Ciudad");
        JButton btnSalir = new JButton("Salir");

        btnBuscarPorPais.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new BuscarPorPaisFrame();
            }
        });

        btnBuscarPorEstado.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new BuscarPorEstadoFrame();
            }
        });

        btnBuscarPorCiudad.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new BuscarPorCiudadFrame();
            }
        });

        btnSalir.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        add(btnBuscarPorPais);
        add(btnBuscarPorEstado);
        add(btnBuscarPorCiudad);
        add(btnSalir);

        setVisible(true);
    }

    public static void main(String[] args) {
        new MainFrame();
    }
}
