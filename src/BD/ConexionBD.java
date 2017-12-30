/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package BD;
// Conexion base de datos huellasD - MySQL
/**
 *
 * @author alejo
 */
import java.sql.*;
import javax.swing.JOptionPane;

public class ConexionBD {
    
    public String puerto="3306";
    public String nombreServidor="localhost";
    public String db="huellasd";
    public String user="root";
    public String pass="";
    
    Connection conn;
    
    public void ConexionBD(){
         conn=null;    
    }
    
    public Connection connectar(){
        try{
            String ruta = "jdbc:mysql://";
            String servidor = nombreServidor+":"+puerto+"/";
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn=DriverManager.getConnection(ruta+servidor+db,user,pass);
            if(conn!=null){
                System.out.println("Conexión a base de datos lista...");
            } else if (conn == null){
                throw new SQLException();
            }
        }catch(SQLException e){
            JOptionPane.showMessageDialog(null,"Error: " + e.getMessage());
        }
        catch(NullPointerException e){
            JOptionPane.showMessageDialog(null,"Error: "+e.getMessage() );
        }
        catch (Exception E) {
                    System.err.println("Unable to load driver.");
                    E.printStackTrace();
        }
        finally{
            return conn;
        }
    }
    
    public void deconectar(){
        conn = null;
        System.out.println("Desconexión de la base de datos lista...");            
    }
        
}
