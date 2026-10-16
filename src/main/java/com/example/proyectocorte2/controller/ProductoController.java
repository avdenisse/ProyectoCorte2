package com.example.proyectocorte2.controller;

import com.example.proyectocorte2.database.DatabaseConnection;
import com.example.proyectocorte2.model.Producto;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductoController {
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtNombre;
    @FXML
    private ComboBox<String> cmbCategoria;
    @FXML
    private TextField txtStock;
    @FXML
    private TextField txtPrecio;
    @FXML
    private TableView<Producto> tblProductos;
    @FXML
    private TableColumn<Producto, String> colCodigo;
    @FXML
    private TableColumn<Producto, String> colNombre;
    @FXML
    private TableColumn<Producto, String> colCategoria;
    @FXML
    private TableColumn<Producto, BigDecimal> colPrecio;
    @FXML
    private TableColumn<Producto, Integer> colStock;

    private final ObservableList<Producto> productos =
            FXCollections.observableArrayList();

    @FXML
    private ComboBox<String> cmbCategoriaBusqueda;

    @FXML
    private ComboBox<String> cmbOrdenar;

    @FXML
    private CheckBox chkExistencia;
    @FXML
    private TextField txtBuscarCodigoNombre;


    @FXML
    private void initialize() {
        configurarComboboxCategoria();

        configurarTableView();

        tblProductos.setItems(productos);

        tblProductos.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, seleccionado) -> {
                    if (seleccionado != null) {
                        llenarFormulario(seleccionado);
                    }
                });

        actualizarTabla();
        cmbCategoriaBusqueda.getItems().clear();
        cmbCategoriaBusqueda.getItems()
                .addAll(cmbCategoria.getItems());

        cmbOrdenar.getItems().addAll(
                "Nombre A-Z",
                "Nombre Z-A",
                "Precio ascendente",
                "Precio descendente",
                "Mayor stock",
                "Menor stock"

        );
    }


    // Cargar categorías desde PostgreSQL
    private void configurarComboboxCategoria() {
        cmbCategoria.getItems().clear();

        String sql = "SELECT DISTINCT categoria " +
                "FROM catalogos.productos " +
                "ORDER BY categoria ASC";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                cmbCategoria.getItems().add(
                        resultSet.getString("categoria"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudieron cargar las categorías",
                    ex.getMessage());
        }
    }

    // Configurar columnas de la tabla
    private void configurarTableView() {
        colCodigo.setCellValueFactory(
                new PropertyValueFactory<>("codigo"));
        colNombre.setCellValueFactory(
                new PropertyValueFactory<>("nombre"));
        colCategoria.setCellValueFactory(
                new PropertyValueFactory<>("nombreCategoria"));
        colPrecio.setCellValueFactory(
                new PropertyValueFactory<>("precio"));
        colStock.setCellValueFactory(
                new PropertyValueFactory<>("stock"));
    }

    // Guardar producto
    @FXML
    private void guardar() {
        if (!validarCampos()) {
            return;
        }

        String sql = "INSERT INTO catalogos.productos " +
                "(codigo, nombre, categoria, precio, stock) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, txtCodigo.getText().trim());
            statement.setString(2, txtNombre.getText().trim());
            statement.setString(3, cmbCategoria.getValue());
            statement.setBigDecimal(4,
                    new BigDecimal(txtPrecio.getText().trim()));
            statement.setInt(5,
                    Integer.parseInt(txtStock.getText().trim()));


            statement.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                    "Producto guardado",
                    "El producto se almacenó exitosamente.");

            limpiar(null);
            actualizarTabla();

        } catch (SQLException ex) {
            ex.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo guardar", ex.getMessage());
        }
    }

    // Actualizar producto seleccionado
    @FXML
    private void actualizarRegistro() {
        Producto seleccionado =
                tblProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención",
                    "Sin selección",
                    "Selecciona un producto de la tabla primero.");
            return;
        }

        if (!validarCampos()) {
            return;
        }

        String sql = "UPDATE catalogos.productos SET "
                + "codigo = ?, nombre = ?, categoria =?,"
                + "precio = ?, stock = ?, ";


        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, txtCodigo.getText().trim());
            statement.setString(2, txtNombre.getText().trim());
            statement.setString(3, cmbCategoria.getValue());
            statement.setBigDecimal(4,
                    new BigDecimal(txtPrecio.getText().trim()));
            statement.setInt(5,
                    Integer.parseInt(txtStock.getText().trim()));

            statement.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                    "Producto actualizado",
                    "El producto se actualizó correctamente.");

            limpiar(null);
            actualizarTabla();

        } catch (SQLException ex) {
            ex.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo actualizar", ex.getMessage());
        }
    }

    // Eliminación lógica del producto
    @FXML
    private void eliminar() {
        Producto seleccionado =
                tblProductos.getSelectionModel().getSelectedItem();

        if (seleccionado == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Atención",
                    "Sin selección",
                    "Selecciona un producto de la tabla primero.");
            return;
        }

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Confirmar");
        confirmacion.setHeaderText("¿Eliminar este producto?");
        confirmacion.setContentText(seleccionado.getNombre());

        if (confirmacion.showAndWait()
                .orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String sql = "DELETE FROM catalogos.productos "
                + "WHERE codigo = ?";

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setString(1, seleccionado.getCodigo());
            statement.executeUpdate();

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito",
                    "Producto eliminado",
                    "El producto se desactivó correctamente.");

            limpiar(null);
            actualizarTabla();

        } catch (SQLException ex) {
            ex.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo eliminar", ex.getMessage());
        }
    }


    // Cargar los datos del producto seleccionado en el formulario
    private void llenarFormulario(Producto p) {
        txtCodigo.setText(p.getCodigo());
        txtNombre.setText(p.getNombre());
        cmbCategoria.setValue(p.getCategoria());
        txtPrecio.setText(String.valueOf(p.getPrecio()));
        txtStock.setText(String.valueOf(p.getStock()));


    }

    // Actualizar la tabla
    @FXML
    private void actualizarTabla() {
        String sql = "SELECT codigo, nombre, categoria, precio, stock " +
                        "FROM catalogos.productos " +
                        "ORDER BY nombre ASC";


        cargarProductos(sql);
    }


    // Ejecutar un SELECT y mostrar el resultado en el TableView
    private void cargarProductos(String sql) {
        productos.clear();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()
        ) {
            while (resultSet.next()) {
                Producto producto = new Producto();


                producto.setCodigo(
                        resultSet.getString("codigo"));
                producto.setNombre(
                        resultSet.getString("nombre"));
                producto.setCategoria(
                        resultSet.getString("categoria"));

                producto.setPrecio(
                        resultSet.getBigDecimal("precio"));
                producto.setStock(
                        resultSet.getInt("stock"));

                productos.add(producto);
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error",
                    "No se pudo consultar", ex.getMessage());
        }
    }

    // Validar los campos obligatorios
    private boolean validarCampos() {
        if (txtCodigo.getText().isBlank()
                || txtNombre.getText().isBlank()
                || cmbCategoria.getValue() == null
                || txtPrecio.getText().isBlank()
                || txtStock.getText().isBlank()

        ) {

            mostrarAlerta(Alert.AlertType.WARNING, "Validación",
                    "Campos incompletos",
                    "Completa todos los campos obligatorios.");

            return false;
        }

        try {
            BigDecimal precio =
                    new BigDecimal(txtPrecio.getText().trim());

            int stock =
                    Integer.parseInt(txtStock.getText().trim());

            if (precio.compareTo(BigDecimal.ZERO) <= 0
                    || precio.scale() > 2 || stock < 0) {
                mostrarAlerta(Alert.AlertType.WARNING, "Validación",
                        "Valores inválidos",
                        "El precio debe ser mayor que cero y el stock no puede ser negativo.");
                return false;
            }

        } catch (NumberFormatException ex) {
            mostrarAlerta(Alert.AlertType.WARNING, "Validación",
                    "Valores inválidos",
                    "El precio debe ser numérico y el stock debe ser entero.");
            return false;
        }

        return true;
    }


    // Limpiar el formulario
    @FXML
    public void limpiar(ActionEvent actionEvent) {
        txtCodigo.clear();
        txtNombre.clear();

        cmbCategoria.getSelectionModel().clearSelection();
        txtPrecio.clear();
        txtStock.clear();


        tblProductos.getSelectionModel().clearSelection();
    }


    // Mostrar mensajes
    private void mostrarAlerta(
            Alert.AlertType tipo, String titulo,
            String encabezado, String mensaje) {

        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(encabezado);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }


@FXML
private void buscarProductos() {
    StringBuilder sql = new StringBuilder(
            "SELECT codigo, nombre, categoria, precio, stock " +
                    "FROM catalogos.productos WHERE 1=1"
    );

    ObservableList<Object> parametros =
            FXCollections.observableArrayList();

    // Buscar por código o nombre
    String busqueda = txtBuscarCodigoNombre.getText().trim();

    if (!busqueda.isEmpty()) {
        sql.append(" AND (codigo ILIKE ? OR nombre ILIKE ?)");
        parametros.add("%" + busqueda + "%");
        parametros.add("%" + busqueda + "%");
    }

    // Filtrar por categoría
    if (cmbCategoriaBusqueda.getValue() != null) {
        sql.append(" AND categoria = ?");
        parametros.add(cmbCategoriaBusqueda.getValue());
    }

    // Mostrar solo productos con existencias
    if (chkExistencia.isSelected()) {
        sql.append(" AND stock > 0");
    }

    // Ordenar los resultados
    String orden = cmbOrdenar.getValue();

    if ("Nombre A-Z".equals(orden)) {
        sql.append(" ORDER BY nombre ASC");
    } else if ("Nombre Z-A".equals(orden)) {
        sql.append(" ORDER BY nombre DESC");
    } else if ("Precio ascendente".equals(orden)) {
        sql.append(" ORDER BY precio ASC");
    } else if ("Precio descendente".equals(orden)) {
        sql.append(" ORDER BY precio DESC");
    } else if ("Mayor stock".equals(orden)) {
        sql.append(" ORDER BY stock DESC");
    } else if ("Menor stock".equals(orden)) {
        sql.append(" ORDER BY stock ASC");
    } else {
        sql.append(" ORDER BY nombre ASC");
    }

    productos.clear();

    try (
            Connection connection =
                    DatabaseConnection.getConnection();
            PreparedStatement statement =
                    connection.prepareStatement(sql.toString())
    ) {
        for (int i = 0; i < parametros.size(); i++) {
            statement.setObject(i + 1, parametros.get(i));
        }

        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Producto producto = new Producto();

                producto.setCodigo(
                        resultSet.getString("codigo"));

                producto.setNombre(
                        resultSet.getString("nombre"));

                producto.setCategoria(
                        resultSet.getString("categoria"));

                producto.setPrecio(
                        resultSet.getBigDecimal("precio"));

                producto.setStock(
                        resultSet.getInt("stock"));

                productos.add(producto);
            }
        }

    } catch (SQLException ex) {
        ex.printStackTrace();

        mostrarAlerta(
                Alert.AlertType.ERROR,
                "Error",
                "No se pudo realizar la búsqueda",
                ex.getMessage()
        );
    }
}

    @FXML
    private void limpiarFiltros() {
        txtBuscarCodigoNombre.clear();
        cmbCategoriaBusqueda.getSelectionModel().clearSelection();
        chkExistencia.setSelected(false);
        cmbOrdenar.getSelectionModel().clearSelection();

        actualizarTabla();
    }
}






