package bot.modelo;
import src.bot.controlador.SnmpLector;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList; // Importante
import java.util.List;

public class ObjetosDAO {
    private String url = "jdbc:mysql://localhost:3306/redes_db"; //base de datos del mysql
    private String usuario = "USUARIO DEL MYSQL";
    private String password = "CONtRASEÑA DEL MYSQL";
    private Connection con;
    private SnmpLector lector = null; 
   

    public ObjetosDAO() {
        try {
            this.con = DriverManager.getConnection(url, usuario, password);
        } catch (Exception e) {
            System.out.println("❌ Error de conexión Java: " + e.getMessage());
        }
        this.lector = new SnmpLector();
    }

    // AHORA DEVUELVE LOS NOMBRES DE LOS OID PARA PODER LEERLOS
    public List<String> obtenerOidsActivos() {
        List<String> lista = new ArrayList<>(); // INICIALIZACIÓN CLAVE
        try {
            // Queremos el nombre del OID para pasárselo al Lector SNMP
            String query = "SELECT OID FROM monitoreo";
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lista.add(rs.getString("OID"));
            }
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo OIDs: " + e.getMessage());
        }
        return lista;
    }

    // ACTUALIZA EL VALOR Y COMPARA CON EL UMBRAL (THRESHOLD)
    //TAMBIEN SE PUEDE PONER COMPARACIÓN DE VALORES
    
      public void actualizarDatoYVerificarAlerta(String _oid, String valorRecibido) {
    try {
        long threshold = 0;
        long valorAnterior = 0;
        int estadoAlerta = 0; // Usamos int para que MySQL lo entienda mejor como booleano
        int intf = 0; 
        // 1. Obtener datos actuales de la fila específica
        String querySelect = "SELECT threshold, valor FROM monitoreo WHERE OID=?";
        PreparedStatement psSelect = con.prepareStatement(querySelect);
        psSelect.setString(1, _oid);
        ResultSet rs = psSelect.executeQuery();

        if (rs.next()) {
            // Usamos getLong para evitar errores de capacidad con los octetos
            threshold = rs.getLong("threshold");
            String valDb = rs.getString("valor");
            valorAnterior = (valDb != null && !valDb.isEmpty()) ? Long.parseLong(valDb.replaceAll("[^0-9]", "")) : 0;
        }

      
        long valorNuevo = Long.parseLong(valorRecibido.replaceAll("[^0-9]", ""));
        if(valorAnterior > 0){
          long diferencia = valorNuevo - valorAnterior;
          if (diferencia > threshold) {
                  System.out.println("⚠️ ¡ALERTA! " + _oid + " superó el umbral (" + diferencia + " > " + threshold + ")");
                  
                  // Extraer el índice de la interfaz correctamente
                  int posit = _oid.lastIndexOf(".");
                  if (posit != -1) {
                      intf = Integer.parseInt(_oid.substring(posit + 1));
                      System.out.println("Interfaz detectada: " + intf);
                  }
                  
                  estadoAlerta = 1; // ACTIVAMOS LA ALERTA
              }
        }

        // 4. ACTUALIZAR SIEMPRE (Fuera del IF/ELSE)
        // Esto garantiza que todas las filas se actualicen una por una
        String queryUpdate = "UPDATE monitoreo SET alerta_enviada = ?, valor = ? WHERE OID = ?";
        PreparedStatement psUpdate = con.prepareStatement(queryUpdate);
        psUpdate.setInt(1, estadoAlerta);
        psUpdate.setString(2, String.valueOf(valorNuevo));
        psUpdate.setString(3, _oid);
        psUpdate.executeUpdate();

    } catch (Exception e) {
        System.out.println("❌ Error actualizando y verificando: " + e.getMessage());
    }
}
    
    
    

  //Para insertar objetos en la tabla el umbral lo ponemos nosotros
    public void insertarNuevoOid(String nombre_oid, String umbral, String oid) {
        try {
            String query = "INSERT INTO monitoreo (nombre_oid, valor, threshold, OID) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, nombre_oid);
            ps.setString(2, "0"); 
            ps.setString(3, umbral);
            ps.setString(4, oid);
            ps.executeUpdate();
            System.out.println("➕ Nuevo OID registrado con umbral: " + umbral);
        } catch (Exception e) {
            System.out.println("❌ Error al insertar: " + e.getMessage());
        }
    }

  
   //Para eliminar objetos de la tabla
  

    public void eliminarOid(String nombre_oid) {
        try {
            String query = "DELETE FROM monitoreo WHERE nombre_oid = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, nombre_oid);
            ps.executeUpdate();
            System.out.println("🗑️ OID eliminado: " + nombre_oid);
        } catch (Exception e) {
            System.out.println("❌ Error al eliminar: " + e.getMessage());
        }
    }
}
