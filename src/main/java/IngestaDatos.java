import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class IngestaDatos {

    // Configuración de los datos de Docker

    private static final String URL = "jdbc:mysql://localhost:41063/gasolineras";
    private static final String USER = "root";
    private static final String PASS = "root";

    // Mapas para guardar los IDs generados y evitar duplicar inserciones
    static Map<String, Integer> mapProvincias = new HashMap<>();
    static Map<String, Integer> mapMunicipios = new HashMap<>();
    static Map<String, Integer> mapEmpresas = new HashMap<>();
    static Map<String, Integer> mapCarburantes = new HashMap<>();

    public static void main(String[] args) {
        try {
            //Forzar la carga del driver de MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.out.println("¡Error! No se encontró el driver de MySQL");
            e.printStackTrace();
            return;
        }

        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("Conectando a la base de datos");

            // Inicializamos los carburantes que tenemos que localizar
            inicializarCarburantes(conn);

            // Procesamos los dos CSV descargados de la web https://geoportalgasolineras.es/geoportal-instalaciones/DescargarFicheros
            procesarArchivo(conn, "preciosEESS_es.csv", false);
            procesarArchivo(conn, "embarcacionesPrecios_es.csv", true);

            System.out.println("¡Acabas de completar la ingesta de datos, ENHORABUENA!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void inicializarCarburantes(Connection conn) throws SQLException {
        String[] carburantes = {"Gasolina 95 ES", "Gasóleo A", "Gasóleo de uso marítimo", "Gasolina 95 E5 Premium"};
        String sql = "INSERT IGNORE INTO CARBURANTE (nombre) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (String c : carburantes) {
                ps.setString(1, c);
                ps.executeUpdate();
            }
        }

        // Leemos los IDs generados
        try (
                Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT id_carburante, nombre FROM CARBURANTE")){
            while (rs.next()) {
                mapCarburantes.put(rs.getString("nombre"), rs.getInt("id_carburante"));
            }
        }
    }

    private static void procesarArchivo(Connection conn, String rutaArchivo, boolean esMaritimo) {
        String linea;
        boolean esCabecera = true;
        int contadorLineas = 0;
        int lineasInsertadas = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(rutaArchivo))) {
            while ((linea = br.readLine()) != null) {
                String[] columnas = linea.split(";", -1);

                if (esCabecera) {
                    for (int i = 0; i < columnas.length; i++) {
                        if (columnas[i].trim().matches("(?i).*(rótulo|localidad|dirección|latitud|longitud|margen|precio gasolina 95 e5|precio gasóleo a|precio gasóleo de uso marítimo).*")) {
                            System.out.println("Columna " + i + ": " + columnas[i].trim());
                        }
                    }

                    esCabecera = false;
                    continue;
                }

                contadorLineas ++;

                if (columnas.length < 10) continue;

                try {
                    String provincia = columnas[0].trim();
                    String municipio = columnas[1].trim();

                    String localidad, cp, direccion, latStr, lonStr, margen, rotulo;

                    if (esMaritimo) {
                        // ÍNDICES PARA ARCHIVO MARÍTIMO (embarcacionesPrecios_es.csv)
                        localidad = columnas[3].trim();
                        cp = columnas[4].trim();
                        direccion = columnas[5].trim();
                        lonStr = columnas[6].trim().replace(",", ".");
                        latStr= columnas[7].trim().replace(",", ".");
                        margen = null;
                        rotulo = "SIN ROTULO";
                        for (int i = 10; i < columnas.length; i++) {
                            if (!columnas[i].trim().isEmpty() && !columnas[i].trim().matches("[-+]?\\d*[.,]?\\d+")) {
                                // Es un texto, probablemente el rótulo
                                rotulo = columnas[i].trim();
                                break;
                            }
                        }
                    } else {
                        // ÍNDICES PARA ARCHIVO TERRESTRE (preciosEESS_es.csv)
                        localidad = columnas[2].trim();
                        cp = columnas[3].trim();
                        direccion = columnas[4].trim();
                        margen = columnas[5].trim();
                        lonStr = columnas[6].trim().replace(",", ".");
                        latStr = columnas[7].trim().replace(",", ".");
                        rotulo = "SIN RÓTULO";
                        for (int i = columnas.length - 1; i > 10; i--) {
                            String valor = columnas[i].trim();
                            if (!valor.isEmpty() && !valor.matches("[-+]?\\d*[.,]?\\d+") && !valor.equals("dm") && !valor.equals("OM") && !valor.equals("P")) {
                                rotulo = valor;
                                break;
                            }
                        }
                    }

                    String tipo = esMaritimo ? "Maritimo" : "Terrestre";

                    // Nombre genérico si "rotulo" está vacío

                    if (rotulo.isEmpty()) rotulo = "SIN ROTULO";

                    // Insertamos provincia si no existe
                    int idProv = getInsertId(conn, "PROVINCIA", provincia, mapProvincias);

                    // Insertamos municipio
                    int idMun = getInsertMun(conn, municipio, idProv);

                    // Insertamos empresa
                    int idEmp = getInsertId(conn, "EMPRESA", rotulo, mapEmpresas);

                    // Insertamos estación de servicio
                    int idEstacion = insertEstacion(conn, idMun, idEmp, localidad, cp, direccion, margen, latStr, lonStr, tipo);

                    // Insertamos precios
                    if (esMaritimo) {
                        insertarPrecio(conn, idEstacion, "Gasolina 95 ES", columnas[8]);
                        insertarPrecio(conn, idEstacion, "Gasóleo A", columnas[10]);
                        insertarPrecio(conn, idEstacion, "Gasoóleo de uso marítimo", columnas[11]);
                    } else {
                        insertarPrecio(conn, idEstacion, "Gasolina 95 ES", columnas[9]);
                        insertarPrecio(conn, idEstacion, "Gasóleo A", columnas[14]);
                    }

                    lineasInsertadas ++;

                } catch (Exception e) {
                    if (contadorLineas < 5) {
                        System.out.println("Error en la línea " + contadorLineas + ": " + e.getMessage());
                    }
                }
            }
            System.out.println("Archivo " + rutaArchivo + " Se han procesado " + contadorLineas + " Se han insertado " + contadorLineas);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Métodos auxiliares

    private static int getInsertId(Connection conn, String tabla, String nombre, Map<String, Integer> mapa) throws SQLException {
        if (nombre == null || nombre.isEmpty()) nombre = "DESCONOCIDO";
        if (mapa.containsKey(nombre)) return mapa.get(nombre);

        String sql = "INSERT INTO " + tabla + " (nombre) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                mapa.put(nombre, id);
                return id;
            }
        }

        return -1;
    }

    private static int getInsertMun(Connection conn, String nombre, int idProv) throws SQLException {
        String key = nombre + "-" + idProv;
        if (mapMunicipios.containsKey(key)) return mapMunicipios.get(key);

        String sql = "INSERT INTO MUNICIPIO (nombre, id_provincia) VALUES (?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            ps.setString(1, nombre);
            ps.setInt(2, idProv);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                mapMunicipios.put(key, id);
                return id;
            }
        }
        return -1;
    }

    private static int insertEstacion(Connection conn, int idMun, int idEmp, String loc, String cp, String dir, String margen,
                                      String lat, String lon, String tipo) throws SQLException {
        String sql = "INSERT INTO ESTACION (id_municipio, id_empresa, localidad, codigo_postal, direccion, margen, latitud," +
                "longitud, tipo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idMun);
            ps.setInt(2, idEmp);
            ps.setString(3, loc);
            ps.setString(4, cp);
            ps.setString(5, dir);
            ps.setString(6, margen);
            ps.setBigDecimal(7, lat.isEmpty() ? null : new java.math.BigDecimal(lat));
            ps.setBigDecimal(8, lon.isEmpty() ? null : new java.math.BigDecimal(lon));
            ps.setString(9, tipo);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        }
        return  -1;
    }

    private static void insertarPrecio(Connection conn, int idEstacion, String nombreCarburante, String precioStr) throws SQLException {
        if (precioStr == null || precioStr.trim().isEmpty()) return;
        int idCarb = mapCarburantes.get(nombreCarburante);
        String precio = precioStr.trim().replace(",", ".");

        String sql = "INSERT INTO PRECIO (id_estacion, id_carburante, precio) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEstacion);
            ps.setInt(2, idCarb);
            ps.setBigDecimal(3, new java.math.BigDecimal(precio));
            ps.executeUpdate();
        }
    }
}
